package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LockTest {
  // 12 bytes: 0x03E8 = 1000 hundredths at 10..11, so 10 kg.
  private val frame = ByteArray(12).also {
    it[10] = 0x03
    it[11] = 0xE8.toByte()
  }

  @Test
  fun otherDevicesAreRejectedOnceLocked() {
    val lock = Lock()
    assertNotNull(lock.accept("A", frame, 0))
    assertNull(lock.accept("B", frame, 1))
    assertEquals(10f, lock.accept("A", frame, 2)!!.kg, 0.001f)
  }

  @Test
  fun invalidFirstFrameDoesNotLock() {
    val lock = Lock()
    assertNull(lock.accept("A", ByteArray(3), 0))
    assertNull(lock.addr)
    assertNotNull(lock.accept("B", frame, 1))
    assertEquals("B", lock.addr)
  }
}
