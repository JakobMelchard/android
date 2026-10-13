package io.jakobmelchard.pull

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Org design tokens from JakobMelchard/web tokens/tokens.json (v0.1.0, 34704ce):
// Dracula on black, dark is the brand, system monospace, line borders instead of shadows.
data class Palette(
  val bg: Color,
  val card: Color,
  val line: Color,
  val fg: Color,
  val muted: Color,
  val accent: Color,
  val pink: Color,
  val purple: Color,
  val green: Color,
  val orange: Color,
  val red: Color,
)

val Dark = Palette(
  bg = Color(0xFF0B0D10),
  card = Color(0xFF15171F),
  line = Color(0x406272A4),
  fg = Color(0xFFF8F8F2),
  muted = Color(0xFF6272A4),
  accent = Color(0xFF8BE9FD),
  pink = Color(0xFFFF79C6),
  purple = Color(0xFFBD93F9),
  green = Color(0xFF50FA7B),
  orange = Color(0xFFFFB86C),
  red = Color(0xFFFF5555),
)

val Light = Palette(
  bg = Color(0xFFFFFFFF),
  card = Color(0xFFF2F2F5),
  line = Color(0x1A000000),
  fg = Color(0xFF1A1A2E),
  muted = Color(0xFF6B7280),
  accent = Color(0xFF2563EB),
  pink = Color(0xFFE11D48),
  purple = Color(0xFF7C3AED),
  green = Color(0xFF059669),
  orange = Color(0xFFD97706),
  red = Color(0xFFDC2626),
)

val LocalPalette = staticCompositionLocalOf { Dark }

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal, line: Float = 1.4f) =
  TextStyle(fontFamily = FontFamily.Monospace, fontSize = size.sp, lineHeight = (size * line).sp, fontWeight = weight)

@Composable
fun PullTheme(content: @Composable () -> Unit) {
  val p = if (isSystemInDarkTheme()) Dark else Light
  val base = if (p == Dark) darkColorScheme() else lightColorScheme()
  val colors = base.copy(
    primary = p.accent,
    onPrimary = p.bg,
    primaryContainer = p.card,
    onPrimaryContainer = p.accent,
    secondary = p.purple,
    onSecondary = p.bg,
    secondaryContainer = p.card,
    onSecondaryContainer = p.accent,
    tertiary = p.pink,
    error = p.red,
    background = p.bg,
    onBackground = p.fg,
    surface = p.bg,
    onSurface = p.fg,
    surfaceVariant = p.card,
    onSurfaceVariant = p.muted,
    surfaceContainer = p.card,
    surfaceContainerLow = p.card,
    surfaceContainerHigh = p.card,
    outline = p.line,
    outlineVariant = p.line,
  )
  // The org's mono scale is sized for the web; read at arm's length mid-pull it needs about 1.3x.
  val type = Typography(
    displayLarge = mono(80, FontWeight.Bold, 1.1f),
    headlineMedium = mono(30, FontWeight.Bold),
    titleMedium = mono(20, FontWeight.Bold),
    titleSmall = mono(18, FontWeight.Bold),
    bodyLarge = mono(20),
    bodyMedium = mono(18, line = 1.5f),
    bodySmall = mono(16, line = 1.5f),
    labelLarge = mono(18, FontWeight.Bold),
    labelMedium = mono(15),
    labelSmall = mono(14),
  )
  val shapes = Shapes(small = RoundedCornerShape(3.dp), medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(10.dp))
  CompositionLocalProvider(LocalPalette provides p) {
    MaterialTheme(colorScheme = colors, typography = type, shapes = shapes, content = content)
  }
}

// The org's raised surface: card fill, line border, radius-md, never a shadow.
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  val p = LocalPalette.current
  val shape = RoundedCornerShape(8.dp)
  Column(
    modifier.fillMaxWidth().background(p.card, shape).border(1.dp, p.line, shape).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    content = content,
  )
}
