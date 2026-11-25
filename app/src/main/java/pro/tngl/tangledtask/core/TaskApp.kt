package pro.tngl.tangledtask.core

import android.app.Application
import kotlinx.coroutines.*
import pro.tngl.tangledtask.data.TaskDatabase
import pro.tngl.tangledtask.data.TaskRepository
import pro.tngl.tangledtask.notifications.ReminderWork
import pro.tngl.tangledtask.settings.PreferencesStore

class TaskApp : Application() {
    val database by lazy { TaskDatabase.open(this) }
    val preferences by lazy { PreferencesStore(this) }
    val tasks by lazy { TaskRepository(database) { ReminderWork.refresh(this) } }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
