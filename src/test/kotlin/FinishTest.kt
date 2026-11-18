package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinishTest {
  private val trace = listOf(Reading(10f, true, 500), Reading(30f, true, 1_500), Reading(5f, true, 9_000))
  private val t = Tally(trace.take(2), 50, 1)
  private val plan = Plan()

  @Test
  fun runningMaxIsSavedFromItsWindow() {
    val r = maxSession(0, 2_000, 0, trace, "L", 70f, 2, 99)!!
    assertEquals(30f, r.peak)
    assertEquals(99L, r.at)
    assertEquals("max", r.type)
  }

  @Test
  fun cancelledMaxIsNotSaved() {
    assertNull(maxSession(0, 2_000, null, trace, "L", 70f, 2, 99))
    assertNull(maxSession(0, 2_000, 5_000, trace, "L", 70f, 2, 99))
  }

  @Test
  fun runningRepeatersIsSaved() {
    val r = repSession(0, 0, t, 3, plan, 70f, 2, 99)!!
    assertEquals("repeaters", r.type)
    assertEquals(30f, r.peak)
  }

  @Test
  fun cancelledOrEmptyRepeatersIsNotSaved() {
    assertNull(repSession(0, null, t, 3, plan, 70f, 2, 99))
    assertNull(repSession(0, 0, Tally(emptyList(), 0, 0), 3, plan, 70f, 2, 99))
  }
}
