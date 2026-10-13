package io.jakobmelchard.pull

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
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

fun pct(kg: Float, body: Float) = if (body > 0f) "%.0f%%".format(kg / body * 100) else "-"

@Composable
fun Big(text: String, color: Color = LocalPalette.current.pink) =
  Text(text, style = MaterialTheme.typography.displayLarge, color = color)

@Composable
fun GripPicker(store: Store) {
  var open by remember { mutableStateOf(false) }
  val p = LocalPalette.current
  Box {
    Row(Modifier.clickable { open = true }.padding(vertical = 8.dp)) {
      Text("grip ", color = p.muted)
      Text("${store.gripName(store.grip)} v", color = p.purple, style = MaterialTheme.typography.titleMedium)
    }
    DropdownMenu(open, { open = false }) {
      store.grips.forEach { g ->
        DropdownMenuItem(
          text = { Text(g.name, color = if (g.id == store.grip) p.accent else p.fg) },
          onClick = {
            store.grip = g.id
            open = false
          },
        )
      }
    }
  }
}

@Composable
fun Live(scale: Scale, store: Store) {
  val now = rememberNow()
  var start by remember { mutableStateOf<Long?>(null) }
  val from = start ?: (now - 10_000)
  val pts = scale.trace.filter { it.t >= from }
  val peak = pts.maxOfOrNull { it.kg } ?: 0f
  Big(kg(scale.trace.lastOrNull()?.kg ?: 0f))
  Text("peak ${kg(peak)}   avg ${kg(avgLoaded(pts))}", color = LocalPalette.current.muted)
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
  val p = LocalPalette.current
  var hand by rememberSaveable { mutableStateOf("L") }
  var dur by rememberSaveable { mutableFloatStateOf(7f) }
  var t0 by remember { mutableStateOf<Long?>(null) }
  // The finished pull and the grip's best before it.
  var result by remember { mutableStateOf<Pair<Session, Float?>?>(null) }
  val s = t0
  if (s == null) {
    GripPicker(store)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("L", "R", "Both").forEach { FilterChip(hand == it, { hand = it }, { Text(it) }) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      NumField("Bodyweight kg", store.body, Modifier.weight(1f)) { store.body = it }
      NumField("Pull s", dur, Modifier.weight(1f)) { dur = it }
    }
    Panel {
      Text("high score $hand", color = p.muted, style = MaterialTheme.typography.labelMedium)
      val best = store.best(store.grip, hand)
      Text(best?.let { "${kg(it)}  ${pct(it, store.body)} bw" } ?: "not tested yet", style = MaterialTheme.typography.titleMedium)
    }
    Button({
      result = null
      t0 = now + 3_000
    }) { Text("Start") }
    result?.let { (r, prev) ->
      Panel {
        if (r.peak <= 0f) {
          Text("No data, nothing saved", color = p.orange, style = MaterialTheme.typography.titleMedium)
          return@Panel
        }
        if (prev == null) {
          Text("First test", color = p.green, style = MaterialTheme.typography.titleMedium)
        } else if (r.peak > prev) {
          Text("New high score", color = p.green, style = MaterialTheme.typography.titleMedium)
        }
        Big(kg(r.peak))
        Text("${pct(r.peak, r.body)} bodyweight", color = p.muted)
        prev?.let { Text("previous best ${r.hand}: ${kg(it)}", color = p.muted) }
      }
    }
    return
  }
  val end = s + (dur * 1000).toLong()
  val pts = scale.trace.filter { it.t >= s - 3_000 }
  Text(store.gripName(store.grip), color = p.purple, style = MaterialTheme.typography.titleMedium)
  Text(
    if (now < s) "get ready ${secs(s - now)}" else "pull ${secs(end - now)}",
    style = MaterialTheme.typography.headlineMedium,
    color = if (now < s) p.orange else p.accent,
  )
  Big(kg(pts.filter { it.t >= s }.maxOfOrNull { it.kg } ?: 0f))
  Graph(pts, s - 3_000, end)
  OutlinedButton({ t0 = null }) { Text("Cancel") }
  LaunchedEffect(s) {
    delay(s - SystemClock.elapsedRealtime())
    beep()
    delay(end - SystemClock.elapsedRealtime())
    beep(400)
    val xs = scale.trace.filter { it.t in s..end }
    val prev = store.best(store.grip, hand)
    val r = Session(
      System.currentTimeMillis(), "max", hand, xs.maxOfOrNull { it.kg } ?: 0f, avgLoaded(xs), store.body, secs(end - s), store.grip,
    )
    if (xs.isNotEmpty()) store.add(r)
    result = r to prev
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
  val pal = LocalPalette.current
  var t0 by remember { mutableStateOf<Long?>(null) }
  var done by remember { mutableStateOf<Session?>(null) }
  val p = store.plan
  val s = t0
  if (s == null) {
    GripPicker(store)
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
      Panel {
        Text("avg ${kg(it.avg)}   peak ${kg(it.peak)}", style = MaterialTheme.typography.titleMedium)
        Text(it.note, color = pal.muted)
      }
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
        "$reps/${work.size} reps ${p.work}/${p.rest} s, $inZone% in ${kg(p.lo)} to ${kg(p.hi)}", store.grip,
      ).also(store::add)
    }
    t0 = null
  }
  val pulling = ph[i].name == "Pull"
  Text(store.gripName(store.grip), color = pal.purple, style = MaterialTheme.typography.titleMedium)
  Text(ph[i].name.lowercase(), style = MaterialTheme.typography.headlineMedium, color = if (pulling) pal.accent else pal.orange)
  Text("${secs(starts[i + 1] - now)}   set ${ph[i].set}/${p.sets}   rep ${ph[i].rep}/${p.reps}", color = pal.muted)
  Big(kg(force), if (!pulling) pal.fg else if (force in zone) pal.green else pal.red)
  Text("$inZone% in target", color = pal.muted)
  Graph(scale.trace.filter { it.t >= now - 15_000 }, now - 15_000, now, zone)
  OutlinedButton(finish) { Text("Stop and save") }
  LaunchedEffect(i) { if (i > 0) beep(if (pulling) 400 else 150) }
  // The timer outlives this composition's xs, so it must call the latest finish.
  val fin by rememberUpdatedState(finish)
  LaunchedEffect(s) {
    delay(starts.last() - SystemClock.elapsedRealtime())
    beep(800)
    fin()
  }
}

@Composable
fun Grips(store: Store) {
  val p = LocalPalette.current
  val fmt = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
  var name by remember { mutableStateOf("") }
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("New grip") }, singleLine = true)
    TextButton(
      {
        store.addGrip(name.trim())
        name = ""
      },
      enabled = name.isNotBlank(),
    ) { Text("Add", color = if (name.isNotBlank()) p.green else p.muted) }
  }
  // Delete takes two taps: the first arms it for that grip only.
  var arm by remember { mutableStateOf<Long?>(null) }
  store.grips.forEach { g ->
    val on = g.id == store.grip
    val xs = store.sessions.filter { it.grip == g.id }
    Panel(Modifier.clickable { store.grip = g.id }) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (on) "> " else "  ", color = p.accent, style = MaterialTheme.typography.titleMedium)
        Text(g.name, Modifier.weight(1f), color = p.purple, style = MaterialTheme.typography.titleMedium)
        if (arm == g.id) {
          TextButton({
            store.removeGrip(g)
            arm = null
          }) { Text("Delete?", color = p.red) }
        } else {
          IconButton({ arm = g.id }) { Icon(Icons.Default.Delete, "Delete ${g.name}", tint = p.muted) }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("L", "R", "Both").forEach { h ->
          Column {
            Text(h, color = p.muted, style = MaterialTheme.typography.labelMedium)
            val b = store.best(g.id, h)
            Text(b?.let { kg(it) } ?: "-", color = if (b != null) p.pink else p.muted, style = MaterialTheme.typography.titleSmall)
            b?.let { Text(pct(it, store.body), color = p.muted, style = MaterialTheme.typography.labelMedium) }
          }
        }
      }
      xs.filter { it.type == "repeaters" }.maxOfOrNull { it.avg }?.let {
        Text("best repeaters avg ${kg(it)}", color = p.muted, style = MaterialTheme.typography.bodySmall)
      }
      xs.maxOfOrNull { it.at }?.let {
        Text("last ${fmt.format(Date(it))}, ${xs.size} session${if (xs.size == 1) "" else "s"}", color = p.muted, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
fun History(store: Store) {
  val p = LocalPalette.current
  val max = store.sessions.filter { it.type == "max" && it.grip == store.grip }.reversed()
  if (max.size >= 2) {
    Row {
      Text("max pulls ", color = p.muted)
      Text(store.gripName(store.grip), color = p.purple, style = MaterialTheme.typography.titleMedium)
    }
    Trend(max)
  }
  val fmt = remember { SimpleDateFormat("d MMM HH:mm", Locale.getDefault()) }
  LazyColumn {
    items(store.sessions, key = { it.at }) { s ->
      Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text("${fmt.format(Date(s.at))}  ${s.type} ${s.hand}", color = p.muted, style = MaterialTheme.typography.labelMedium)
          if (s.grip != 0L) Text(store.gripName(s.grip), color = p.purple, style = MaterialTheme.typography.titleSmall)
          Text("peak ${kg(s.peak)} (${pct(s.peak, s.body)} bw)   avg ${kg(s.avg)}")
          Text(s.note, color = p.muted, style = MaterialTheme.typography.bodySmall)
        }
        IconButton({ store.remove(s) }) { Icon(Icons.Default.Delete, "Delete", tint = p.muted) }
      }
      HorizontalDivider(color = p.line)
    }
  }
}

@Composable
fun Trend(xs: List<Session>) {
  val pal = LocalPalette.current
  val colors = mapOf("L" to pal.accent, "R" to pal.pink, "Both" to pal.purple)
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    colors.filterKeys { h -> xs.any { it.hand == h } }.forEach { (h, c) -> Text(h, color = c) }
  }
  Canvas(Modifier.fillMaxWidth().height(140.dp)) {
    val top = maxOf(xs.maxOf { it.peak } * 1.1f, 1f)
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
