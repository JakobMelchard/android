package io.lilfeelz.fz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrecencyTest {
    private val day = 86_400_000L

    @Test fun usageReadsOnlyTheFirstTwoFields() = assertEquals(1L to 2L, usage("1|2|3"))

    @Test fun usageParsesNegativeValues() = assertEquals(-5L to -3L, usage("-5|-3"))

    @Test fun usageDoesNotTrimWhitespace() {
        for (s in listOf(" 1|2", "1|2 ", "1 |2")) assertEquals(0L to 0L, usage(s))
    }

    @Test fun usageRejectsOverflow() = assertEquals(0L to 0L, usage("9223372036854775808|0"))

    @Test fun frecencyAtTheLaunchInstantIsTheCount() = assertEquals(
        4.0,
        frecency("4|100", 100),
        1e-9
    )

    @Test fun zeroLaunchesStayZero() = assertEquals(0.0, frecency("0|0", day), 1e-9)

    @Test fun frecencyHalvesAfterADay() = assertEquals(3.0, frecency("6|1000", 1000 + day), 1e-9)

    @Test fun frecencyTendsToZeroButStaysPositive() {
        val f = frecency("10|0", 1000 * day)
        assertEquals(10.0 / 1001.0, f, 1e-12)
        assertTrue(f > 0.0)
    }

    @Test fun stampZeroRoundTrips() {
        assertEquals("0|0", stamp(0, 0))
        assertEquals(0L to 0L, usage(stamp(0, 0)))
    }
}
