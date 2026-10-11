// Hand port of the dark tokens in JakobMelchard/web dist/tokens.json (web has no Kotlin output).

package io.lilfeelz.fz

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Dark-theme colour tokens and radii, one per token in tokens.json. */
object Tokens {
  val bg = Color(0xFF0B0D10)
  val card = Color(0xFF15171F)
  val glass = Color(0xFFFFFFFF).copy(alpha = 0.06f)
  val line = Color(0xFF6272A4).copy(alpha = 0.25f)
  val grid = Color(0xFFEEF4FF).copy(alpha = 0.035f)
  val fg = Color(0xFFF8F8F2)
  val muted = Color(0xFF6272A4)
  val accent = Color(0xFF8BE9FD)
  val pink = Color(0xFFFF79C6)
  val purple = Color(0xFFBD93F9)
  val green = Color(0xFF50FA7B)
  val orange = Color(0xFFFFB86C)
  val red = Color(0xFFFF5555)
  val hover = Color(0xFF8BE9FD).copy(alpha = 0.07f)
  val ghost = Color(0xFFFFFFFF).copy(alpha = 0.18f)
  val scrim = Color(0xFF000000).copy(alpha = 0.7f)
  val scrimBlur = Color(0xFF0B0D10).copy(alpha = 0.8f)
  val link = accent
  val caret = red
  val success = green
  val warn = orange
  val danger = red
  val info = purple
  val highlight = pink

  val radiusXs = 2.dp
  val radiusSm = 3.dp
  val radiusMd = 8.dp
  val radiusLg = 10.dp
  val radiusXl = 14.dp
  val radius2xl = 28.dp
  val radiusPill = 999.dp
}
