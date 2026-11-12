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
  fun seedUsesGivenSeparator() {
    assertEquals("72,5", numText(72.5f, ','))
    assertEquals("72.5", numText(72.5f, '.'))
    assertEquals("70", numText(70f, ','))
  }

  @Test
  fun seededTextParsesBackForEverySeparator() {
    for (sep in listOf(',', '.', '\u066B')) {
      assertEquals(72.5f, parseNum(numText(72.5f, sep), sep))
      assertEquals(72.6f, parseNum(numText(72.5f, sep).replace('5', '6'), sep))
    }
    // a period or comma typed by hand still parses under an Arabic locale
    assertEquals(72.5f, parseNum("72.5", '\u066B'))
    assertEquals(72.5f, parseNum("72,5", '\u066B'))
  }
}
