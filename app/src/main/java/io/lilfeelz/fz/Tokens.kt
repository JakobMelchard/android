package io.lilfeelz.fz

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Hand port of the dark tokens in JakobMelchard/web dist/tokens.json (web has no Kotlin output).
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
