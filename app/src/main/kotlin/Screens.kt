package io.jakobmelchard.pull

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import jakobmelchard.tokens.RadiusTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tone by lazy { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }

fun beep(ms: Int = 150) = tone.startTone(ToneGenerator.TONE_PROP_BEEP, ms)

fun secs(ms: Long) = "${(ms.coerceAtLeast(0) + 999) / 1000} s"

fun pct(kg: Float, body: Float) = if (body > 0f) "%.0f%%".format(kg / body * 100) else "-"

@Composable
// Shrinks to stay on one line: 80sp only fits about six mono characters on a 360dp phone.
fun Big(text: String, color: Color = LocalPalette.current.pink) =
  BasicText(
    text,
    style = MaterialTheme.typography.displayLarge.copy(color = color),
    maxLines = 1,
    softWrap = false,
    autoSize = TextAutoSize.StepBased(minFontSize = 32.sp, maxFontSize = 80.sp),
  )

@Composable
fun Label(text: String) = Text(text, color = LocalPalette.current.muted, style = MaterialTheme.typography.labelMedium)

// The grip pill that heads Max and Repeaters; opens the grip menu.
@Composable
fun GripPicker(store: Store) {
  var open by remember { mutableStateOf(false) }
  val p = LocalPalette.current
  Box {
    Row(
      Modifier.border(1.dp, p.purple.copy(alpha = 0.5f), RoundedCornerShape(RadiusTokens.radiusPill.dp)).clickable { open = true }
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(store.gripName(store.grip), color = p.purple, style = MaterialTheme.typography.titleSmall)
      Text("  v", color = p.muted, style = MaterialTheme.typography.labelMedium)
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
  val p = LocalPalette.current
  var start by remember { mutableStateOf<Long?>(null) }
  val s = start
  val from = s ?: (now - 10_000)
  val pts = scale.trace.filter { it.t >= from }
  val peak = pts.maxOfOrNull { it.kg } ?: 0f
  Screen({
    Action(if (s == null) "Record" else "Stop and save", if (s == null) p.accent else p.orange) {
      if (s == null) {
        start = now
      } else {
        if (pts.isNotEmpty()) store.add(Session(System.currentTimeMillis(), "live", "", peak, avgLoaded(pts), store.body, secs(now - s)))
        start = null
      }
    }
  }) {
    Panel {
      Label(if (s == null) "force" else "recording")
      Big(kg(scale.trace.lastOrNull()?.kg ?: 0f))
      Row {
        Stat("peak", kg(peak))
        Stat("avg", kg(avgLoaded(pts)))
        Stat(if (s == null) "window" else "time", secs(now - from))
      }
    }
    Panel { Graph(pts, from, maxOf(now, from + 10_000)) }
  }
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
    Screen({
      Action("Start") {
        result = null
        t0 = now + 3_000
      }
    }) {
      GripPicker(store)
      Seg(listOf("L", "R", "Both"), hand) { hand = it }
      result?.let { (r, prev) ->
        Panel {
          when {
            r.peak <= 0f -> Text("No data, nothing saved", color = p.orange, style = MaterialTheme.typography.titleMedium)
            prev == null -> Label("first test")
            r.peak > prev -> Text("New high score", color = p.green, style = MaterialTheme.typography.titleMedium)
            else -> Label("last pull")
          }
          if (r.peak > 0f) {
            Big(kg(r.peak), if (prev != null && r.peak > prev) p.green else p.pink)
            Row {
              Stat("bodyweight", pct(r.peak, r.body))
              Stat("previous best", prev?.let { kg(it) } ?: "-")
            }
          }
        }
      }
      val best = store.best(store.grip, hand)
      Panel {
        Label("high score $hand")
        Big(best?.let { kg(it) } ?: "-", if (best != null) p.pink else p.muted)
        Row {
          Stat("bodyweight", best?.let { pct(it, store.body) } ?: "-")
          Stat("tests", "${store.sessions.count { it.type == "max" && it.grip == store.grip && it.hand == hand }}")
        }
      }
      Panel {
        Label("setup")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NumField("Bodyweight kg", store.body, Modifier.weight(1f)) { store.body = it }
          NumField("Pull s", dur, Modifier.weight(1f)) { dur = it.coerceAtLeast(1f) }
        }
      }
    }
    return
  }
  val end = s + (dur * 1000).toLong()
  val pts = scale.trace.filter { it.t >= s - 3_000 }
  val ready = now < s
  Screen({ Action("Cancel", p.red) { t0 = null } }) {
    Text("${store.gripName(store.grip)}  $hand", color = p.purple, style = MaterialTheme.typography.titleSmall)
    Panel {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          if (ready) "get ready" else "pull",
          Modifier.weight(1f),
          color = if (ready) p.orange else p.accent,
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(secs(if (ready) s - now else end - now), style = MaterialTheme.typography.headlineMedium)
      }
      LinearProgressIndicator(
        { if (ready) 0f else ((now - s).toFloat() / (end - s)).coerceIn(0f, 1f) },
        Modifier.fillMaxWidth(),
        color = p.accent,
        trackColor = p.line,
        drawStopIndicator = {},
      )
      Label("peak")
      Big(kg(pts.filter { it.t >= s }.maxOfOrNull { it.kg } ?: 0f))
    }
    Panel { Graph(pts, s - 3_000, end) }
  }
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

data class Tally(val xs: List<Reading>, val inZone: Int, val reps: Int)

// Samples taken in the pull windows, the percentage inside the target band, and the pulls that
// have ended by now. starts holds each phase's start time plus the end of the last one.
fun tally(ph: List<Phase>, starts: List<Long>, trace: List<Reading>, now: Long, zone: ClosedFloatingPointRange<Float>): Tally {
  val work = ph.indices.filter { ph[it].name == "Pull" }.map { starts[it] until starts[it + 1] }
  val xs = trace.filter { r -> r.t >= starts[0] && work.any { r.t in it } }
  val inZone = if (xs.isEmpty()) 0 else xs.count { it.kg in zone } * 100 / xs.size
  return Tally(xs, inZone, work.count { it.last < now })
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
    Screen({
      Action("Start") {
        done = null
        t0 = now
      }
    }) {
      GripPicker(store)
      done?.let {
        Panel {
          Label("last session")
          Row {
            Stat("avg", kg(it.avg), pal.pink)
            Stat("peak", kg(it.peak))
          }
          Label(it.note)
        }
      }
      Panel {
        Label("plan")
        Text("${p.reps} x ${p.work}/${p.rest} s", style = MaterialTheme.typography.headlineMedium)
        Row {
          Stat("sets", "${p.sets}")
          Stat("set rest", "${p.setRest} s")
          Stat("total", secs(schedule(p).sumOf { it.ms }))
        }
        Text("target ${kg(p.lo)} to ${kg(p.hi)}", color = pal.green, style = MaterialTheme.typography.titleSmall)
      }
      Panel {
        Label("edit")
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
      }
    }
    return
  }
  val ph = remember(p) { schedule(p) }
  val starts = ph.runningFold(s) { a, x -> a + x.ms }
  val i = starts.indexOfLast { it <= now }.coerceIn(0, ph.size - 1)
  val zone = p.lo..p.hi
  val force = scale.trace.lastOrNull()?.kg ?: 0f
  val inZone = tally(ph, starts, scale.trace, now, zone).inZone
  val finish = {
    val t = tally(ph, starts, scale.trace, SystemClock.elapsedRealtime(), zone)
    if (t.xs.isNotEmpty()) {
      done = Session(
        System.currentTimeMillis(), "repeaters", "Both", t.xs.maxOf { it.kg }, avgLoaded(t.xs), store.body,
        "${t.reps}/${ph.count { it.name == "Pull" }} reps ${p.work}/${p.rest} s, ${t.inZone}% in ${kg(p.lo)} to ${kg(p.hi)}", store.grip,
      ).also(store::add)
    }
    t0 = null
  }
  val pulling = ph[i].name == "Pull"
  Screen({ Action("Stop and save", pal.orange, finish) }) {
    Text(store.gripName(store.grip), color = pal.purple, style = MaterialTheme.typography.titleSmall)
    Panel {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          ph[i].name.lowercase(),
          Modifier.weight(1f),
          color = if (pulling) pal.accent else pal.orange,
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(secs(starts[i + 1] - now), style = MaterialTheme.typography.headlineMedium)
      }
      LinearProgressIndicator(
        { ((now - s).toFloat() / (starts.last() - s)).coerceIn(0f, 1f) },
        Modifier.fillMaxWidth(),
        color = pal.accent,
        trackColor = pal.line,
        drawStopIndicator = {},
      )
      Row {
        Stat("set", "${ph[i].set}/${p.sets}")
        Stat("rep", "${ph[i].rep}/${p.reps}")
        Stat("in target", "$inZone%", if (inZone >= 70) pal.green else pal.fg)
      }
      ph.getOrNull(i + 1)?.let { Label("next: ${it.name.lowercase()} ${secs(it.ms)}") }
    }
    Panel {
      Big(kg(force), if (!pulling) pal.fg else if (force in zone) pal.green else pal.red)
      Graph(scale.trace.filter { it.t >= now - 15_000 }, now - 15_000, now, zone)
    }
  }
  LaunchedEffect(i) { if (i > 0) beep(if (pulling) 400 else 150) }
  // The timer outlives this composition, so it must call the latest finish.
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
  // Delete takes two taps: the first arms it for that grip only.
  var arm by remember { mutableStateOf<Long?>(null) }
  Screen {
    store.grips.forEach { g ->
      val xs = store.sessions.filter { it.grip == g.id }
      Panel(Modifier.clickable { store.grip = g.id }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(g.name, Modifier.weight(1f), color = p.purple, style = MaterialTheme.typography.titleMedium)
          if (g.id == store.grip) Label("current")
          if (arm == g.id) {
            TextButton({
              store.removeGrip(g)
              arm = null
            }) { Text("Delete?", color = p.red) }
          } else {
            IconButton({ arm = g.id }) { Icon(Icons.Default.Delete, "Delete ${g.name}", tint = p.muted) }
          }
        }
        Row {
          listOf("L", "R", "Both").forEach { h ->
            val b = store.best(g.id, h)
            Stat(h, b?.let { kg(it) } ?: "-", if (b != null) p.pink else p.muted)
          }
        }
        val rep = xs.filter { it.type == "repeaters" }.maxOfOrNull { it.avg }
        val last = xs.maxOfOrNull { it.at }
        if (last != null) {
          Label(
            listOfNotNull(
              rep?.let { "repeaters avg ${kg(it)}" },
              "last ${fmt.format(Date(last))}",
              "${xs.size} session${if (xs.size == 1) "" else "s"}",
            ).joinToString("  "),
          )
        }
      }
    }
    Panel {
      Label("new grip")
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.weight(1f), placeholder = { Text("25 mm edge") }, singleLine = true)
        Pill("Add", if (name.isNotBlank()) p.green else p.muted, enabled = name.isNotBlank()) {
          store.addGrip(name.trim())
          name = ""
        }
      }
    }
  }
}

@Composable
fun History(store: Store) {
  val p = LocalPalette.current
  val max = store.sessions.filter { it.type == "max" && it.grip == store.grip }.reversed()
  val fmt = remember { SimpleDateFormat("d MMM HH:mm", Locale.getDefault()) }
  // LazyColumn scrolls itself, so this screen does not use Screen.
  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (max.size >= 2) {
      item {
        Panel {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Label("max pulls  ")
            Text(store.gripName(store.grip), color = p.purple, style = MaterialTheme.typography.titleSmall)
          }
          Trend(max)
        }
      }
    }
    items(store.sessions, key = { it.at }) { s ->
      Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Label("${fmt.format(Date(s.at))}  ${s.type} ${s.hand}")
            if (s.grip != 0L) Text(store.gripName(s.grip), color = p.purple, style = MaterialTheme.typography.titleSmall)
          }
          IconButton({ store.remove(s) }) { Icon(Icons.Default.Delete, "Delete", tint = p.muted) }
        }
        Row {
          Stat("peak", kg(s.peak), p.pink)
          Stat("avg", kg(s.avg))
          Stat("bw", pct(s.peak, s.body))
        }
        if (s.note.isNotEmpty()) Label(s.note)
      }
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
