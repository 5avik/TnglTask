package pro.tngl.tangledtask.ui

import androidx.compose.ui.res.stringResource
import pro.tngl.tangledtask.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pro.tngl.tangledtask.domain.streaks
import java.time.*

@Composable
fun StatisticsScreen(state: TaskState, today: LocalDate) {
    val tasks = state.tasks.filterNot { it.deleted }
    val completed = tasks.mapNotNull { it.completedAt }.map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    val counts = completed.groupingBy { it }.eachCount()
    val streak = streaks(completed, today)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.ui_statisticsscreen_39), style = MaterialTheme.typography.headlineMedium) }
        item { TaskProgress(completed.size, tasks.size, state.prefs.animations) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Всего задач", tasks.size, Modifier.weight(1f))
                Metric("Выполнено", completed.size, Modifier.weight(1f))
                Metric("Осталось", tasks.size - completed.size, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Текущая серия", streak.current, Modifier.weight(1f))
                Metric("Лучшая серия", streak.longest, Modifier.weight(1f))
            }
        }
        item { ActivityChart("Последние 7 дней", (6L downTo 0).map { counts[today.minusDays(it)] ?: 0 }) }
        item { ActivityChart("Последние 30 дней", (29L downTo 0).map { counts[today.minusDays(it)] ?: 0 }) }
        item {
            StatsCard("Задачи по категориям") {
                if (tasks.isEmpty()) Text(stringResource(R.string.ui_statisticsscreen_40))
                (state.categories.map { Triple(it.id, "${it.icon} ${it.name}", Color(it.color)) } +
                    Triple(null, "Без категории", MaterialTheme.colorScheme.primary)).forEach { (id, name, color) ->
                    val count = tasks.count { it.categoryId == id }
                    if (count > 0) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(name, Modifier.weight(1f))
                            Text(count.toString())
                        }
                        LinearProgressIndicator({ count.toFloat() / tasks.size }, Modifier.fillMaxWidth(), color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(title: String, value: Int, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ActivityChart(title: String, counts: List<Int>) {
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    StatsCard(title) {
        Text("Выполнено задач · ${counts.sum()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(78.dp), horizontalArrangement = Arrangement.spacedBy(if (counts.size > 7) 2.dp else 8.dp),
            verticalAlignment = Alignment.Bottom) {
            counts.forEachIndexed { index, value ->
                Box(Modifier.weight(1f).height(if (value == 0) 4.dp else (value.toFloat() / max * 72).dp)
                    .background(if (value == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp))
                    .semantics { contentDescription = "День ${index + 1}: выполнено $value" })
            }
        }
    }
}
