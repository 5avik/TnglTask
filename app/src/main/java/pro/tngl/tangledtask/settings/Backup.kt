package pro.tngl.tangledtask.settings

import org.json.JSONArray
import org.json.JSONObject
import pro.tngl.tangledtask.data.*
import pro.tngl.tangledtask.domain.RepeatRule

object Backup {
    const val MAX_BYTES = 8 * 1024 * 1024

    fun encode(tasks: List<Task>, categories: List<Category>): String = JSONObject()
        .put("version", 1)
        .put("categories", JSONArray(categories.map {
            JSONObject().put("id", it.id).put("name", it.name).put("color", it.color).put("icon", it.icon)
        }))
        .put("tasks", JSONArray(tasks.map { t ->
            JSONObject().put("id", t.id).put("title", t.title).put("description", t.description)
                .put("category", t.categoryId).put("day", t.dueDay).put("minute", t.minute)
                .put("priority", t.priority).put("reminder", t.reminder).put("repeat", t.repeat)
                .put("weekdays", t.weekdays).put("interval", t.interval).put("anchor", t.anchor)
                .put("subtasks", JSONArray(t.subtasks)).put("color", t.color).put("completed", t.completedAt)
                .put("previous", t.previousId).put("deleted", t.deleted).put("position", t.position)
                .put("snoozed", t.snoozedUntil)
        })).toString(2)

    fun decode(value: String): Pair<List<Task>, List<Category>> {
        val root = JSONObject(value)
        require(root.getInt("version") == 1)
        val categories = root.getJSONArray("categories").objects().map { o ->
            Category(o.getString("id"), o.getString("name"), o.getLong("color"), o.getString("icon")).also {
                require(it.id.isNotBlank() && it.name.isNotBlank() && it.icon.isNotBlank() && it.color in 0..0xFFFFFFFF)
            }
        }
        val categoryIds = categories.map { it.id }.toSet()
        require(categoryIds.size == categories.size)
        val tasks = root.getJSONArray("tasks").objects().map { o ->
            Task(
                id = o.getString("id"), title = o.getString("title"), description = o.getString("description"),
                categoryId = o.optionalString("category"), dueDay = o.optionalLong("day"), minute = o.optionalInt("minute"),
                priority = o.getInt("priority"), reminder = o.optionalInt("reminder"), repeat = o.getString("repeat"),
                weekdays = o.getInt("weekdays"), interval = o.getInt("interval"), anchor = o.optionalInt("anchor"),
                subtasks = o.getJSONArray("subtasks").toString(), color = o.optionalLong("color"),
                completedAt = o.optionalLong("completed"), previousId = o.optionalString("previous"),
                deleted = o.getBoolean("deleted"), position = o.getLong("position"), snoozedUntil = o.optionalLong("snoozed"),
            ).also { t ->
                val repeat = RepeatRule.valueOf(t.repeat)
                require(t.id.isNotBlank() && t.title.isNotBlank() && t.priority in 0..4)
                require(t.categoryId == null || t.categoryId in categoryIds)
                require(t.dueDay == null || t.dueDay in -719162..2932896)
                require(t.minute == null || (t.dueDay != null && t.minute in 0..1439))
                require(t.reminder == null || (t.minute != null && t.reminder in 0..10080))
                require(t.color == null || t.color in 0..0xFFFFFFFF)
                require(t.weekdays in 0..127 && t.interval in 1..3650 && (t.anchor == null || t.anchor in 1..31))
                require(repeat == RepeatRule.NONE || t.dueDay != null)
                require(repeat != RepeatRule.WEEKDAYS || t.weekdays != 0)
                require(decodeSubtasks(t.subtasks).all { it.name.isNotBlank() })
            }
        }
        val byId = tasks.associateBy { it.id }
        val linked = tasks.mapNotNull { it.previousId }
        require(byId.size == tasks.size && linked.size == linked.toSet().size)
        val visited = mutableSetOf<String>()
        val next = tasks.mapNotNull { task -> task.previousId?.let { it to task } }.toMap()
        tasks.filter { it.previousId == null }.forEach { first ->
            var task: Task? = first
            while (task != null) {
                require(visited.add(task.id))
                task = next[task.id]
            }
        }
        require(visited.size == tasks.size)
        return tasks to categories
    }

    private fun JSONArray.objects() = List(length(), ::getJSONObject)
    private fun JSONObject.optionalLong(key: String) = if (isNull(key)) null else getLong(key)
    private fun JSONObject.optionalInt(key: String) = if (isNull(key)) null else getInt(key)
    private fun JSONObject.optionalString(key: String) = if (isNull(key)) null else getString(key)
}
