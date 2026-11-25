package pro.tngl.tangledtask.navigation

import androidx.compose.ui.res.stringResource
import pro.tngl.tangledtask.R

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import pro.tngl.tangledtask.ui.*
import java.time.LocalDate

private val routes = listOf("today", "calendar", "all", "stats")
private val titles = listOf("Сегодня", "Календарь", "Все задачи", "Статистика")
private val icons = listOf(Icons.Default.Home, Icons.Default.DateRange, Icons.Default.List, Icons.Default.Star)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAppContent(model: TaskViewModel, launchAction: String?, handled: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: "today"
    val snack = remember { SnackbarHostState() }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var editorDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var todaySelected by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var calendarSelected by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val next = LocalDate.now()
                if (todaySelected == today.toEpochDay()) todaySelected = next.toEpochDay()
                if (calendarSelected == today.toEpochDay()) calendarSelected = next.toEpochDay()
                today = next
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(model) {
        model.feedback.collect { message ->
            val result = snack.showSnackbar(message.text, actionLabel = if (message.undo != null) "Отменить" else null,
                withDismissAction = message.undo != null, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) message.undo?.invoke()
        }
    }
    fun add(day: Long?) { editorDay = day; editor = "new" }
    LaunchedEffect(launchAction, state.loading) {
        if (launchAction == "new") { add(today.toEpochDay()); handled() }
        if (!state.loading && launchAction?.startsWith("task:") == true) {
            editor = state.tasks.firstOrNull { !it.deleted && it.id == launchAction.removePrefix("task:") }?.id
            handled()
        }
    }
    fun choose(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    NothingTheme(state.prefs) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 760.dp
            Scaffold(
                topBar = {
                    TopAppBar(title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            NothingDotMatrixText(
                                if (route == "settings") "НАСТРОЙКИ" else if (route == "today") "TNGLTASK" else titles[routes.indexOf(route)].uppercase(),
                                MaterialTheme.typography.headlineSmall,
                            )
                        }
                    },
                        navigationIcon = {
                            if (route == "settings") NothingIconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Назад")
                            }
                        }, actions = {
                            if (route != "settings") IconButton(onClick = { nav.navigate("settings") }) {
                                Icon(Icons.Default.Settings, "Настройки")
                            }
                        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
                },
                bottomBar = {
                    if (!wide && route in routes) NothingNavigationBar(routes.indexOf(route), titles) { choose(routes[it]) }
                },
                snackbarHost = { SnackbarHost(snack) },
                floatingActionButton = {
                    if (route in routes.take(3)) FloatingActionButton(
                        onClick = { add(if (route == "calendar") calendarSelected else if (route == "today") todaySelected else null) },
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Icon(Icons.Default.Add, "Создать задачу") }
                },
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                    if (wide && route in routes) NavigationRail {
                        routes.forEachIndexed { index, target ->
                            NavigationRailItem(target == route, { choose(target) },
                                icon = { Icon(icons[index], null) }, label = { Text(titles[index]) })
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        NavHost(nav, startDestination = "today", modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                            enterTransition = { if (state.prefs.animations) fadeIn(tween(170)) else EnterTransition.None },
                            exitTransition = { if (state.prefs.animations) fadeOut(tween(120)) else ExitTransition.None }) {
                            routes.take(3).forEach { screen ->
                                composable(screen) {
                                    when {
                                        state.loading -> StatusScreen("Загрузка задач…", true)
                                        state.failed -> StatusScreen("Не удалось загрузить задачи", false)
                                        else -> TaskScreen(screen, state, model, today,
                                            if (screen == "calendar") calendarSelected else todaySelected,
                                            { if (screen == "calendar") calendarSelected = it else todaySelected = it },
                                            { editorDay = null; editor = it.id }, ::add)
                                    }
                                }
                            }
                            composable("stats") {
                                when {
                                    state.loading -> StatusScreen("Загрузка статистики…", true)
                                    state.failed -> StatusScreen("Не удалось загрузить статистику", false)
                                    else -> StatisticsScreen(state, today)
                                }
                            }
                            composable("settings") { SettingsScreen(state, model) }
                        }
                    }
                }
            }
        }
        editor?.let { id ->
            val task = state.tasks.firstOrNull { !it.deleted && it.id == id }
            if (id == "new" || task != null) key(id) { TaskEditor(task, editorDay, state, model) { editor = null } }
        }
    }
}

@Composable
private fun StatusScreen(message: String, loading: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (loading) CircularProgressIndicator(Modifier.size(26.dp))
        Text(message, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
    }
}
