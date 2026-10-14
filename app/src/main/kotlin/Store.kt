package io.jakobmelchard.pull

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// type is live, max or repeaters; hand is L, R or Both; grip 0 means none.
data class Session(
  val at: Long,
  val type: String,
  val hand: String,
  val peak: Float,
  val avg: Float,
  val body: Float,
  val note: String,
  val grip: Long = 0,
)

data class Grip(val id: Long, val name: String)

val presets = listOf("20 mm half crimp", "20 mm open hand", "15 mm half crimp", "10 mm half crimp", "Pinch", "Sloper")

data class Plan(
  val work: Int = 7,
  val rest: Int = 3,
  val reps: Int = 6,
  val sets: Int = 3,
  val setRest: Int = 180,
  val lo: Float = 20f,
  val hi: Float = 25f,
)

fun encodePlan(p: Plan) = listOf(p.work, p.rest, p.reps, p.sets, p.setRest, p.lo, p.hi).joinToString(",")

fun decodePlan(s: String?): Plan =
  runCatching {
    val f = s!!.split(",")
    Plan(f[0].toInt(), f[1].toInt(), f[2].toInt(), f[3].toInt(), f[4].toInt(), f[5].toFloat(), f[6].toFloat())
  }.getOrDefault(Plan())

fun encodeSessions(list: List<Session>): String {
  val a = JSONArray()
  list.forEach {
    a.put(
      JSONObject()
        .put("at", it.at).put("type", it.type).put("hand", it.hand)
        .put("peak", it.peak.toDouble()).put("avg", it.avg.toDouble())
        .put("body", it.body.toDouble()).put("note", it.note).put("grip", it.grip),
    )
  }
  return a.toString()
}

fun decodeSessions(s: String): List<Session> {
  val a = JSONArray(s)
  return List(a.length()) {
    val o = a.getJSONObject(it)
    Session(
      o.getLong("at"), o.getString("type"), o.getString("hand"),
      o.getDouble("peak").toFloat(), o.getDouble("avg").toFloat(),
      o.getDouble("body").toFloat(), o.getString("note"), o.optLong("grip", 0),
    )
  }
}

fun encodeGrips(list: List<Grip>): String {
  val a = JSONArray()
  list.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
  return a.toString()
}

fun decodeGrips(s: String): List<Grip> {
  val a = JSONArray(s)
  return List(a.length()) { a.getJSONObject(it).let { o -> Grip(o.getLong("id"), o.getString("name")) } }
}

// Past every id a session still holds, so a new grip never inherits a deleted one's history.
fun nextGripId(grips: List<Grip>, sessions: List<Session>) =
  maxOf(grips.maxOfOrNull { it.id } ?: 0, sessions.maxOfOrNull { it.grip } ?: 0) + 1

class Store(ctx: Context) {
  private val file = File(ctx.filesDir, "sessions.json")
  private val prefs = ctx.getSharedPreferences("pull", Context.MODE_PRIVATE)
  val sessions = mutableStateListOf<Session>()
  val grips = mutableStateListOf<Grip>()

  // Start times of the running sessions, kept here so switching tabs does not lose them.
  var liveStart by mutableStateOf<Long?>(null)
  var maxStart by mutableStateOf<Long?>(null)
  var repStart by mutableStateOf<Long?>(null)

  // The max test's hand and pull length live beside maxStart: a resumed session must use what it started with.
  var maxHand by mutableStateOf("L")
  var maxDur by mutableFloatStateOf(7f)

  // Scanning stops with the app, so a running session would be saved with missing samples.
  fun cancelSessions() {
    liveStart = null
    maxStart = null
    repStart = null
  }

  private var _grip by mutableStateOf(prefs.getLong("grip", 1))
  var grip: Long
    get() = _grip
    set(v) {
      _grip = v
      prefs.edit().putLong("grip", v).apply()
    }

  private var _body by mutableFloatStateOf(prefs.getFloat("body", 70f))
  var body: Float
    get() = _body
    set(v) {
      _body = v
      prefs.edit().putFloat("body", v).apply()
    }

  private var _plan by mutableStateOf(decodePlan(prefs.getString("plan", null)))
  var plan: Plan
    get() = _plan
    set(p) {
      _plan = p
      prefs.edit().putString("plan", encodePlan(p)).apply()
    }

  init {
    val g = prefs.getString("grips", null)?.let { runCatching { decodeGrips(it) }.getOrNull() }
    if (g == null) {
      presets.forEachIndexed { i, n -> grips.add(Grip(i + 1L, n)) }
      saveGrips()
    } else {
      grips.addAll(g)
    }
    if (file.exists()) {
      runCatching { sessions.addAll(decodeSessions(file.readText())) }.onFailure {
        // Keep the unreadable file for recovery instead of overwriting it on the next save.
        file.renameTo(File(file.path + ".bad"))
      }
    }
  }

  fun gripName(id: Long) = grips.firstOrNull { it.id == id }?.name ?: if (id == 0L) "No grip" else "Deleted grip"

  // Best max pull on a grip for one hand, or null before the first test.
  fun best(grip: Long, hand: String) =
    sessions.filter { it.type == "max" && it.grip == grip && it.hand == hand }.maxOfOrNull { it.peak }

  fun addGrip(name: String) {
    val g = Grip(nextGripId(grips, sessions), name)
    grips.add(g)
    grip = g.id
    saveGrips()
  }

  // Sessions keep the id, so their history reads "Deleted grip".
  fun removeGrip(g: Grip) {
    grips.remove(g)
    if (grip == g.id) grip = grips.firstOrNull()?.id ?: 0
    saveGrips()
  }

  private fun saveGrips() {
    prefs.edit().putString("grips", encodeGrips(grips)).apply()
  }

  fun add(s: Session) {
    sessions.add(0, s)
    save()
  }

  fun remove(s: Session) {
    sessions.remove(s)
    save()
  }

  // A kill mid-write must not leave a truncated sessions.json, so write aside and rename over it.
  private fun save() {
    val tmp = File(file.path + ".tmp")
    tmp.writeText(encodeSessions(sessions))
    tmp.renameTo(file)
  }
}
