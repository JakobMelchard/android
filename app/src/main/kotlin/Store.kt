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

// type is live, max or repeaters; hand is L, R or Both.
data class Session(
  val at: Long,
  val type: String,
  val hand: String,
  val peak: Float,
  val avg: Float,
  val body: Float,
  val note: String,
)

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
    if (file.exists()) {
      val a = JSONArray(file.readText())
      for (i in 0 until a.length()) {
        val o = a.getJSONObject(i)
        sessions.add(
          Session(
            o.getLong("at"), o.getString("type"), o.getString("hand"),
            o.getDouble("peak").toFloat(), o.getDouble("avg").toFloat(),
            o.getDouble("body").toFloat(), o.getString("note"),
          ),
        )
      }
    }
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
          .put("body", it.body.toDouble()).put("note", it.note),
      )
    }
    file.writeText(a.toString())
  }
}
