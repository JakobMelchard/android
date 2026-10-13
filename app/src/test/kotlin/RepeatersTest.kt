package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatersTest {
  // Two 10 s pulls with a 10 s rest: pull 0..10_000, rest 10_000..20_000, pull 20_000..30_000.
  private val ph = listOf(Phase("Pull", 10_000, 1, 1), Phase("Rest", 10_000, 1, 1), Phase("Pull", 10_000, 1, 2))
  private val starts = listOf(0L, 10_000, 20_000, 30_000)
  private val zone = 20f..25f
  private fun r(t: Long, kg: Float) = Reading(kg, true, t)

  @Test
  fun emptyTraceScoresZero() = assertEquals(Tally(emptyList(), 0, 0), tally(ph, starts, emptyList(), 0, zone))

  @Test
  fun samplesOutsidePullWindowsAreIgnored() {
    val t = tally(ph, starts, listOf(r(5_000, 22f), r(15_000, 30f), r(25_000, 10f)), 30_000, zone)
    assertEquals(listOf(5_000L, 25_000L), t.xs.map { it.t })
    assertEquals(50, t.inZone)
  }

  @Test
  fun windowStartIsInAndEndIsOut() {
    val t = tally(ph, starts, listOf(r(0, 22f), r(10_000, 22f), r(20_000, 22f), r(30_000, 22f)), 30_000, zone)
    assertEquals(listOf(0L, 20_000L), t.xs.map { it.t })
  }

  @Test
  fun zoneBoundsAreInclusive() = assertEquals(100, tally(ph, starts, listOf(r(1, 20f), r(2, 25f)), 30_000, zone).inZone)

  @Test
  fun samplesBeforeStartAreIgnored() = assertEquals(emptyList<Reading>(), tally(ph, starts.map { it + 1_000 }, listOf(r(500, 22f)), 0, zone).xs)

  @Test
  fun partialSessionCountsOnlyEndedPulls() {
    assertEquals(0, tally(ph, starts, emptyList(), 9_999, zone).reps)
    assertEquals(1, tally(ph, starts, emptyList(), 10_000, zone).reps)
    assertEquals(1, tally(ph, starts, emptyList(), 29_999, zone).reps)
    assertEquals(2, tally(ph, starts, emptyList(), 30_000, zone).reps)
  }
}
