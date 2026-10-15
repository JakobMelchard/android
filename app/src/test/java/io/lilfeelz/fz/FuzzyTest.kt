package io.lilfeelz.fz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyTest {
    private fun app(label: String) = App(label, "p.$label", "c")
    private fun score(q: String, s: String) = fuzzy(q, s)!!

    @Test fun subsequenceMatches() {
        assert(fuzzy("ytm", "YT Music") != null)
        assert(fuzzy("chr", "Chrome") != null)
    }

    @Test fun outOfOrderMisses() = assertNull(fuzzy("mty", "YT Music"))

    @Test fun consecutiveBeatsScattered() =
        assertTrue(score("cal", "Calendar") > score("cal", "Clock Alarm"))

    @Test fun wordStartBeatsMidWord() = assertTrue(score("m", "YT Music") > score("m", "Gmail"))

    @Test fun shorterLabelWinsTie() = assertTrue(score("cam", "Cam") > score("cam", "Camera"))

    @Test fun rankOrdersByScore() {
        val apps = listOf("Clock Manager", "Camera", "Cam Scanner").map(::app)
        assertEquals(
            listOf("Clock Manager", "Camera", "Cam Scanner"),
            rank("cm", apps) { 0.0 }.map { it.label }
        )
    }

    @Test fun frecencyBreaksFuzzyTie() {
        val a = app("Alpha")
        val hot = a.copy(pkg = "hot")
        assertEquals(hot, rank("alp", listOf(a, hot)) { if (it == hot) 5.0 else 0.0 }.first())
    }

    @Test fun blankQueryOrdersByFrecency() {
        val apps = listOf("A", "B", "C").map(::app)
        val f = mapOf(apps[0] to 1.0, apps[1] to 3.0, apps[2] to 2.0)
        assertEquals(listOf("B", "C", "A"), rank("", apps) { f.getValue(it) }.map { it.label })
    }

    @Test fun malformedUsageReadsAsZero() {
        for (s in listOf(null, "", "x", "5", "a|b", "5|x", "|")) assertEquals(0L to 0L, usage(s))
        assertEquals(0.0, frecency("junk", 1_000_000), 0.0)
    }

    @Test fun frecencyDecaysWithDays() {
        val day = 86_400_000L
        assertEquals(4.0, frecency("4|0", 0), 1e-9)
        assertEquals(2.0, frecency("4|0", day), 1e-9)
        assertEquals(3L to 7L, usage("3|7"))
    }

    @Test fun launchTimeInTheFutureCountsAsNow() {
        val day = 86_400_000L
        for (ahead in listOf(1L, day, 3 * day)) assertEquals(4.0, frecency("4|$ahead", 0), 1e-9)
    }

    @Test fun staleKeysDropsOnlyUnlisted() {
        val a = app("a")
        assertEquals(setOf("gone/c"), staleKeys(setOf(a.key, "gone/c"), listOf(a)))
    }

    @Test fun staleKeysKeepsAllOnEmptyList() =
        assertEquals(emptySet<String>(), staleKeys(setOf("p/c"), emptyList()))

    @Test fun stampRoundTripsThroughUsage() = assertEquals(8L to 1234L, usage(stamp(8, 1234)))

    @Test fun forgottenAppDropsToBottom() {
        val apps = listOf("A", "B", "C").map(::app)
        val stored = mutableMapOf(apps[0].key to "9|0", apps[1].key to "5|0", apps[2].key to "1|0")
        stored.remove(apps[0].key)
        assertEquals(
            listOf("B", "C", "A"),
            rank("", apps) { frecency(stored[it.key], 0) }.map { it.label }
        )
    }

    @Test fun ownProfileKeyKeepsOldForm() = assertEquals("p/c", App("x", "p", "c").key)

    @Test fun profilesRankSeparately() {
        val own = App("Chrome", "p", "c")
        val work = own.copy(profile = 10)
        assertEquals("p/c@10", work.key)
        val stored = mapOf(work.key to "5|0")
        assertEquals(listOf(work, own), rank("", listOf(own, work)) { frecency(stored[it.key], 0) })
    }

    @Test fun staleKeysTracksProfilesApart() {
        val own = App("Chrome", "p", "c")
        assertEquals(setOf("p/c@10"), staleKeys(setOf(own.key, "p/c@10"), listOf(own)))
    }

    @Test fun asksHomeOnlyWhenAvailableUnheldAndNotAsked() {
        assertTrue(askHome(available = true, held = false, asked = false))
        val declined = listOf(
            Triple(false, false, false),
            Triple(true, true, false),
            Triple(true, false, true)
        )
        for ((a, h, q) in declined) assertFalse(askHome(a, h, q))
    }
}
