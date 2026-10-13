package io.jakobmelchard.pull

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
  @Test
  fun greetingNamesTheTarget() = assertEquals("hello, x", greeting("x"))
}
