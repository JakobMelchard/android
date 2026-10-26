package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeTest {
  // 17-byte payload as seen from a WH-C06: 0x0BB8 = 3000 hundredths, byte 14 = stable kg.
  private fun payload(hi: Int, lo: Int, flags: Int) = ByteArray(17).also {
    it[10] = hi.toByte()
    it[11] = lo.toByte()
    it[14] = flags.toByte()
  }

  @Test
  fun kilograms() {
    val r = decode(payload(0x0B, 0xB8, 0x11), 5)!!
    assertEquals(30f, r.kg, 0.001f)
    assertTrue(r.stable)
    assertEquals(5L, r.t)
  }

  @Test
  fun poundsConvertToKilograms() = assertEquals(13.6078f, decode(payload(0x0B, 0xB8, 0x00), 0)!!.kg, 0.001f)

  @Test
  fun highByteIsUnsigned() = assertEquals(655.35f * 0.45359237f, decode(payload(0xFF, 0xFF, 0x02), 0)!!.kg, 0.01f)

  @Test
  fun overCapacityIsDropped() = assertNull(decode(payload(0xFF, 0xFF, 0x01), 0))

  @Test
  fun shortPayloadIsDropped() = assertNull(decode(ByteArray(11), 0))

  @Test
  fun missingUnitByteMeansKilograms() = assertEquals(30f, decode(payload(0x0B, 0xB8, 0).copyOf(12), 0)!!.kg, 0.001f)

  @Test
  fun scheduleHasNoTrailingRest() {
    val ph = schedule(Plan(work = 7, rest = 3, reps = 2, sets = 2, setRest = 60))
    assertEquals(listOf("Get ready", "Pull", "Rest", "Pull", "Set rest", "Pull", "Rest", "Pull"), ph.map { it.name })
    assertEquals(5_000L + 4 * 7_000 + 2 * 3_000 + 60_000, ph.sumOf { it.ms })
  }
}
