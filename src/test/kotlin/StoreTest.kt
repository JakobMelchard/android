package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StoreTest {
  @get:Rule val tmp = TemporaryFolder()

  private class MapKv(val m: MutableMap<String, Any> = mutableMapOf()) : Kv {
    override fun get(k: String) = m[k]
    override fun set(k: String, v: Any) {
      m[k] = v
    }
  }

  private val s = Session(1, "max", "L", 31.5f, 28f, 70f, "good", 3)
  private val kv = MapKv()
  private fun file() = File(tmp.root, "sessions.json")

  @Test
  fun corruptSessionsAreKeptAsBadAndNotOverwritten() {
    file().writeText("[{")
    val store = Store(tmp.root, kv)
    assertTrue(store.sessions.isEmpty())
    assertFalse(file().exists())
    assertEquals("[{", File(tmp.root, "sessions.json.bad").readText())
  }

  @Test
  fun savedSessionsLoadAgain() {
    Store(tmp.root, kv).add(s)
    assertEquals(listOf(s), Store(tmp.root, kv).sessions.toList())
    assertFalse(File(tmp.root, "sessions.json.tmp").exists())
  }

  @Test
  fun failedSaveKeepsTheOldFileAndSetsError() {
    val store = Store(tmp.root, kv)
    store.add(s)
    val old = file().readText()
    File(tmp.root, "sessions.json.tmp").mkdir()
    store.add(s.copy(at = 2))
    assertEquals("Could not save sessions", store.saveError)
    assertEquals(old, file().readText())
  }

  @Test
  fun corruptGripsAreKeptUnderBackupKeyNotOverwritten() {
    kv["grips"] = "not json"
    val store = Store(tmp.root, kv)
    assertEquals(presets, store.grips.map { it.name })
    assertEquals("not json", kv["grips.bad"])
  }

  @Test
  fun firstRunSeedsPresetsWithoutBackup() {
    val store = Store(tmp.root, kv)
    assertEquals(presets, store.grips.map { it.name })
    assertNull(kv["grips.bad"])
  }

  @Test
  fun gripsPersist() {
    Store(tmp.root, kv).addGrip("Edge")
    assertEquals("Edge", Store(tmp.root, kv).grips.last().name)
  }
}
