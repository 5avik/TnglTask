package pro.tngl.tangledtask

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pro.tngl.tangledtask.core.TaskApp
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createAndCompleteFromToday() {
        val title = "Task ${UUID.randomUUID().toString().take(8)}"
        val app = compose.activity.application as TaskApp
        try {
            compose.onNodeWithContentDescription("Создать задачу").performClick()
            compose.onNode(hasSetTextAction() and hasText("Название")).performTextInput(title)
            compose.onNodeWithText("Создать задачу").performClick()
            val checkbox = hasContentDescription(title) and isToggleable()
            compose.waitUntil(10_000) { compose.onAllNodes(checkbox).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(checkbox).assertIsOff().performClick()
            compose.waitUntil(10_000) { runBlocking { app.tasks.all().singleOrNull { it.title == title }?.completedAt != null } }
        } finally {
            runBlocking {
                app.tasks.all().filter { it.title == title }.forEach { app.database.dao().remove(it.id) }
            }
        }
    }
}
