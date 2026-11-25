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
            NothingStatCard("Карта активности · 35 дней") {
                val peak = (0L..34L).maxOf { counts[today.minusDays(it)] ?: 0 }.coerceAtLeast(1)
                repeat(5) { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(7) { column ->
                            val day = today.minusDays((34 - week * 7 - column).toLong())
                            val count = counts[day] ?: 0
                            val ink = when {
                                count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                day == today -> MaterialTheme.colorScheme.primary
                                else -> androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.onSurface, .3f + .7f * count / peak)
                            }
                            Box(Modifier.weight(1f).height(16.dp).background(ink, RoundedCornerShape(3.dp))
                                .semantics { contentDescription = "${day.format(shortDate)} · выполнено $count" })
                        }
                    }
                }
            }
        }
        item {
            NothingStatCard("Задачи по категориям") {
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
    NothingCard(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MatrixDisplayText(value.toString(), MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NothingStatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    NothingCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ActivityChart(title: String, counts: List<Int>) {
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    NothingStatCard(title) {
        Text("Выполнено задач · ${counts.sum()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().height(78.dp), horizontalArrangement = Arrangement.spacedBy(if (counts.size > 7) 2.dp else 8.dp),
            verticalAlignment = Alignment.Bottom) {
            counts.forEachIndexed { index, value ->
                Box(Modifier.weight(1f).height(if (value == 0) 4.dp else (value.toFloat() / max * 72).dp)
                    .background(if (value == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
                        RoundedCornerShape(2.dp))
                    .semantics { contentDescription = "День ${index + 1}: выполнено $value" })
            }
        }
    }
}
