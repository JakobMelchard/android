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

class Store(ctx: Context) {
  private val file = File(ctx.filesDir, "sessions.json")
  private val prefs = ctx.getSharedPreferences("pull", Context.MODE_PRIVATE)
  val sessions = mutableStateListOf<Session>()
  val grips = mutableStateListOf<Grip>()

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

  private var _plan by mutableStateOf(
    prefs.getString("plan", null)?.split(",")?.let {
      Plan(it[0].toInt(), it[1].toInt(), it[2].toInt(), it[3].toInt(), it[4].toInt(), it[5].toFloat(), it[6].toFloat())
    } ?: Plan(),
  )
  var plan: Plan
    get() = _plan
    set(p) {
      _plan = p
      prefs.edit().putString("plan", listOf(p.work, p.rest, p.reps, p.sets, p.setRest, p.lo, p.hi).joinToString(",")).apply()
    }

  init {
    val g = prefs.getString("grips", null)
    if (g == null) {
      presets.forEachIndexed { i, n -> grips.add(Grip(i + 1L, n)) }
      saveGrips()
    } else {
      val a = JSONArray(g)
      for (i in 0 until a.length()) a.getJSONObject(i).let { grips.add(Grip(it.getLong("id"), it.getString("name"))) }
    }
    if (file.exists()) {
      val a = JSONArray(file.readText())
      for (i in 0 until a.length()) {
        val o = a.getJSONObject(i)
        sessions.add(
          Session(
            o.getLong("at"), o.getString("type"), o.getString("hand"),
            o.getDouble("peak").toFloat(), o.getDouble("avg").toFloat(),
            o.getDouble("body").toFloat(), o.getString("note"), o.optLong("grip", 0),
          ),
        )
      }
    }
  }

  fun gripName(id: Long) = grips.firstOrNull { it.id == id }?.name ?: if (id == 0L) "No grip" else "Deleted grip"

  // Best max pull on a grip for one hand, or null before the first test.
  fun best(grip: Long, hand: String) =
    sessions.filter { it.type == "max" && it.grip == grip && it.hand == hand }.maxOfOrNull { it.peak }

  fun addGrip(name: String) {
    // Past every id a session still holds, so a new grip never inherits a deleted one's history.
    val g = Grip(maxOf(grips.maxOfOrNull { it.id } ?: 0, sessions.maxOfOrNull { it.grip } ?: 0) + 1, name)
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
    val a = JSONArray()
    grips.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name)) }
    prefs.edit().putString("grips", a.toString()).apply()
  }

  fun add(s: Session) {
    sessions.add(0, s)
    save()
  }

  fun remove(s: Session) {
    sessions.remove(s)
    save()
  }

  private fun save() {
    val a = JSONArray()
    sessions.forEach {
      a.put(
        JSONObject()
          .put("at", it.at).put("type", it.type).put("hand", it.hand)
          .put("peak", it.peak.toDouble()).put("avg", it.avg.toDouble())
          .put("body", it.body.toDouble()).put("note", it.note).put("grip", it.grip),
      )
    }
    file.writeText(a.toString())
  }
}
