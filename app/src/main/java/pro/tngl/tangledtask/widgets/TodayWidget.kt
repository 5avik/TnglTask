package pro.tngl.tangledtask.widgets

import android.content.Context
import android.content.Intent
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
    Column(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).padding(12.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Сегодня", GlanceModifier.defaultWeight(),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp))
            Button("Добавить", create)
        }
        Text("Выполнено $count из ${tasks.size}", style = TextStyle(color = GlanceTheme.colors.onSurface))
        LinearProgressIndicator(if (tasks.isEmpty()) 0f else count.toFloat() / tasks.size,
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
            color = androidx.glance.unit.ColorProvider(Color(0xFF34D399)))
        if (tasks.isEmpty()) Text("На сегодня задач нет", style = TextStyle(color = GlanceTheme.colors.onSurface))
        LazyColumn {
            items(tasks) { task ->
                val done = task.completedAt != null
                Text(if (done) "✓  ${task.title}" else "○  ${task.title}",
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 8.dp).clickable(
                        actionRunCallback<CompleteWidgetTask>(actionParametersOf(taskId to task.id, targetDone to !done))),
                    style = TextStyle(color = GlanceTheme.colors.onSurface), maxLines = 2)
            }
        }
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
