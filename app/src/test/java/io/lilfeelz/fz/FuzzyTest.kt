package io.lilfeelz.fz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuzzyTest {
  @Test fun subsequenceMatches() {
    assert(fuzzy("ytm", "YT Music") != null)
    assert(fuzzy("chr", "Chrome") != null)
  }

  @Test fun outOfOrderMisses() = assertNull(fuzzy("mty", "YT Music"))

  @Test fun consecutiveBeatsScattered() {
    val apps = listOf("Calendar", "Camera", "Calculator")
    assertEquals("Camera", apps.maxBy { fuzzy("cam", it) ?: -1.0 })
  }
}
