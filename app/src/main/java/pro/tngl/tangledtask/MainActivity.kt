package pro.tngl.tangledtask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pro.tngl.tangledtask.navigation.TaskAppContent
import pro.tngl.tangledtask.notifications.ReminderWork
import pro.tngl.tangledtask.ui.TaskViewModel

class MainActivity : ComponentActivity() {
    private val model by viewModels<TaskViewModel>()
    private var launchAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) launchAction = intent.appAction()
        setContent { TaskAppContent(model, launchAction) { launchAction = null } }
    }

    override fun onResume() {
        super.onResume()
        ReminderWork.refresh(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchAction = intent.appAction()
    }

    private fun Intent.appAction(): String? = when {
        data?.scheme != "tngltask" -> null
        data?.host == "new" -> "new"
        data?.host == "task" -> data?.lastPathSegment?.let { "task:$it" }
        else -> null
    }
}
