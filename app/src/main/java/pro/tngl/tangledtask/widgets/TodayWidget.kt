package pro.tngl.tangledtask.widgets

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.compose.runtime.remember
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import pro.tngl.tangledtask.MainActivity
import pro.tngl.tangledtask.R
import androidx.glance.unit.ColorProvider
import pro.tngl.tangledtask.core.TaskApp
import pro.tngl.tangledtask.data.Task
import java.time.LocalDate

private val taskId = ActionParameters.Key<String>("task-id")

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayWidget()
}

class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now().toEpochDay()
        val tasks = (context.applicationContext as TaskApp).tasks.all().filter { !it.deleted && it.dueDay == today }
        val open = Intent(context, MainActivity::class.java).setData(Uri.parse("tngltask://new/today"))
        provideContent { GlanceTheme { TodayContent(tasks, actionStartActivity(open)) } }
    }
}

@Composable
private fun TodayContent(tasks: List<Task>, create: Action) {
    val count = tasks.count { it.completedAt != null }
    val ink = widgetColor(R.color.widget_ink)
    Column(GlanceModifier.fillMaxSize().background(widgetColor(R.color.widget_background)).padding(12.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Сегодня", GlanceModifier.defaultWeight(),
                style = TextStyle(color = ink, fontWeight = FontWeight.Bold, fontSize = 17.sp))
            Text("＋ Добавить", GlanceModifier.clickable(create).padding(8.dp), style = TextStyle(color = ink))
        }
        WidgetProgress(count, tasks.size)
        if (tasks.isEmpty()) Text("На сегодня задач нет", style = TextStyle(color = ink))
        LazyColumn {
            items(tasks) { task ->
                val done = task.completedAt != null
                Text(if (done) "●  ${task.title}" else "○  ${task.title}",
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 8.dp).clickable(
                        actionRunCallback<CompleteWidgetTask>(actionParametersOf(taskId to task.id, targetDone to !done))),
                    style = TextStyle(color = ink), maxLines = 2)
            }
        }
    }
}

class ProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ProgressWidget()
}

class ProgressWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now().toEpochDay()
        val tasks = (context.applicationContext as TaskApp).tasks.all().filter { !it.deleted && it.dueDay == today }
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(widgetColor(R.color.widget_background)).padding(12.dp)) {
                Text("Прогресс сегодня", style = TextStyle(color = widgetColor(R.color.widget_ink), fontWeight = FontWeight.Bold))
                WidgetProgress(tasks.count { it.completedAt != null }, tasks.size)
            }
        }
    }
}

@Composable
private fun WidgetProgress(count: Int, total: Int) {
    val ink = widgetColor(R.color.widget_ink)
    val muted = widgetColor(R.color.widget_muted)
    val accent = ColorProvider(Color(0xFFD71921))
    val track = widgetColor(R.color.widget_track)
    val progress = if (total == 0) 0 else count * 100 / total
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$progress%", GlanceModifier.defaultWeight(),
            style = TextStyle(color = ink, fontWeight = FontWeight.Bold, fontSize = 26.sp, fontFamily = FontFamily.Monospace))
        Text("Осталось ${total - count}", style = TextStyle(color = muted))
    }
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)) {
        repeat(10) { i ->
            Box(GlanceModifier.defaultWeight().padding(end = 2.dp).height(4.dp)
                .background(if (progress >= (i + 1) * 10) accent else track)) {}
        }
    }
}

@Composable
private fun widgetColor(@ColorRes id: Int): ColorProvider {
    val context = LocalContext.current
    return remember(context, id) {
        fun resolve(mode: Int): Color {
            val config = Configuration(context.resources.configuration)
            config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            return Color(context.createConfigurationContext(config).getColor(id))
        }
        androidx.glance.color.ColorProvider(resolve(Configuration.UI_MODE_NIGHT_NO), resolve(Configuration.UI_MODE_NIGHT_YES))
    }
}

private val targetDone = ActionParameters.Key<Boolean>("done")

class CompleteWidgetTask : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[taskId] ?: return
        val done = parameters[targetDone] ?: return
        (context.applicationContext as TaskApp).tasks.complete(id, done)
        TodayWidget().updateAll(context)
    }
}
