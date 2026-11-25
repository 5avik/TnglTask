package pro.tngl.tangledtask.ui

import androidx.compose.ui.res.stringResource
import pro.tngl.tangledtask.R

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pro.tngl.tangledtask.data.*
import pro.tngl.tangledtask.domain.RepeatRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

val russian = Locale.forLanguageTag("ru-RU")
val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", russian)
val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", russian)
fun formatTime(minute: Int, time24: Boolean): String = LocalTime.ofSecondOfDay(minute * 60L)
    .format(DateTimeFormatter.ofPattern(if (time24) "HH:mm" else "h:mm a", russian))

val repeatNames = listOf("Без повторения", "Каждый день", "Каждую неделю", "Каждый месяц", "Дни недели", "Интервал")
private val reminderOptions = listOf(null to "Без напоминания", 0 to "В срок", 5 to "За 5 мин", 15 to "За 15 мин",
    30 to "За 30 мин", 60 to "За час", 1440 to "За день")

@Composable
fun <T> Choice(label: String, selected: T, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$label · ${options.firstOrNull { it.first == selected }?.second.orEmpty()}")
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (value, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}

@Composable
fun ColorChoices(selected: Long?, onSelect: (Long) -> Unit) {
    val names = listOf("Красный", "Изумрудный", "Голубой", "Сиреневый", "Янтарный", "Розовый", "Коралловый")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        taskColors.forEachIndexed { index, color ->
            Box(Modifier.size(44.dp).semantics { contentDescription = names[index] }
                .selectable(selected == color, role = Role.RadioButton, onClick = { onSelect(color) }),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(24.dp).background(Color(color), CircleShape)
                    .border(if (selected == color) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditor(original: Task?, initialDay: Long?, state: TaskState, model: TaskViewModel, close: () -> Unit) {
    val context = LocalContext.current
    val id = rememberSaveable { original?.id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(original?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(original?.description.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(original?.categoryId) }
    var day by rememberSaveable { mutableStateOf(original?.dueDay ?: initialDay) }
    var minute by rememberSaveable { mutableStateOf(original?.minute) }
    var priority by rememberSaveable { mutableIntStateOf(original?.priority ?: 0) }
    var reminder by rememberSaveable { mutableStateOf(original?.reminder) }
    var repeat by rememberSaveable { mutableStateOf(original?.repeat ?: RepeatRule.NONE.name) }
    var weekdays by rememberSaveable { mutableIntStateOf(original?.weekdays ?: 0) }
    var interval by rememberSaveable { mutableStateOf((original?.interval ?: 1).toString()) }
    var color by rememberSaveable { mutableStateOf(original?.color) }
    var checklist by rememberSaveable { mutableStateOf(original?.subtasks ?: "[]") }
    var newSubtask by rememberSaveable { mutableStateOf("") }
    var delete by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun persist(task: Task) {
        saving = true
        model.save(task) { saved ->
            saving = false
            if (saved) {
                if (task.reminder != null && state.prefs.notifications && Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                close()
            }
        }
    }
    val subtasks = remember(checklist) { decodeSubtasks(checklist) }
    val valid = title.isNotBlank() && (repeat != RepeatRule.WEEKDAYS.name || weekdays != 0) &&
        (repeat != RepeatRule.INTERVAL.name || interval.toIntOrNull() in 1..3650)
    fun draft() = (original ?: Task(id = id)).copy(
        title = title, description = description, categoryId = category, dueDay = day,
        minute = minute.takeIf { day != null }, priority = priority,
        reminder = reminder.takeIf { day != null && minute != null },
        repeat = if (day == null) RepeatRule.NONE.name else repeat, weekdays = weekdays,
        interval = interval.toIntOrNull()?.coerceIn(1, 3650) ?: 1, subtasks = checklist, color = color,
        anchor = day?.let { if (it == original?.dueDay) original?.anchor else LocalDate.ofEpochDay(it).dayOfMonth },
    )
    NothingBottomSheet(onDismissRequest = { if (!saving) close() }) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NothingDotMatrixText(if (original == null) "Новая задача" else "Редактирование", MaterialTheme.typography.headlineSmall)
            NothingTextField(title, { title = it }, label = { Text(stringResource(R.string.ui_settingsscreen_18)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            NothingButton(onClick = { persist(draft()) }, enabled = valid && !saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (original == null) "Создать задачу" else "Сохранить")
            }
            NothingTextField(description, { description = it }, label = { Text(stringResource(R.string.ui_taskeditor_21)) }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Категория", category, listOf(null to "Без категории") + state.categories.map { it.id to "${it.icon} ${it.name}" }) { category = it }
                Choice("Приоритет", priority, priorities.mapIndexed { i, name -> i to name }) { priority = it }
                OutlinedButton(onClick = { picker = "date" }) { Text(day?.let { LocalDate.ofEpochDay(it).format(shortDate) } ?: "Дата") }
                if (day != null) {
                    TextButton(onClick = { day = null; minute = null; reminder = null; repeat = RepeatRule.NONE.name }) { Text(stringResource(R.string.ui_taskeditor_22)) }
                    OutlinedButton(onClick = { picker = "time" }) { Text(minute?.let { formatTime(it, state.prefs.time24) } ?: "Время") }
                    if (minute != null) {
                        TextButton(onClick = { minute = null; reminder = null }) { Text(stringResource(R.string.ui_taskeditor_23)) }
                        Choice("Напоминание", reminder, reminderOptions) { reminder = it }
                    }
                    Choice("Повтор", repeat, RepeatRule.entries.map { it.name to repeatNames[it.ordinal] }) { repeat = it }
                }
            }
            if (day != null && repeat == RepeatRule.WEEKDAYS.name) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { index, name ->
                        val bit = 1 shl index
                        FilterChip(weekdays and bit != 0, { weekdays = weekdays xor bit }, label = { Text(name) })
                    }
                }
            }
            if (day != null && repeat == RepeatRule.INTERVAL.name) {
                NothingTextField(interval, { interval = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.ui_taskeditor_24)) }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            }
            HorizontalDivider()
            Text(stringResource(R.string.ui_taskeditor_25), style = MaterialTheme.typography.titleMedium)
            ColorChoices(color) { color = if (it == color) null else it }
            Text(stringResource(R.string.ui_taskeditor_26), style = MaterialTheme.typography.titleMedium)
            subtasks.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NothingCheckbox(item.done, { done ->
                        checklist = encodeSubtasks(subtasks.mapIndexed { i, sub -> if (i == index) sub.copy(done = done) else sub })
                    }, modifier = Modifier.semantics { contentDescription = item.name })
                    Text(item.name, Modifier.weight(1f))
                    TextButton(onClick = { checklist = encodeSubtasks(subtasks.filterIndexed { i, _ -> i != index }) }) { Text(stringResource(R.string.ui_taskeditor_27)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                NothingTextField(newSubtask, { newSubtask = it }, label = { Text(stringResource(R.string.ui_taskeditor_28)) },
                    modifier = Modifier.weight(1f), singleLine = true)
                TextButton(enabled = newSubtask.isNotBlank(), onClick = {
                    checklist = encodeSubtasks(subtasks + Subtask(newSubtask.trim()))
                    newSubtask = ""
                }) { Text(stringResource(R.string.ui_settingsscreen_8)) }
            }
            if (original != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = valid && !saving, onClick = {
                        persist(draft().copy(id = UUID.randomUUID().toString(), previousId = null,
                            completedAt = null, deleted = false, position = System.currentTimeMillis()))
                    }) { Text(stringResource(R.string.ui_taskeditor_29)) }
                    TextButton(onClick = { delete = true }) { Text(stringResource(R.string.ui_taskeditor_30), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    when (picker) {
        "date" -> {
            val selected = day ?: LocalDate.now().toEpochDay()
            val year = LocalDate.ofEpochDay(selected).year
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = selected * 86_400_000L,
                yearRange = minOf(1900, year)..maxOf(2100, year),
            )
            DatePickerDialog(onDismissRequest = { picker = null }, confirmButton = {
                TextButton(enabled = dateState.selectedDateMillis != null, onClick = {
                    day = dateState.selectedDateMillis?.let { Math.floorDiv(it, 86_400_000L) }
                    picker = null
                }) { Text("Выбрать") }
            }, dismissButton = { TextButton(onClick = { picker = null }) { Text("Отмена") } }) {
                DatePicker(dateState, title = {
                    NothingDotMatrixText("ДАТА", MaterialTheme.typography.titleLarge, Modifier.padding(16.dp))
                }, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }
        "time" -> {
            val initial = minute ?: 540
            val clock = rememberTimePickerState(initial / 60, initial % 60, state.prefs.time24)
            AlertDialog(onDismissRequest = { picker = null },
                title = { NothingDotMatrixText("ВРЕМЯ", MaterialTheme.typography.headlineSmall) },
                text = { TimeInput(clock) },
                confirmButton = { TextButton(onClick = {
                    minute = clock.hour * 60 + clock.minute
                    picker = null
                }) { Text("Выбрать") } },
                dismissButton = { TextButton(onClick = { picker = null }) { Text("Отмена") } },
            )
        }
    }
    if (delete && original != null) AlertDialog(onDismissRequest = { delete = false },
        title = { Text(stringResource(R.string.ui_taskeditor_31)) },
        confirmButton = { TextButton(onClick = { model.delete(setOf(original.id)); close() }) { Text(stringResource(R.string.ui_taskeditor_32)) } },
        dismissButton = { TextButton(onClick = { delete = false }) { Text(stringResource(R.string.ui_settingsscreen_17)) } })
}
