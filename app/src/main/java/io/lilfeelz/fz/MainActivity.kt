package io.lilfeelz.fz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ln

data class App(val label: String, val pkg: String, val cls: String) {
  val key get() = "$pkg/$cls"
}

// subsequence match: consecutive chars and word starts score higher
fun fuzzy(q: String, s: String): Double? {
  val t = s.lowercase()
  var score = 0.0
  var j = 0
  var prev = -2
  for (c in q.lowercase()) {
    j = t.indexOf(c, j).takeIf { it >= 0 } ?: return null
    score += when {
      j == prev + 1 -> 3.0
      j == 0 || t[j - 1] == ' ' -> 2.0
      else -> 1.0
    }
    prev = j++
  }
  return score - t.length * 0.01
}

private val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = Tokens.fg)

class MainActivity : ComponentActivity() {
  private val prefs by lazy { getSharedPreferences("usage", MODE_PRIVATE) }
  private var apps by mutableStateOf(emptyList<App>())
  private var query by mutableStateOf("")
  private var resumes by mutableIntStateOf(0)

  override fun onCreate(s: Bundle?) {
    super.onCreate(s)
    val bars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    enableEdgeToEdge(bars, bars)
    onBackPressedDispatcher.addCallback(this) { query = "" }
    setContent { Launcher() }
  }

  override fun onResume() {
    super.onResume()
    query = ""
    resumes++
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    apps = packageManager.queryIntentActivities(i, 0)
      .map { App(it.loadLabel(packageManager).toString(), it.activityInfo.packageName, it.activityInfo.name) }
      .filter { it.pkg != packageName }
  }

  // launches / (1 + days since last launch)
  private fun frecency(a: App): Double {
    val (n, t) = prefs.getString(a.key, "0|0")!!.split("|").map { it.toLong() }
    return n / (1 + (System.currentTimeMillis() - t) / 86_400_000.0)
  }

  private fun launch(a: App) {
    val n = prefs.getString(a.key, "0|0")!!.substringBefore("|").toLong()
    prefs.edit().putString(a.key, "${n + 1}|${System.currentTimeMillis()}").apply()
    startActivity(Intent().setClassName(a.pkg, a.cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  private fun matches(): List<App> =
    if (query.isBlank()) apps.sortedByDescending(::frecency)
    else apps.mapNotNull { a -> fuzzy(query, a.label)?.let { a to it + ln(1 + frecency(a)) } }
      .sortedByDescending { it.second }.map { it.first }

  @Composable
  fun Launcher() {
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    val all = matches()
    val list = all.take(8)
    val shape = RoundedCornerShape(Tokens.radiusMd)
    Column(
      Modifier.fillMaxSize().background(Tokens.bg).systemBarsPadding().imePadding().padding(16.dp),
      verticalArrangement = Arrangement.Bottom,
    ) {
      list.asReversed().forEach { a ->
        val top = a == list.first()
        Row(
          Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .background(if (top) Tokens.hover else Color.Transparent, shape)
            .clickable { launch(a) }.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          BasicText(a.label, style = mono.copy(color = if (top) Tokens.accent else Tokens.fg))
        }
      }
      BasicText(
        "${all.size}/${apps.size}",
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = mono.copy(color = Tokens.muted, fontSize = 13.sp),
      )
      Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
          .background(Tokens.card, shape).border(1.dp, Tokens.line, shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BasicText("> ", style = mono.copy(color = Tokens.accent))
        BasicTextField(
          query, { query = it }, singleLine = true,
          textStyle = mono,
          cursorBrush = SolidColor(Tokens.caret),
          keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Go),
          keyboardActions = KeyboardActions(onGo = { list.firstOrNull()?.let(::launch) }),
          modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
      }
    }
    LaunchedEffect(resumes) { focus.requestFocus(); kb?.show() }
  }
}
