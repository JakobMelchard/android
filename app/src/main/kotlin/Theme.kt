package io.jakobmelchard.pull

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import jakobmelchard.tokens.DarkTokens
import jakobmelchard.tokens.LightTokens
import jakobmelchard.tokens.RadiusTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
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

// Org design tokens (tokens/Tokens.kt, vendored from JakobMelchard/web):
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

val Dark = DarkTokens.run { Palette(bg, card, line, fg, muted, accent, pink, purple, green, orange, red) }

val Light = LightTokens.run { Palette(bg, card, line, fg, muted, accent, pink, purple, green, orange, red) }

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
  val shapes = Shapes(small = RoundedCornerShape(RadiusTokens.radiusSm.dp), medium = RoundedCornerShape(RadiusTokens.radiusMd.dp), large = RoundedCornerShape(RadiusTokens.radiusLg.dp))
  CompositionLocalProvider(LocalPalette provides p) {
    MaterialTheme(colorScheme = colors, typography = type, shapes = shapes, content = content)
  }
}

// The org's raised surface: card fill, line border, radius-md, never a shadow.
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  val p = LocalPalette.current
  val shape = RoundedCornerShape(RadiusTokens.radiusMd.dp)
  Column(
    modifier.fillMaxWidth().background(p.card, shape).border(1.dp, p.line, shape).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    content = content,
  )
}

// Screen frame: scrolling content, and the one primary action pinned in the thumb zone.
@Composable
fun Screen(action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
  Column(Modifier.fillMaxSize()) {
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      content = content,
    )
    action?.let { Box(Modifier.padding(16.dp)) { it() } }
  }
}

@Composable
fun Action(text: String, color: Color = LocalPalette.current.accent, onClick: () -> Unit) =
  Button(
    onClick,
    Modifier.fillMaxWidth().height(60.dp),
    shape = RoundedCornerShape(RadiusTokens.radiusLg.dp),
    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = LocalPalette.current.bg),
  ) { Text(text, style = MaterialTheme.typography.titleMedium) }

@Composable
fun RowScope.Stat(label: String, value: String, color: Color = LocalPalette.current.fg) =
  Column(Modifier.weight(1f)) {
    Text(label, color = LocalPalette.current.muted, style = MaterialTheme.typography.labelMedium)
    Text(value, color = color, style = MaterialTheme.typography.titleMedium)
  }

@Composable
fun Seg(options: List<String>, selected: String, onSelect: (String) -> Unit) {
  val p = LocalPalette.current
  Row(
    Modifier.fillMaxWidth().border(1.dp, p.line, RoundedCornerShape(RadiusTokens.radiusLg.dp)).padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    options.forEach { o ->
      val on = o == selected
      Box(
        Modifier.weight(1f).height(48.dp).background(if (on) p.accent else Color.Transparent, RoundedCornerShape(RadiusTokens.radiusMd.dp))
          .selectable(on, role = Role.RadioButton) { onSelect(o) },
        contentAlignment = Alignment.Center,
      ) { Text(o, color = if (on) p.bg else p.muted, style = MaterialTheme.typography.labelLarge) }
    }
  }
}

@Composable
fun Pill(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) =
  Box(
    Modifier.heightIn(min = 48.dp).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(RadiusTokens.radiusPill.dp))
      .clickable(enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) { Text(text, color = color, style = MaterialTheme.typography.labelLarge) }
