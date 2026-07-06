package tech.acachi.ytplucker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette ported from the desktop yt-grab styles.css.
val Bg = Color(0xFF0F1115)
val Panel = Color(0xFF181B22)
val Panel2 = Color(0xFF1F232C)
val BorderCol = Color(0xFF2A2F3A)
val TextCol = Color(0xFFE8EAF0)
val TextDim = Color(0xFF9AA1AF)
val Accent = Color(0xFFFF3D3D)
val AccentDim = Color(0xFFB32020)
val Ok = Color(0xFF3ECF6E)
val Warn = Color(0xFFF0B429)

private val YtPluckerColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentDim,
    background = Bg,
    onBackground = TextCol,
    surface = Panel,
    onSurface = TextCol,
    surfaceVariant = Panel2,
    onSurfaceVariant = TextDim,
    outline = BorderCol,
    error = Warn,
)

@Composable
fun YtPluckerTheme(
    // Always dark — the desktop app is dark-only; ignore system setting but keep the param.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = YtPluckerColors,
        content = content,
    )
}
