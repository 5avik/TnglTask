package pro.tngl.tangledtask.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.store by preferencesDataStore("preferences")

data class Preferences(
    val name: String = "",
    val theme: String = "system",
    val accent: Long = 0xFF34D399,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    val notifications: Boolean = true,
    val mondayFirst: Boolean = true,
    val time24: Boolean = true,
    val showCompleted: Boolean = true,
    val dotTypography: Boolean = true,
)

class PreferencesStore(context: Context) {
    private val store = context.store
    private val name = stringPreferencesKey("name")
    private val theme = stringPreferencesKey("theme")
    private val accent = longPreferencesKey("accent")
    private val animations = booleanPreferencesKey("animations")
    private val haptics = booleanPreferencesKey("haptics")
    private val notifications = booleanPreferencesKey("notifications")
    private val mondayFirst = booleanPreferencesKey("mondayFirst")
    private val time24 = booleanPreferencesKey("time24")
    private val showCompleted = booleanPreferencesKey("showCompleted")
    private val dotTypography = booleanPreferencesKey("dotTypography")

    val values = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map(::read)

    private fun read(p: androidx.datastore.preferences.core.Preferences) = Preferences(
        p[name].orEmpty(), p[theme] ?: "system", p[accent] ?: 0xFF34D399,
        p[animations] ?: true, p[haptics] ?: true, p[notifications] ?: true,
        p[mondayFirst] ?: true, p[time24] ?: true, p[showCompleted] ?: true, p[dotTypography] ?: true,
    )

    suspend fun edit(change: (Preferences) -> Preferences) {
        store.edit { p ->
            val value = change(read(p))
            p[name] = value.name
            p[theme] = value.theme
            p[accent] = value.accent
            p[animations] = value.animations
            p[haptics] = value.haptics
            p[notifications] = value.notifications
            p[mondayFirst] = value.mondayFirst
            p[time24] = value.time24
            p[showCompleted] = value.showCompleted
            p[dotTypography] = value.dotTypography
        }
    }
}
