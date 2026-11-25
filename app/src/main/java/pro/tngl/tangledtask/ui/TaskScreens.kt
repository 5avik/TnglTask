package pro.tngl.tangledtask.ui

import androidx.compose.ui.res.stringResource
import pro.tngl.tangledtask.R

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.tngl.tangledtask.data.*
import java.time.*
import java.time.format.TextStyle

@Composable
fun TaskProgress(done: Int, total: Int, animations: Boolean) {
    val target = if (total == 0) 0f else done.toFloat() / total
    val progress by animateFloatAsState(target, if (animations) tween(260) else snap(), label = "progress")
    val celebration = remember { Animatable(1f) }
    LaunchedEffect(target == 1f, animations) {
        celebration.snapTo(1f)
        if (animations && target == 1f) {
            celebration.animateTo(1.08f, tween(130))
            celebration.animateTo(1f, tween(150))
        }
    }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).graphicsLayer {
                scaleX = celebration.value
                scaleY = celebration.value
            }, contentAlignment = Alignment.Center) {
                CircularProgressIndicator({ progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 5.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Column(Modifier.weight(1f).then(if (animations) Modifier.animateContentSize() else Modifier),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ui_taskscreens_33), style = MaterialTheme.typography.titleMedium)
                Text("Выполнено $done · осталось ${total - done}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (animations) AnimatedVisibility(target == 1f) { Text(stringResource(R.string.ui_taskscreens_34), color = MaterialTheme.colorScheme.primary) }
                else if (target == 1f) Text(stringResource(R.string.ui_taskscreens_34), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun weekStart(date: LocalDate, monday: Boolean): LocalDate =
    date.minusDays(((date.dayOfWeek.value - if (monday) 1 else 7) + 7L) % 7L)

@Composable
fun CalendarGrid(selected: LocalDate, today: LocalDate, daysWithTasks: Set<Long>, monday: Boolean,
                 month: Boolean, onSelect: (LocalDate) -> Unit) {
    val first = if (month) selected.withDayOfMonth(1) else selected
    val start = weekStart(first, monday)
    val rows = if (month) ((first.toEpochDay() - start.toEpochDay()).toInt() + first.lengthOfMonth() + 6) / 7 else 1
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { index ->
                    val day = start.plusDays(index.toLong())
                    Box(Modifier.weight(1f).height(25.dp), contentAlignment = Alignment.Center) {
                        Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, russian),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            repeat(rows) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val day = start.plusDays((row * 7 + col).toLong())
                        val active = day == selected
                        Box(Modifier.weight(1f).heightIn(min = 44.dp).padding(2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable(onClickLabel = day.format(longDate)) { onSelect(day) }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val color = when {
                                    active -> MaterialTheme.colorScheme.onPrimary
                                    day == today -> MaterialTheme.colorScheme.primary
                                    month && day.month != selected.month -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(day.dayOfMonth.toString(), color = color, fontWeight = if (day == today || active) FontWeight.Bold else FontWeight.Normal)
                                Box(Modifier.size(4.dp).background(if (day.toEpochDay() in daysWithTasks) color else Color.Transparent, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTasks(title: String, body: String, add: (() -> Unit)? = null) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            if (add != null) TextButton(onClick = add) { Text(stringResource(R.string.ui_taskscreens_35)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: Task, category: Category?, state: TaskState, today: Long, selected: Boolean, selecting: Boolean,
    open: () -> Unit, select: () -> Unit, complete: (Boolean) -> Unit, delete: () -> Unit,
    restore: () -> Unit, drag: Modifier, modifier: Modifier,
) {
    val done = task.completedAt != null
    val color = task.color ?: category?.color
    val subtasks = remember(task.subtasks) { decodeSubtasks(task.subtasks) }
    val date = task.dueDay?.let { LocalDate.ofEpochDay(it).format(shortDate) }
    val details = listOfNotNull(
        date, task.minute?.let { formatTime(it, state.prefs.time24) },
        category?.let { "${it.icon} ${it.name}" },
        priorities[task.priority].takeIf { task.priority > 0 },
        if (subtasks.isEmpty()) null else "Шаги ${subtasks.count { it.done }}/${subtasks.size}",
        "Повтор".takeIf { task.repeat != "NONE" }, "Напоминание".takeIf { task.reminder != null },
    ).joinToString(" · ")
    val swipe = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.StartToEnd) complete(!done)
        if (it == SwipeToDismissBoxValue.EndToStart) delete()
        false
    })
    SwipeToDismissBox(swipe, modifier = modifier.clip(RoundedCornerShape(15.dp)),
        enableDismissFromStartToEnd = !selecting && !task.deleted,
        enableDismissFromEndToStart = !selecting && !task.deleted,
        backgroundContent = {
            val end = swipe.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(Modifier.fillMaxSize().background(if (end) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 20.dp), contentAlignment = if (end) Alignment.CenterEnd else Alignment.CenterStart) {
                Icon(if (end) Icons.Default.Delete else Icons.Default.Check, null)
            }
        }) {
        Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().combinedClickable(onClick = open, onLongClick = select).padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (task.deleted) {
                    TextButton(onClick = restore) { Text(stringResource(R.string.ui_taskscreens_36)) }
                } else {
                    Checkbox(if (selecting) selected else done, { if (selecting) select() else complete(it) },
                        modifier = Modifier.semantics { contentDescription = task.title })
                }
                if (color != null) Spacer(Modifier.padding(end = 8.dp).width(3.dp).height(24.dp).background(Color(color), CircleShape))
                Column(Modifier.weight(1f).padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    if (task.description.isNotBlank()) Text(task.description, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.labelSmall,
                        color = if (!done && (task.dueDay ?: today) < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (task.priority > 0) Spacer(Modifier.padding(horizontal = 6.dp).size(7.dp).background(priorityColors[task.priority], CircleShape))
                if (!task.deleted && !selecting && drag != Modifier) {
                    Box(drag.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Menu, "Удерживайте для перемещения", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(route: String, state: TaskState, model: TaskViewModel, today: LocalDate,
               selectedDay: Long, selectDay: (Long) -> Unit, open: (Task) -> Unit, add: (Long?) -> Unit) {
    val lazy = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val currentState by rememberUpdatedState(state)
    var selection by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableIntStateOf(0) }
    var priority by rememberSaveable { mutableIntStateOf(-1) }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var dateFilter by rememberSaveable { mutableIntStateOf(0) }
    var dragged by remember { mutableStateOf<String?>(null) }
    var displacement by remember { mutableFloatStateOf(0f) }
    var month by rememberSaveable { mutableStateOf(true) }
    var focused by rememberSaveable { mutableStateOf(false) }
    val selected = LocalDate.ofEpochDay(selectedDay)
    val day = today.toEpochDay()
    val all = state.tasks.filterNot { it.deleted }
    val dueToday = all.filter { it.dueDay == day }
    val categories = state.categories.associateBy { it.id }
    val shown = state.tasks.filter { t ->
        if (status == 3) t.deleted else !t.deleted &&
            (status != 1 || t.completedAt == null) && (status != 2 || t.completedAt != null) &&
            (state.prefs.showCompleted || status == 2 || t.completedAt == null) &&
            (route != "calendar" || t.dueDay == selectedDay) &&
            (route != "today" || !focused || t.dueDay == selectedDay) &&
            (category == null || t.categoryId == category) &&
            (priority == -1 || t.priority == priority) &&
            (dateFilter == 0 || when (dateFilter) {
                1 -> t.dueDay == day
                2 -> t.dueDay != null && t.dueDay < day
                3 -> t.dueDay != null && t.dueDay > day
                else -> t.dueDay == null
            }) &&
            (search.isBlank() || t.title.contains(search.trim(), true) || t.description.contains(search.trim(), true) ||
                categories[t.categoryId]?.name?.contains(search.trim(), true) == true)
    }.let { tasks ->
        when (sort) {
            1 -> tasks.sortedWith(compareBy({ it.dueDay ?: Long.MAX_VALUE }, { it.minute ?: Int.MAX_VALUE }))
            2 -> tasks.sortedByDescending { it.priority }
            3 -> tasks.sortedBy { categories[it.categoryId]?.name.orEmpty() }
            4 -> tasks.sortedBy { it.completedAt != null }
            else -> tasks
        }
    }
    val groups = if (route == "calendar" || status == 3 || (route == "all" && sort != 0)) listOf("" to shown) else listOf(
        "Просроченные" to shown.filter { it.completedAt == null && (it.dueDay ?: day) < day },
        "Сегодня" to shown.filter { it.completedAt == null && it.dueDay == day },
        "Предстоящие" to shown.filter { it.completedAt == null && (it.dueDay ?: day) > day },
        "Без срока" to shown.filter { it.completedAt == null && it.dueDay == null },
        "Выполненные" to shown.filter { it.completedAt != null },
    )
    fun pulse() { if (state.prefs.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun select(id: String) {
        pulse()
        selection = ArrayList(if (id in selection) selection - id else selection + id)
    }
    BackHandler(selection.isNotEmpty()) { selection = arrayListOf() }
    LazyColumn(state = lazy, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (route == "today") {
            item {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (state.prefs.name.isBlank()) "Привет!" else "Привет, ${state.prefs.name}!",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(today.format(longDate).replaceFirstChar { it.uppercase(russian) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { TaskProgress(dueToday.count { it.completedAt != null }, dueToday.size, state.prefs.animations) }
        }
        if (route == "calendar") item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectDay((if (month) selected.minusMonths(1) else selected.minusWeeks(1)).toEpochDay()) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Назад")
                }
                Text(selected.format(java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", russian)),
                    Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { selectDay((if (month) selected.plusMonths(1) else selected.plusWeeks(1)).toEpochDay()) }) {
                    Icon(Icons.Default.KeyboardArrowRight, "Вперёд")
                }
                TextButton(onClick = { month = !month }) { Text(if (month) "Неделя" else "Месяц") }
            }
        }
        if (route != "all") item {
            val taskDays = all.mapNotNull { it.dueDay }.toSet()
            val monthView = route == "calendar" && month
            val selectDate: (LocalDate) -> Unit = {
                selectDay(it.toEpochDay())
                focused = route == "today"
            }
            if (state.prefs.animations && route == "calendar") {
                AnimatedContent(monthView, label = "calendar-mode") { fullMonth ->
                    CalendarGrid(selected, today, taskDays, state.prefs.mondayFirst, fullMonth, selectDate)
                }
            } else {
                CalendarGrid(selected, today, taskDays, state.prefs.mondayFirst, monthView, selectDate)
            }
        }
        if (route == "all") item {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text(stringResource(R.string.ui_taskscreens_37)) })
        }
        if (route != "calendar") item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (route == "all") listOf("Все", "Открытые", "Готовые", "Корзина") else listOf("Обзор", "Открытые", "Готовые"))
                    .forEachIndexed { index, name ->
                        FilterChip(status == index && !focused, { status = index; focused = false }, label = { Text(name) })
                    }
                if (focused) FilterChip(true, { focused = false }, label = { Text(selected.format(shortDate)) })
            }
        }
        if (route == "all" && status != 3) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("Категория", category, listOf(null to "Все") + state.categories.map { it.id to it.name }) { category = it }
                Choice("Приоритет", priority, listOf(-1 to "Все") + priorities.mapIndexed { i, s -> i to s }) { priority = it }
                Choice("Дата", dateFilter, listOf(0 to "Любая", 1 to "Сегодня", 2 to "Прошедшие", 3 to "Будущие", 4 to "Без срока")) { dateFilter = it }
                Choice("Порядок", sort, listOf(0 to "Ручной", 1 to "Дата", 2 to "Приоритет", 3 to "Категория", 4 to "Статус")) { sort = it }
            }
        }
        if (selection.isNotEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Выбрано ${selection.size}", Modifier.weight(1f))
                TextButton(onClick = {
                    if (status == 3) model.restore(selection.toSet()) else model.delete(selection.toSet())
                    selection = arrayListOf()
                }) { Text(if (status == 3) "Вернуть" else "В корзину") }
                TextButton(onClick = { selection = arrayListOf() }) { Text(stringResource(R.string.ui_taskscreens_38)) }
            }
        }
        if (shown.isEmpty()) item {
            EmptyTasks(if (status == 3) "Корзина пуста" else "Задач пока нет",
                if (status == 3) "Удалённые задачи появляются здесь." else "Добавьте задачу или измените фильтры.",
                if (status == 3) null else ({ add(selectedDay.takeIf { route == "calendar" || focused }) }))
        }
        groups.filter { it.second.isNotEmpty() }.forEach { (label, entries) ->
            if (label.isNotBlank()) item("header-$label") {
                Text("$label  ${entries.size}", Modifier.padding(top = 10.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(entries, key = { it.id }) { task ->
                val drag = if (sort == 0 && !task.deleted && entries.size > 1) Modifier.pointerInput(task.id, label) {
                    var target: String? = null
                    var position = 0f
                    val entryIds = entries.mapTo(hashSetOf(), Task::id)
                    detectDragGesturesAfterLongPress(onDragStart = {
                        if (currentState.prefs.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragged = task.id
                        displacement = 0f
                        position = lazy.layoutInfo.visibleItemsInfo.firstOrNull { it.key == task.id }?.let { it.offset + it.size / 2f } ?: 0f
                    }, onDragEnd = {
                        target?.let { model.move(task.id, it) }
                        dragged = null
                        displacement = 0f
                    }, onDragCancel = {
                        dragged = null
                        displacement = 0f
                    }) { change, amount ->
                        change.consume()
                        position += amount.y
                        displacement += amount.y
                        target = lazy.layoutInfo.visibleItemsInfo.firstOrNull {
                            position >= it.offset && position < it.offset + it.size && it.key.toString() in entryIds
                        }?.key as? String
                    }
                } else Modifier
                TaskRow(task, categories[task.categoryId], state, day, task.id in selection, selection.isNotEmpty(),
                    open = { if (selection.isNotEmpty() || task.deleted) select(task.id) else open(task) },
                    select = { select(task.id) }, complete = { pulse(); model.complete(task, it) },
                    delete = { pulse(); model.delete(setOf(task.id)) }, restore = { model.restore(setOf(task.id)) },
                    drag = drag, modifier = if (dragged == task.id) Modifier.zIndex(1f).graphicsLayer { translationY = displacement }
                    else if (state.prefs.animations) Modifier.animateItem() else Modifier)
            }
        }
    }
}
