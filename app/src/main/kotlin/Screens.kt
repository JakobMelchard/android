package io.jakobmelchard.pull

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tone by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }

fun beep(ms: Int = 150) = tone.startTone(ToneGenerator.TONE_PROP_BEEP, ms)

fun secs(ms: Long) = "${(ms.coerceAtLeast(0) + 999) / 1000} s"

@Composable
fun Live(scale: Scale, store: Store) {
  val now = rememberNow()
  var start by remember { mutableStateOf<Long?>(null) }
  val from = start ?: (now - 10_000)
  val pts = scale.trace.filter { it.t >= from }
  val peak = pts.maxOfOrNull { it.kg } ?: 0f
  Text(kg(scale.trace.lastOrNull()?.kg ?: 0f), style = MaterialTheme.typography.displayLarge)
  Text("Peak ${kg(peak)}   Avg ${kg(avgLoaded(pts))}")
  Graph(pts, from, maxOf(now, from + 10_000))
  Button({
    val s = start
    if (s == null) {
      start = now
    } else {
      if (pts.isNotEmpty()) store.add(Session(System.currentTimeMillis(), "live", "", peak, avgLoaded(pts), store.body, secs(now - s)))
      start = null
    }
  }) { Text(if (start == null) "Record" else "Stop and save") }
}

@Composable
fun Max(scale: Scale, store: Store) {
  val now = rememberNow()
  var hand by rememberSaveable { mutableStateOf("L") }
  var dur by rememberSaveable { mutableFloatStateOf(7f) }
  var t0 by remember { mutableStateOf<Long?>(null) }
  var result by remember { mutableStateOf<Session?>(null) }
  val s = t0
  if (s == null) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("L", "R", "Both").forEach { FilterChip(hand == it, { hand = it }, { Text(it) }) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      NumField("Bodyweight kg", store.body, Modifier.weight(1f)) { store.body = it }
      NumField("Pull s", dur, Modifier.weight(1f)) { dur = it }
    }
    Button({
      result = null
      t0 = now + 3_000
    }) { Text("Start") }
    result?.let { r ->
      val best = store.sessions.filter { it.type == "max" && it.hand == r.hand && it !== r }.maxOfOrNull { it.peak }
      Text(kg(r.peak), style = MaterialTheme.typography.displayLarge)
      Text("%.0f%% bodyweight".format(r.peak / r.body * 100))
      best?.let { Text("Previous best ${r.hand}: ${kg(it)}") }
    }
    return
  }
  val end = s + (dur * 1000).toLong()
  val pts = scale.trace.filter { it.t >= s - 3_000 }
  Text(if (now < s) "Get ready ${secs(s - now)}" else "Pull ${secs(end - now)}", style = MaterialTheme.typography.headlineMedium)
  Text(kg(pts.filter { it.t >= s }.maxOfOrNull { it.kg } ?: 0f), style = MaterialTheme.typography.displayLarge)
  Graph(pts, s - 3_000, end)
  OutlinedButton({ t0 = null }) { Text("Cancel") }
  LaunchedEffect(s) {
    delay(s - SystemClock.elapsedRealtime())
    beep()
    delay(end - SystemClock.elapsedRealtime())
    beep(400)
    val xs = scale.trace.filter { it.t in s..end }
    val r = Session(System.currentTimeMillis(), "max", hand, xs.maxOfOrNull { it.kg } ?: 0f, avgLoaded(xs), store.body, secs(end - s))
    if (xs.isNotEmpty()) store.add(r)
    result = r
    t0 = null
  }
}

data class Phase(val name: String, val ms: Long, val set: Int, val rep: Int)

fun schedule(p: Plan) = buildList {
  add(Phase("Get ready", 5_000, 1, 1))
  for (s in 1..p.sets) {
    for (r in 1..p.reps) {
      add(Phase("Pull", p.work * 1000L, s, r))
      if (r < p.reps) add(Phase("Rest", p.rest * 1000L, s, r))
    }
    if (s < p.sets) add(Phase("Set rest", p.setRest * 1000L, s, p.reps))
  }
}

@Composable
fun Repeaters(scale: Scale, store: Store) {
  val now = rememberNow()
  var t0 by remember { mutableStateOf<Long?>(null) }
  var done by remember { mutableStateOf<Session?>(null) }
  val p = store.plan
  val s = t0
  if (s == null) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      NumField("Pull s", p.work.toFloat(), Modifier.weight(1f)) { store.plan = store.plan.copy(work = it.toInt().coerceAtLeast(1)) }
      NumField("Rest s", p.rest.toFloat(), Modifier.weight(1f)) { store.plan = store.plan.copy(rest = it.toInt().coerceAtLeast(0)) }
      NumField("Reps", p.reps.toFloat(), Modifier.weight(1f)) { store.plan = store.plan.copy(reps = it.toInt().coerceAtLeast(1)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      NumField("Sets", p.sets.toFloat(), Modifier.weight(1f)) { store.plan = store.plan.copy(sets = it.toInt().coerceAtLeast(1)) }
      NumField("Set rest s", p.setRest.toFloat(), Modifier.weight(1f)) { store.plan = store.plan.copy(setRest = it.toInt().coerceAtLeast(0)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      NumField("Target min kg", p.lo, Modifier.weight(1f)) { store.plan = store.plan.copy(lo = it) }
      NumField("Target max kg", p.hi, Modifier.weight(1f)) { store.plan = store.plan.copy(hi = it) }
    }
    Button({
      done = null
      t0 = now
    }) { Text("Start") }
    done?.let {
      Text("Avg ${kg(it.avg)}   Peak ${kg(it.peak)}", style = MaterialTheme.typography.titleMedium)
      Text(it.note)
    }
    return
  }
  val ph = remember(p) { schedule(p) }
  val starts = ph.runningFold(s) { a, x -> a + x.ms }
  val i = starts.indexOfLast { it <= now }.coerceIn(0, ph.size - 1)
  val zone = p.lo..p.hi
  val force = scale.trace.lastOrNull()?.kg ?: 0f
  val work = ph.indices.filter { ph[it].name == "Pull" }.map { starts[it] until starts[it + 1] }
  val xs = scale.trace.filter { r -> r.t >= s && work.any { r.t in it } }
  val inZone = if (xs.isEmpty()) 0 else xs.count { it.kg in zone } * 100 / xs.size
  val finish = {
    val reps = work.count { it.last < SystemClock.elapsedRealtime() }
    if (xs.isNotEmpty()) {
      done = Session(
        System.currentTimeMillis(), "repeaters", "Both", xs.maxOf { it.kg }, avgLoaded(xs), store.body,
        "$reps/${work.size} reps ${p.work}/${p.rest} s, $inZone% in ${kg(p.lo)} to ${kg(p.hi)}",
      ).also(store::add)
    }
    t0 = null
  }
  Text(ph[i].name, style = MaterialTheme.typography.headlineMedium)
  Text("${secs(starts[i + 1] - now)}   set ${ph[i].set}/${p.sets}   rep ${ph[i].rep}/${p.reps}")
  Text(
    kg(force),
    style = MaterialTheme.typography.displayLarge,
    color = if (ph[i].name != "Pull" || force in zone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
  )
  Text("$inZone% in target")
  Graph(scale.trace.filter { it.t >= now - 15_000 }, now - 15_000, now, zone)
  OutlinedButton(finish) { Text("Stop and save") }
  LaunchedEffect(i) { if (i > 0) beep(if (ph[i].name == "Pull") 400 else 150) }
  // The timer outlives this composition's xs, so it must call the latest finish.
  val fin by rememberUpdatedState(finish)
  LaunchedEffect(s) {
    delay(starts.last() - SystemClock.elapsedRealtime())
    beep(800)
    fin()
  }
}

@Composable
fun History(store: Store) {
  val max = store.sessions.filter { it.type == "max" }.reversed()
  if (max.size >= 2) {
    Text("Max pulls", style = MaterialTheme.typography.titleMedium)
    Trend(max)
  }
  val fmt = remember { SimpleDateFormat("d MMM HH:mm", Locale.getDefault()) }
  LazyColumn {
    items(store.sessions, key = { it.at }) { s ->
      Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text("${fmt.format(Date(s.at))}  ${s.type} ${s.hand}", style = MaterialTheme.typography.titleSmall)
          Text("Peak ${kg(s.peak)} (%.0f%% bw)   Avg ${kg(s.avg)}".format(s.peak / s.body * 100))
          Text(s.note, style = MaterialTheme.typography.bodySmall)
        }
        IconButton({ store.remove(s) }) { Icon(Icons.Default.Delete, "Delete") }
      }
      HorizontalDivider()
    }
  }
}

@Composable
fun Trend(xs: List<Session>) {
  val colors = mapOf(
    "L" to MaterialTheme.colorScheme.primary,
    "R" to MaterialTheme.colorScheme.tertiary,
    "Both" to MaterialTheme.colorScheme.secondary,
  )
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    colors.filterKeys { h -> xs.any { it.hand == h } }.forEach { (h, c) -> Text(h, color = c) }
  }
  Canvas(Modifier.fillMaxWidth().height(140.dp)) {
    val top = xs.maxOf { it.peak } * 1.1f
    fun x(i: Int) = i.toFloat() / (xs.size - 1) * size.width
    fun y(v: Float) = size.height * (1 - v / top)
    colors.forEach { (h, c) ->
      val p = Path()
      var first = true
      xs.forEachIndexed { i, s ->
        if (s.hand != h) return@forEachIndexed
        if (first) p.moveTo(x(i), y(s.peak)) else p.lineTo(x(i), y(s.peak))
        first = false
        drawCircle(c, 4.dp.toPx(), Offset(x(i), y(s.peak)))
      }
      drawPath(p, c, style = Stroke(2.dp.toPx()))
    }
  }
}
