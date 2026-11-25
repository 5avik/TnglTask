package pro.tngl.tangledtask.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.tngl.tangledtask.core.TaskApp
import pro.tngl.tangledtask.data.*
import pro.tngl.tangledtask.notifications.ReminderWork
import pro.tngl.tangledtask.settings.*

data class TaskState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val prefs: Preferences = Preferences(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

data class Feedback(val text: String, val undo: (() -> Unit)? = null)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as TaskApp
    private val repo = app.tasks
    val state = combine(repo.tasks, repo.categories, app.preferences.values) { tasks, categories, prefs ->
        TaskState(tasks, categories, prefs, loading = false)
    }.catch { emit(TaskState(loading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskState())

    private val messages = Channel<Feedback>(Channel.BUFFERED)
    val feedback = messages.receiveAsFlow()

    init { execute { repo.seed(); ReminderWork.refresh(app) } }

    private fun execute(failure: String = "Не удалось выполнить действие", block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                messages.send(Feedback(failure))
            }
        }
    }

    fun save(task: Task, finished: (Boolean) -> Unit) = execute {
        var saved = false
        try {
            repo.save(task)
            saved = true
        } finally {
            finished(saved)
        }
    }
    fun category(category: Category) = execute { repo.saveCategory(category) }
    fun move(source: String, target: String) = execute { repo.move(source, target) }

    fun complete(task: Task, done: Boolean) = execute {
        if (repo.complete(task.id, done)) {
            messages.send(Feedback(if (done) "Задача выполнена" else "Задача снова открыта") {
                execute {
                    if (!repo.complete(task.id, !done)) messages.send(Feedback("Состояние задачи уже изменилось"))
                }
            })
        } else messages.send(Feedback("Состояние задачи уже изменилось"))
    }

    fun delete(ids: Set<String>) = execute {
        repo.setDeleted(ids, true)
        messages.send(Feedback("Перемещено в корзину") { restore(ids) })
    }

    fun restore(ids: Set<String>) = execute { repo.setDeleted(ids, false) }

    fun prefs(change: (Preferences) -> Preferences) = execute {
        val enabled = app.preferences.values.first().notifications
        app.preferences.edit(change)
        if (app.preferences.values.first().notifications != enabled) ReminderWork.refresh(app)
    }

    fun export(uri: Uri) = execute("Не удалось сохранить резервную копию") {
        val (tasks, categories) = repo.snapshot()
        withContext(Dispatchers.IO) {
            val bytes = Backup.encode(tasks, categories).toByteArray()
            require(bytes.size <= Backup.MAX_BYTES)
            requireNotNull(app.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
        }
        messages.send(Feedback("Резервная копия сохранена"))
    }

    fun import(uri: Uri) = execute("Не удалось прочитать резервную копию") {
        val (tasks, categories) = withContext(Dispatchers.IO) {
            val bytes = requireNotNull(app.contentResolver.openInputStream(uri)).use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= Backup.MAX_BYTES)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            Backup.decode(bytes.toString(Charsets.UTF_8))
        }
        repo.restore(tasks, categories)
        messages.send(Feedback("Резервная копия восстановлена"))
    }
}
