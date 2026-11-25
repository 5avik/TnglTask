package pro.tngl.tangledtask.ui

import androidx.compose.ui.res.stringResource
import pro.tngl.tangledtask.R

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import pro.tngl.tangledtask.data.Category

@Composable
fun SettingsScreen(state: TaskState, model: TaskViewModel) {
    val prefs = state.prefs
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var importUri by rememberSaveable { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(model::export) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it?.toString() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.prefs { it.copy(notifications = true) }
    }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SettingsCard("Персонализация") {
                NothingTextField(name ?: prefs.name, { name = it.take(60) }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.ui_settingsscreen_1)) }, singleLine = true)
                if (name != null) TextButton(onClick = {
                    val value = name.orEmpty().trim()
                    model.prefs { it.copy(name = value) }
                    name = null
                }) { Text(stringResource(R.string.ui_settingsscreen_2)) }
                Choice("Тема", prefs.theme, listOf("system" to "Nothing System", "light" to "Nothing Light", "dark" to "Nothing Dark", "amoled" to "AMOLED")) { key ->
                    model.prefs { it.copy(theme = key) }
                }
                SettingSwitch("Точечная типографика", prefs.dotTypography) { value -> model.prefs { it.copy(dotTypography = value) } }
                SettingSwitch("Анимации", prefs.animations) { value -> model.prefs { it.copy(animations = value) } }
                SettingSwitch("Тактильная отдача", prefs.haptics) { value -> model.prefs { it.copy(haptics = value) } }
            }
        }
        item {
            SettingsCard("Планирование") {
                Choice("Первый день недели", prefs.mondayFirst, listOf(true to "Понедельник", false to "Воскресенье")) { value ->
                    model.prefs { it.copy(mondayFirst = value) }
                }
                Choice("Формат времени", prefs.time24, listOf(true to "24 часа", false to "12 часов")) { value ->
                    model.prefs { it.copy(time24 = value) }
                }
                SettingSwitch("Показывать выполненные задачи", prefs.showCompleted) { value -> model.prefs { it.copy(showCompleted = value) } }
            }
        }
        item {
            SettingsCard("Уведомления") {
                SettingSwitch("Напоминания о задачах", prefs.notifications) { enabled ->
                    model.prefs { it.copy(notifications = enabled) }
                    if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestNotifications()
                }
                TextButton(onClick = ::requestNotifications) { Text(stringResource(R.string.ui_settingsscreen_4)) }
                if (Build.VERSION.SDK_INT >= 31 && !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                    Text(stringResource(R.string.ui_settingsscreen_5), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                    }) { Text(stringResource(R.string.ui_settingsscreen_6)) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_settingsscreen_7), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { category = "new" }) { Text(stringResource(R.string.ui_settingsscreen_8)) }
            }
        }
        items(state.categories, key = { it.id }) { entry ->
            Surface(onClick = { category = entry.id }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(entry.icon, color = androidx.compose.ui.graphics.Color(entry.color))
                    Text(entry.name)
                }
            }
        }
        item {
            SettingsCard("Резервная копия") {
                Text(stringResource(R.string.ui_settingsscreen_9), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { export.launch("TnglTask.json") }) { Text(stringResource(R.string.ui_settingsscreen_10)) }
                TextButton(onClick = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text(stringResource(R.string.ui_settingsscreen_11)) }
            }
        }
        item { SettingsCard("О приложении") { Text(stringResource(R.string.ui_settingsscreen_12)); Text(stringResource(R.string.ui_settingsscreen_13)) } }
    }
    category?.let { id ->
        key(id) { CategoryDialog(state.categories.firstOrNull { it.id == id }, { category = null }) { model.category(it); category = null } }
    }
    importUri?.let { value ->
        AlertDialog(onDismissRequest = { importUri = null }, title = { Text(stringResource(R.string.ui_settingsscreen_14)) },
            text = { Text(stringResource(R.string.ui_settingsscreen_15)) },
            confirmButton = { TextButton(onClick = { model.import(Uri.parse(value)); importUri = null }) { Text(stringResource(R.string.ui_settingsscreen_16)) } },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text(stringResource(R.string.ui_settingsscreen_17)) } })
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    NothingCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SettingSwitch(label: String, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        NothingSwitch(enabled, change, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun CategoryDialog(original: Category?, close: () -> Unit, save: (Category) -> Unit) {
    var name by rememberSaveable { mutableStateOf(original?.name.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(original?.icon ?: "◇") }
    var color by rememberSaveable { mutableLongStateOf(original?.color ?: taskColors.first()) }
    AlertDialog(onDismissRequest = close, title = { Text(if (original == null) "Новая категория" else "Изменить категорию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NothingTextField(name, { name = it.take(60) }, label = { Text(stringResource(R.string.ui_settingsscreen_18)) }, singleLine = true)
                NothingTextField(icon, { if (it.length <= 8) icon = it }, label = { Text(stringResource(R.string.ui_settingsscreen_19)) }, singleLine = true)
                ColorChoices(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && icon.isNotBlank(), onClick = {
                save(original?.copy(name = name, color = color, icon = icon) ?: Category(name = name, color = color, icon = icon))
            }) { Text(stringResource(R.string.ui_settingsscreen_20)) }
        }, dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.ui_settingsscreen_17)) } })
}
