package pro.tngl.tangledtask.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pro.tngl.tangledtask.MainActivity
import pro.tngl.tangledtask.core.TaskApp
import pro.tngl.tangledtask.data.Task
import pro.tngl.tangledtask.widgets.TodayWidget
import androidx.glance.appwidget.updateAll
import java.time.*
import java.util.concurrent.TimeUnit

private const val CHANNEL = "task-reminders"
private const val FIRE = "pro.tngl.tangledtask.FIRE"
private const val COMPLETE = "pro.tngl.tangledtask.COMPLETE"
private const val SNOOZE = "pro.tngl.tangledtask.SNOOZE"
private const val EXPECTED_TIME = "expectedTime"

private fun Task.trigger(): Long? {
    if (deleted || completedAt != null) return null
    snoozedUntil?.let { return it }
    val day = dueDay ?: return null
    val time = minute ?: return null
    val offset = reminder ?: return null
    return LocalDate.ofEpochDay(day).atStartOfDay().plusMinutes(time.toLong())
        .atZone(ZoneId.systemDefault()).minusMinutes(offset.toLong()).toInstant().toEpochMilli()
}

private fun pending(context: Context, action: String, id: String, expectedTime: Long? = null): PendingIntent =
    PendingIntent.getBroadcast(context, 0, Intent(context, ReminderReceiver::class.java).apply {
        this.action = action
        data = Uri.Builder().scheme("tngltask").authority("reminder").appendPath(id).build()
        if (expectedTime != null) putExtra(EXPECTED_TIME, expectedTime)
    }, PendingIntent.FLAG_IMMUTABLE or if (expectedTime == null) 0 else PendingIntent.FLAG_UPDATE_CURRENT)

class ReminderWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as TaskApp
        val manager = app.getSystemService(AlarmManager::class.java)
        val enabled = app.preferences.values.first().notifications
        val now = System.currentTimeMillis()
        val previous = app.getSharedPreferences("scheduled", Context.MODE_PRIVATE)
        val scheduled = mutableSetOf<String>()
        for (task in app.tasks.all()) {
            val time = task.trigger()
            if (enabled && time != null && time > now) {
                val alarm = pending(app, FIRE, task.id, time)
                if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
                    try {
                        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarm)
                    } catch (_: SecurityException) {
                        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarm)
                    }
                } else {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, alarm)
                }
                scheduled.add(task.id)
            }
            if (!enabled || task.deleted || task.completedAt != null) {
                NotificationManagerCompat.from(app).cancel(task.id, 1)
            }
        }
        (previous.getStringSet("ids", emptySet()).orEmpty() - scheduled).forEach {
            val task = app.tasks.get(it)
            if (!enabled || task?.trigger() == null) manager.cancel(pending(app, FIRE, it))
        }
        previous.edit().putStringSet("ids", scheduled).apply()
        runCatching { TodayWidget().updateAll(app) }
        val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        WorkManager.getInstance(app).enqueueUniqueWork("day-rollover", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DayRolloverWork>()
                .setInitialDelay((midnight - now).coerceAtLeast(0), TimeUnit.MILLISECONDS).build())
        return Result.success()
    }

    companion object {
        fun refresh(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("reminders", ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<ReminderWork>().build())
        }
    }
}

class DayRolloverWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ReminderWork.refresh(applicationContext)
        return Result.success()
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TaskApp
        val action = intent.action
        if (action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            ReminderWork.refresh(app)
            return
        }
        val id = intent.data?.lastPathSegment ?: return
        val result = goAsync()
        app.scope.launch {
            try {
                when (action) {
                    COMPLETE -> app.tasks.complete(id, true)
                    SNOOZE -> app.tasks.snooze(id, System.currentTimeMillis() + 10 * 60_000L)
                    FIRE -> {
                        val task = app.tasks.get(id) ?: return@launch
                        val time = task.trigger() ?: return@launch
                        val expected = intent.getLongExtra(EXPECTED_TIME, Long.MIN_VALUE)
                        if (time == expected && time <= System.currentTimeMillis() &&
                            app.preferences.values.first().notifications) notify(app, task)
                    }
                }
                if (action == COMPLETE || action == SNOOZE) NotificationManagerCompat.from(app).cancel(id, 1)
            } finally {
                result.finish()
            }
        }
    }

    private fun notify(context: Context, task: Task) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Напоминания о задачах", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply {
            data = Uri.Builder().scheme("tngltask").authority("task").appendPath(task.id).build()
        }, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(task.title)
            .setContentText(task.description.ifBlank { "Запланированная задача" })
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Выполнить", pending(context, COMPLETE, task.id))
            .addAction(0, "Через 10 минут", pending(context, SNOOZE, task.id))
            .build()
        NotificationManagerCompat.from(context).notify(task.id, 1, notification)
    }
}
