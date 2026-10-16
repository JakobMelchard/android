package io.lilfeelz.fz

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Hand copy of DarkTokens and RadiusTokens from JakobMelchard/web dist/tokens.kt; the build does not pull it.
object Tokens {
    val bg = Color(0xFF0B0D10)
    val card = Color(0xFF15171F)
    val line = Color(0xFF6272A4).copy(alpha = 0.25f)
    val fg = Color(0xFFF8F8F2)
    val muted = Color(0xFF6272A4)
    val accent = Color(0xFF8BE9FD)
    val hover = Color(0xFF8BE9FD).copy(alpha = 0.07f)
    val caret = Color(0xFFFF5555)

    val radiusMd = 8.dp
}
