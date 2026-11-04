package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumFieldTest {
  @Test
  fun decimalCommaParses() {
    assertEquals(72.5f, parseNum("72,5"))
    assertEquals(72.5f, parseNum("72.5"))
    assertEquals(70f, parseNum(" 70 "))
    assertNull(parseNum("72,5,1"))
    assertNull(parseNum(""))
  }

  @Test
  fun seedUsesTheLocaleSeparator() {
    assertEquals("72,5", numText(72.5f, ','))
    assertEquals("72.5", numText(72.5f, '.'))
    assertEquals("70", numText(70f, ','))
    assertEquals(72.5f, parseNum(numText(72.5f, ',')))
  }
}
