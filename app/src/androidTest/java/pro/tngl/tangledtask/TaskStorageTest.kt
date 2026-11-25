package pro.tngl.tangledtask

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pro.tngl.tangledtask.data.*
import pro.tngl.tangledtask.settings.Backup
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TaskStorageTest {
    private lateinit var db: TaskDatabase
    private lateinit var repo: TaskRepository

    @Before fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        repo = TaskRepository(db)
    }

    @After fun close() = db.close()

    @Test fun createCompleteRepeatAndUndoAreConsistent() = runBlocking {
        val day = LocalDate.of(2025, 1, 31).toEpochDay()
        val task = Task(title = "Оплатить", dueDay = day, repeat = "MONTHLY",
            subtasks = encodeSubtasks(listOf(Subtask("Проверить счёт", true))))
        repo.save(task)
        assertTrue(repo.complete(task.id, true, 123))
        assertFalse(repo.complete(task.id, true, 456))
        val next = repo.all().single { it.previousId == task.id }
        assertEquals(123L, repo.get(task.id)!!.completedAt)
        assertEquals(LocalDate.of(2025, 2, 28).toEpochDay(), next.dueDay)
        assertFalse(decodeSubtasks(next.subtasks).single().done)
        assertTrue(repo.complete(task.id, false))
        assertNull(repo.get(task.id)!!.completedAt)
        assertNull(repo.get(next.id))
    }

    @Test fun trashRestoreAndBackupRoundTrip() = runBlocking {
        repo.seed()
        val category = repo.snapshot().second.first()
        val task = Task(title = "План", categoryId = category.id)
        repo.save(task)
        repo.setDeleted(setOf(task.id), true)
        assertTrue(repo.get(task.id)!!.deleted)
        repo.setDeleted(setOf(task.id), false)
        val (tasks, categories) = repo.snapshot()
        val decoded = Backup.decode(Backup.encode(tasks, categories))
        repo.restore(decoded.first, decoded.second)
        assertEquals(tasks, repo.all())
        assertEquals(categories.toSet(), repo.snapshot().second.toSet())
    }

    @Test fun backupRejectsBrokenRecurrenceChain() {
        val task = Task(title = "План", previousId = "missing")
        assertThrows(IllegalArgumentException::class.java) { Backup.decode(Backup.encode(listOf(task), emptyList())) }
    }
}
