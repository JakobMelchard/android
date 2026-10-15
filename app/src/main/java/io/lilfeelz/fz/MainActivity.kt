package io.lilfeelz.fz

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlin.math.ln
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

// stored as "count|epochMs"; anything malformed reads as never launched
fun usage(stored: String?): Pair<Long, Long> {
  val p = stored?.split("|")
  val n = p?.getOrNull(0)?.toLongOrNull()
  val t = p?.getOrNull(1)?.toLongOrNull()
  return if (n == null || t == null) 0L to 0L else n to t
}

fun stamp(n: Long, now: Long) = "$n|$now"

// an empty list is a failed query, not an uninstall of everything
fun staleKeys(stored: Set<String>, apps: List<App>): Set<String> =
  if (apps.isEmpty()) emptySet() else stored - apps.map { it.key }.toSet()

fun frecency(stored: String?, now: Long): Double {
  val (n, t) = usage(stored)
  // a clock set back after a launch makes now - t negative
  return n / (1 + maxOf(0L, now - t) / 86_400_000.0)
}

fun rank(query: String, apps: List<App>, frecencyOf: (App) -> Double): List<App> {
  val f = apps.associateWith(frecencyOf)
  return if (query.isBlank()) apps.sortedByDescending { f.getValue(it) }
  else apps.mapNotNull { a -> fuzzy(query, a.label)?.let { a to it + ln(1 + f.getValue(a)) } }
    .sortedByDescending { it.second }.map { it.first }
}

private val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = Tokens.fg)

class MainActivity : ComponentActivity() {
  private val prefs by lazy { getSharedPreferences("usage", MODE_PRIVATE) }
  private var apps by mutableStateOf(emptyList<App>())
  private var query by mutableStateOf("")
  private var menu by mutableStateOf<App?>(null)
  private var resumes by mutableIntStateOf(0)
  private var load: Job? = null

  // The list is loaded once here and again when a package changes, not on every Home press.
  private val changed = object : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) = loadApps()
  }

  override fun onCreate(s: Bundle?) {
    super.onCreate(s)
    val bars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    enableEdgeToEdge(bars, bars)
    onBackPressedDispatcher.addCallback(this) { if (menu != null) menu = null else query = "" }
    setContent { Launcher() }
    val f = IntentFilter().apply {
      for (a in listOf(Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_CHANGED, Intent.ACTION_PACKAGE_REPLACED)) addAction(a)
      addDataScheme("package")
    }
    ContextCompat.registerReceiver(this, changed, f, ContextCompat.RECEIVER_NOT_EXPORTED)
    loadApps()
  }

  override fun onDestroy() {
    unregisterReceiver(changed)
    super.onDestroy()
  }

  override fun onResume() {
    super.onResume()
    query = ""
    menu = null
    resumes++
  }

  // One loadLabel IPC per activity, so off the main thread. A burst of package broadcasts cancels the
  // load in flight, so an older list never lands after a newer one.
  private fun loadApps() {
    load?.cancel()
    load = lifecycleScope.launch(Dispatchers.IO) {
      val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      val list = packageManager.queryIntentActivities(i, 0)
        .map { App(it.loadLabel(packageManager).toString(), it.activityInfo.packageName, it.activityInfo.name) }
        .filter { it.pkg != packageName }
      ensureActive()
      prefs.edit().apply { staleKeys(prefs.all.keys, list).forEach(::remove) }.apply()
      withContext(Dispatchers.Main) { apps = list }
    }
  }

  private fun launch(a: App) {
    try {
      startActivity(Intent().setClassName(a.pkg, a.cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
      return loadApps()
    } catch (_: SecurityException) {
      return loadApps()
    }
    val (n, _) = usage(prefs.getString(a.key, null))
    prefs.edit().putString(a.key, stamp(n + 1, System.currentTimeMillis())).apply()
  }

  private fun open(i: Intent) {
    menu = null
    try {
      startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
  }

  private fun forget(a: App) {
    prefs.edit().remove(a.key).apply()
    menu = null
  }

  private fun matches(): List<App> {
    val now = System.currentTimeMillis()
    return rank(query, apps) { frecency(prefs.getString(it.key, null), now) }
  }

  @Composable
  fun Launcher() {
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    val all = matches()
    val scroll = rememberLazyListState()
    val shape = RoundedCornerShape(Tokens.radiusMd)
    Column(
      Modifier.fillMaxSize().background(Tokens.bg).systemBarsPadding().imePadding().padding(16.dp),
      verticalArrangement = Arrangement.Bottom,
    ) {
      // reversed so the top hit sits next to the field and the rest scrolls away upward
      LazyColumn(Modifier.weight(1f).fillMaxWidth(), scroll, reverseLayout = true) {
        items(all, key = { it.key }) { a ->
          val top = a == all.first()
          Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp)
              .background(if (top) Tokens.hover else Color.Transparent, shape)
              .combinedClickable(onLongClick = { menu = a }) { launch(a) }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            BasicText(a.label, style = mono.copy(color = if (top) Tokens.accent else Tokens.fg))
          }
        }
      }
      // while the action row is open the count line names the app it acts on
      BasicText(
        menu?.label ?: "${all.size}/${apps.size}",
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = mono.copy(color = Tokens.muted, fontSize = 13.sp),
      )
      menu?.let { a ->
        val pkg = Uri.parse("package:${a.pkg}")
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) {
          for ((name, act) in listOf<Pair<String, () -> Unit>>(
            "info" to { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)) },
            "uninstall" to { open(Intent(Intent.ACTION_DELETE, pkg)) },
            "forget" to { forget(a) },
          )) {
            BasicText(
              name,
              Modifier.heightIn(min = 44.dp).border(1.dp, Tokens.line, shape)
                .clickable(onClick = act).padding(horizontal = 12.dp, vertical = 10.dp),
              style = mono,
            )
          }
        }
      }
      Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
          .background(Tokens.card, shape).border(1.dp, Tokens.line, shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BasicText("> ", style = mono.copy(color = Tokens.accent))
        BasicTextField(
          query, { query = it; menu = null }, singleLine = true,
          textStyle = mono,
          cursorBrush = SolidColor(Tokens.caret),
          // Password type hides Gboard's suggestion strip; no visualTransformation, so text stays visible
          keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Go,
          ),
          keyboardActions = KeyboardActions(onGo = { all.firstOrNull()?.let(::launch) }),
          modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
      }
    }
    LaunchedEffect(resumes) { focus.requestFocus(); kb?.show() }
    LaunchedEffect(query, resumes) { scroll.scrollToItem(0) }
  }
}
