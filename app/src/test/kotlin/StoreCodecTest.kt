package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StoreCodecTest {
  private val s = Session(1700000000000, "max", "L", 31.5f, 28f, 70f, "good", 3)

  @Test
  fun sessionsRoundTrip() = assertEquals(listOf(s, s.copy(at = 2, grip = 0)), decodeSessions(encodeSessions(listOf(s, s.copy(at = 2, grip = 0)))))

  @Test
  fun sessionWithoutGripReadsAsNone() =
    assertEquals(0L, decodeSessions("""[{"at":1,"type":"live","hand":"R","peak":1,"avg":1,"body":70,"note":""}]""")[0].grip)

  @Test
  fun truncatedSessionsThrow() {
    val text = encodeSessions(listOf(s))
    assertThrows(Exception::class.java) { decodeSessions(text.dropLast(5)) }
  }

  @Test
  fun gripsRoundTrip() = assertEquals(listOf(Grip(1, "Pinch"), Grip(9, "Sloper")), decodeGrips(encodeGrips(listOf(Grip(1, "Pinch"), Grip(9, "Sloper")))))

  @Test
  fun planRoundTrip() = assertEquals(Plan(5, 5, 4, 2, 90, 10.5f, 15f), decodePlan(encodePlan(Plan(5, 5, 4, 2, 90, 10.5f, 15f))))

  @Test
  fun badPlanFallsBackToDefault() {
    assertEquals(Plan(), decodePlan(null))
    assertEquals(Plan(), decodePlan("7,3,6"))
    assertEquals(Plan(), decodePlan("7,3,x,3,180,20.0,25.0"))
  }

  @Test
  fun newGripSkipsIdsOfDeletedGripsStillInHistory() {
    val grips = listOf(Grip(1, "Pinch"))
    assertEquals(2L, nextGripId(grips, listOf(s.copy(grip = 1))))
    assertEquals(4L, nextGripId(grips, listOf(s.copy(grip = 3))))
    assertEquals(1L, nextGripId(emptyList(), emptyList()))
  }

  @Test
  fun planIsCapped() {
    val big = Plan(1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 20f, 25f)
    assertEquals(Plan(3600, 3600, 50, 20, 3600, 20f, 25f), big.capped())
    assertEquals(Plan(3600, 3600, 50, 20, 3600, 20f, 25f), decodePlan(encodePlan(big)))
    assertEquals(Plan(1, 0, 1, 1, 0, 20f, 25f), Plan(0, -5, 0, 0, -1, 20f, 25f).capped())
  }

  @Test
  fun targetBandIsFiniteNonNegativeAndOrdered() {
    assertEquals(Plan(lo = 10f, hi = 30f), Plan(lo = 30f, hi = 10f).capped())
    assertEquals(Plan(lo = 0f, hi = 5f), Plan(lo = -3f, hi = 5f).capped())
    assertEquals(Plan(lo = 0f, hi = 0f), Plan(lo = Float.NaN, hi = Float.POSITIVE_INFINITY).capped())
    assertEquals(Plan(lo = 10f, hi = 30f), decodePlan("7,3,6,3,180,30.0,10.0"))
  }
}
