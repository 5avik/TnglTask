package pro.tngl.tangledtask.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "tasks", indices = [Index("dueDay"), Index(value = ["previousId"], unique = true)])
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val dueDay: Long? = null,
    val minute: Int? = null,
    val priority: Int = 0,
    val reminder: Int? = null,
    val repeat: String = "NONE",
    val weekdays: Int = 0,
    val interval: Int = 1,
    val anchor: Int? = null,
    val subtasks: String = "[]",
    val color: Long? = null,
    val completedAt: Long? = null,
    val previousId: String? = null,
    val deleted: Boolean = false,
    val position: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL") val snoozedUntil: Long? = null,
)

@Entity(tableName = "categories")
data class Category(@PrimaryKey val id: String = UUID.randomUUID().toString(), val name: String, val color: Long, val icon: String)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY position, id") fun observe(): Flow<List<Task>>
    @Query("SELECT * FROM categories ORDER BY name") fun observeCategories(): Flow<List<Category>>
    @Query("SELECT * FROM tasks ORDER BY position, id") suspend fun all(): List<Task>
    @Query("SELECT * FROM categories") suspend fun categories(): List<Category>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun get(id: String): Task?
    @Query("SELECT * FROM tasks WHERE previousId = :id") suspend fun next(id: String): Task?
    @Upsert suspend fun put(task: Task)
    @Upsert suspend fun putCategory(category: Category)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun remove(id: String)
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM categories") suspend fun clearCategories()
}

@Database(entities = [Task::class, Category::class], version = 2, exportSchema = true)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        fun open(context: Context): TaskDatabase = Room.databaseBuilder(context, TaskDatabase::class.java, "tasks.db")
            .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN snoozedUntil INTEGER DEFAULT NULL")
                }
            }).build()
    }
}
