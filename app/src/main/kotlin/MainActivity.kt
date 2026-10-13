package io.jakobmelchard.pull

import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.content.pm.ApplicationInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  private lateinit var scale: Scale
  private var demo = false

  private val perms =
    if (Build.VERSION.SDK_INT >= 31) {
      arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

  private val ask = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    if (it.values.all { ok -> ok }) scale.start() else scale.status = "Bluetooth permission denied"
  }

  private fun granted() = perms.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // Scanning pauses with the screen off, and a session should never lose samples.
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    scale = Scale(applicationContext)
    val store = Store(applicationContext)
    // Debug builds started with --ez demo true draw synthetic pulls instead of scanning (scripts/screenshots.sh).
    demo = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 && intent.getBooleanExtra("demo", false)
    if (demo) lifecycleScope.launch { scale.demo() } else if (!granted()) ask.launch(perms)
    setContent { PullTheme { App(scale, store) { if (granted()) scale.start() else ask.launch(perms) } } }
  }

  override fun onStart() {
    super.onStart()
    if (!demo && granted()) scale.start()
  }

  override fun onStop() {
    super.onStop()
    if (!demo) scale.stop()
  }
}

@Composable
fun App(scale: Scale, store: Store, connect: () -> Unit) {
  var tab by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf(
    "Live" to Icons.Default.PlayArrow,
    "Max" to Icons.Default.Star,
    "Repeat" to Icons.Default.Refresh,
    "Grips" to Icons.Default.ThumbUp,
    "History" to Icons.Default.DateRange,
  )
  val p = LocalPalette.current
  Scaffold(
    containerColor = p.bg,
    topBar = { Header(scale, connect) },
    bottomBar = {
      NavigationBar(containerColor = p.card) {
        tabs.forEachIndexed { i, (label, icon) ->
          NavigationBarItem(
            selected = tab == i,
            onClick = { tab = i },
            icon = { Icon(icon, null) },
            label = { Text(label) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = p.accent,
              selectedTextColor = p.accent,
              indicatorColor = p.accent.copy(alpha = 0.07f),
              unselectedIconColor = p.muted,
              unselectedTextColor = p.muted,
            ),
          )
        }
      }
    },
  ) { pad ->
    // Each screen owns its scrolling and its bottom action.
    // The keyboard pushes content up instead of covering the fields at the bottom.
    Box(Modifier.padding(pad).consumeWindowInsets(pad).imePadding()) {
      when (tab) {
        0 -> Live(scale, store)
        1 -> Max(scale, store)
        2 -> Repeaters(scale, store)
        3 -> Grips(store)
        else -> History(store)
      }
    }
  }
}

@Composable
fun Header(scale: Scale, connect: () -> Unit) {
  val now = rememberNow(500)
  val last = scale.trace.lastOrNull()
  val live = last != null && now - last.t < 3_000
  val p = LocalPalette.current
  Row(
    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("pull", style = MaterialTheme.typography.titleMedium, color = p.pink)
    Box(Modifier.size(10.dp).background(if (live) p.green else if (scale.scanning) p.orange else p.red, CircleShape))
    Text(
      if (live) "scale" else if (scale.scanning) "searching" else scale.status.lowercase(),
      Modifier.weight(1f),
      style = MaterialTheme.typography.labelMedium,
      color = p.muted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (!scale.scanning) Pill("Connect", p.accent, onClick = connect)
    Pill("Tare", if (live) p.accent else p.muted, enabled = live, onClick = scale::zero)
  }
}

@Composable
fun rememberNow(ms: Long = 100): Long {
  var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
  LaunchedEffect(ms) {
    while (true) {
      delay(ms)
      now = SystemClock.elapsedRealtime()
    }
  }
  return now
}

// Average over the loaded part of a pull, so hang-on and let-go do not drag it down.
fun avgLoaded(xs: List<Reading>): Float {
  val l = xs.filter { it.kg > 1f }
  return if (l.isEmpty()) 0f else l.map { it.kg }.average().toFloat()
}

fun kg(x: Float) = "%.1f kg".format(x)

@Composable
fun Graph(pts: List<Reading>, from: Long, to: Long, zone: ClosedFloatingPointRange<Float>? = null) {
  val line = MaterialTheme.colorScheme.primary
  val band = LocalPalette.current.green.copy(alpha = 0.15f)
  val grid = MaterialTheme.colorScheme.outlineVariant
  val label = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val tm = rememberTextMeasurer()
  Canvas(Modifier.fillMaxWidth().height(220.dp)) {
    val top = maxOf(10f, (pts.maxOfOrNull { it.kg } ?: 0f) * 1.15f, (zone?.endInclusive ?: 0f) * 1.15f)
    fun x(t: Long) = (t - from).toFloat() / (to - from) * size.width
    fun y(v: Float) = size.height * (1 - v.coerceIn(0f, top) / top)
    zone?.let { drawRect(band, Offset(0f, y(it.endInclusive)), Size(size.width, y(it.start) - y(it.endInclusive))) }
    for (f in listOf(0f, 0.5f, 1f)) {
      drawLine(grid, Offset(0f, y(top * f)), Offset(size.width, y(top * f)))
      if (f > 0f) drawText(tm, kg(top * f), Offset(4f, y(top * f) + 4f), TextStyle.Default.merge(label))
    }
    val p = Path()
    pts.forEachIndexed { i, r -> if (i == 0) p.moveTo(x(r.t), y(r.kg)) else p.lineTo(x(r.t), y(r.kg)) }
    drawPath(p, line, style = Stroke(3.dp.toPx()))
  }
}

@Composable
fun NumField(label: String, value: Float, modifier: Modifier = Modifier, onChange: (Float) -> Unit) {
  var s by remember { mutableStateOf(if (value % 1f == 0f) value.toInt().toString() else value.toString()) }
  OutlinedTextField(
    s,
    {
      s = it
      it.toFloatOrNull()?.let(onChange)
    },
    modifier,
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}
