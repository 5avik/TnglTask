package pro.tngl.tangledtask.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import pro.tngl.tangledtask.domain.RepeatRule
import pro.tngl.tangledtask.domain.nextDate
import java.time.LocalDate
import java.util.UUID

data class Subtask(val name: String, val done: Boolean = false)

fun decodeSubtasks(value: String): List<Subtask> = JSONArray(value).let { array ->
    List(array.length()) { index ->
        val item = array.getJSONObject(index)
        Subtask(item.getString("name"), item.getBoolean("done"))
    }
}

fun encodeSubtasks(items: List<Subtask>): String = JSONArray().apply {
    items.forEach { put(JSONObject().put("name", it.name).put("done", it.done)) }
}.toString()

class TaskRepository(private val db: TaskDatabase, private val changed: () -> Unit = {}) {
    private val dao = db.dao()
    val tasks = dao.observe()
    val categories = dao.observeCategories()

    suspend fun all() = dao.all()
    suspend fun get(id: String) = dao.get(id)

    suspend fun seed() = db.withTransaction {
        if (dao.categories().isEmpty()) {
            val names = listOf("Работа", "Личное", "Покупки", "Учёба", "Дом", "Проекты")
            val colors = listOf(0xFF60A5FA, 0xFFC084FC, 0xFFFBBF24, 0xFF2DD4BF, 0xFFF472B6, 0xFF34D399)
            names.forEachIndexed { i, name ->
                dao.putCategory(Category("default-$i", name, colors[i], listOf("◆", "♡", "▣", "✦", "⌂", "◇")[i]))
            }
        }
    }

    suspend fun save(task: Task) {
        require(task.title.isNotBlank())
        db.withTransaction {
            val stored = dao.get(task.id)
            dao.put(task.copy(title = task.title.trim(), completedAt = stored?.completedAt,
                deleted = stored?.deleted ?: false, snoozedUntil = null))
        }
        changed()
    }

    suspend fun saveCategory(category: Category) {
        require(category.name.isNotBlank() && category.icon.isNotBlank())
        dao.putCategory(category.copy(name = category.name.trim()))
        changed()
    }

    suspend fun setDeleted(ids: Set<String>, deleted: Boolean) {
        db.withTransaction {
            ids.forEach { id -> dao.get(id)?.let { dao.put(it.copy(deleted = deleted, snoozedUntil = null)) } }
        }
        changed()
    }

    suspend fun snooze(id: String, until: Long) {
        db.withTransaction {
            dao.get(id)?.takeIf { !it.deleted && it.completedAt == null }?.let {
                dao.put(it.copy(snoozedUntil = until))
            }
        }
        changed()
    }

    suspend fun move(source: String, target: String) {
        db.withTransaction {
            val ordered = dao.all().toMutableList()
            val from = ordered.indexOfFirst { it.id == source }
            val to = ordered.indexOfFirst { it.id == target }
            if (from >= 0 && to >= 0 && from != to) {
                ordered.add(to, ordered.removeAt(from))
                ordered.forEachIndexed { index, task ->
                    if (task.position != index.toLong()) dao.put(task.copy(position = index.toLong()))
                }
            }
        }
        changed()
    }

    suspend fun complete(id: String, done: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        val updated = db.withTransaction {
            val task = dao.get(id) ?: return@withTransaction false
            if (task.deleted || (task.completedAt != null) == done) return@withTransaction false
            val child = dao.next(id)
            if (!done && child?.completedAt != null) return@withTransaction false
            dao.put(task.copy(completedAt = now.takeIf { done }, snoozedUntil = null))
            if (!done && child != null) dao.remove(child.id)
            if (done && child == null && task.dueDay != null) {
                val date = LocalDate.ofEpochDay(task.dueDay)
                val next = nextDate(date, RepeatRule.valueOf(task.repeat), task.weekdays, task.interval, task.anchor ?: date.dayOfMonth)
                if (next != null) {
                    dao.put(task.copy(id = UUID.randomUUID().toString(), dueDay = next.toEpochDay(),
                        anchor = task.anchor ?: date.dayOfMonth, previousId = id, completedAt = null,
                        snoozedUntil = null, position = now,
                        subtasks = encodeSubtasks(decodeSubtasks(task.subtasks).map { it.copy(done = false) })))
                }
            }
            true
        }
        if (updated) changed()
        return updated
    }

    suspend fun snapshot(): Pair<List<Task>, List<Category>> = db.withTransaction { dao.all() to dao.categories() }

    suspend fun restore(tasks: List<Task>, categories: List<Category>) {
        db.withTransaction {
            dao.clearTasks()
            dao.clearCategories()
            categories.forEach { dao.putCategory(it) }
            tasks.forEach { dao.put(it) }
        }
        changed()
    }
}
