package pro.tngl.tangledtask.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.core.view.WindowCompat
import pro.tngl.tangledtask.settings.Preferences

val taskColors = listOf(0xFF34D399, 0xFF60A5FA, 0xFFC084FC, 0xFFFBBF24, 0xFFF472B6, 0xFFFB7185)
val priorities = listOf("Без приоритета", "Низкий", "Средний", "Высокий", "Срочный")
val priorityColors = listOf(Color.Gray, Color(0xFF60A5FA), Color(0xFFFBBF24), Color(0xFFFB923C), Color(0xFFEF4444))

@Composable
fun PlanningTheme(prefs: Preferences, content: @Composable () -> Unit) {
    val dark = when (prefs.theme) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val spec = if (prefs.animations) tween<Color>(180) else snap()
    val bg by animateColorAsState(when {
        prefs.theme == "amoled" -> Color.Black
        dark -> Color(0xFF0D0F12)
        else -> Color(0xFFF4F7F5)
    }, spec, label = "background")
    val accent by animateColorAsState(Color(prefs.accent), spec, label = "accent")
    val onAccent = if (accent.luminance() > .38f) Color(0xFF101713) else Color.White
    val activity = LocalActivity.current
    SideEffect {
        activity?.window?.let {
            val controller = WindowCompat.getInsetsController(it, it.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    val colors = if (dark) darkColorScheme(
        primary = accent, onPrimary = onAccent, background = bg, surface = Color(0xFF171A20),
        surfaceContainer = Color(0xFF171A20), surfaceVariant = Color(0xFF20242C),
        secondaryContainer = Color(0xFF20382F), onSecondaryContainer = Color(0xFFD3F4E7), onSurface = Color(0xFFF5F5F5),
        onBackground = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFF8B929E),
        outline = Color(0xFF737C8B), error = Color(0xFFEF4444),
    ) else lightColorScheme(
        primary = lerp(accent, Color.Black, .52f), onPrimary = Color.White,
        primaryContainer = accent, onPrimaryContainer = onAccent,
        background = bg, surface = Color.White, surfaceContainer = Color(0xFFE8EEEA),
        surfaceVariant = Color(0xFFEDF2EF), secondaryContainer = accent,
        onSecondaryContainer = onAccent, onBackground = Color(0xFF14201B),
        onSurface = Color(0xFF14201B), onSurfaceVariant = Color(0xFF47564E),
        outline = Color(0xFF69786F), error = Color(0xFFB32032),
    )
    MaterialTheme(colorScheme = colors, content = content)
}
