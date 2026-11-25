package pro.tngl.tangledtask.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import pro.tngl.tangledtask.settings.Preferences

val taskColors = listOf(0xFFD71921, 0xFF34D399, 0xFF60A5FA, 0xFFC084FC, 0xFFFBBF24, 0xFFF472B6, 0xFFFB7185)
val priorities = listOf("Без приоритета", "Низкий", "Средний", "Высокий", "Срочный")

@Composable
fun NothingTheme(prefs: Preferences, content: @Composable () -> Unit) {
    val dark = when (prefs.theme) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val spec = if (prefs.animations) tween<Color>(180) else snap()
    val bg by animateColorAsState(when {
        prefs.theme == "amoled" -> Color.Black
        dark -> Color(0xFF000000)
        else -> Color(0xFFF5F5F5)
    }, spec, label = "background")
    val activity = LocalActivity.current
    SideEffect {
        activity?.window?.let {
            val controller = WindowCompat.getInsetsController(it, it.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    val colors = NothingColors(dark, bg)
    CompositionLocalProvider(
        LocalDotTypography provides prefs.dotTypography,
        LocalNothingMotion provides prefs.animations,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = NothingTypography,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}

private val NothingTypography = Typography()

private fun NothingColors(dark: Boolean, background: Color): ColorScheme {
    val surface = if (dark) Color(0xFF141414) else Color.White
    val variant = if (dark) Color(0xFF262626) else Color(0xFFE8E8E8)
    val ink = if (dark) Color(0xFFF5F5F5) else Color(0xFF111111)
    val muted = if (dark) Color(0xFF909090) else Color(0xFF666666)
    val outline = if (dark) Color(0xFF303030) else Color(0xFFD5D5D5)
    val red = Color(0xFFD71921)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = red, onPrimary = Color.White, primaryContainer = variant, onPrimaryContainer = ink,
        secondary = ink, onSecondary = background, secondaryContainer = variant, onSecondaryContainer = ink,
        tertiary = ink, onTertiary = background, tertiaryContainer = variant, onTertiaryContainer = ink,
        background = background, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = variant, onSurfaceVariant = muted, surfaceTint = Color.Transparent,
        surfaceBright = variant, surfaceDim = background, surfaceContainerLowest = background,
        surfaceContainerLow = surface, surfaceContainer = surface, surfaceContainerHigh = variant,
        surfaceContainerHighest = variant, outline = outline, outlineVariant = outline,
        inverseSurface = ink, inverseOnSurface = surface, inversePrimary = if (dark) red else Color(0xFFFF7479),
        error = if (dark) Color(0xFFFF7479) else Color(0xFFB3141B),
        errorContainer = variant, onErrorContainer = ink,
    )
}
