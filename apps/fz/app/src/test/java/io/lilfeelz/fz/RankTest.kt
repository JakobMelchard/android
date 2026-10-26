package io.lilfeelz.fz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankTest {
    private fun app(label: String) = App(label, "p.$label", "c")

    @Test fun emptyQueryMatchesEveryLabel() {
        assertEquals(-0.03, fuzzy("", "abc")!!, 1e-9)
        assertTrue(fuzzy("", "zzz") != null)
    }

    @Test fun fuzzyIsCaseInsensitiveOnBothSides() =
        assertEquals(fuzzy("chr", "chrome"), fuzzy("CHR", "CHROME"))

    @Test fun firstCharAndWordStartScoreTheSame() {
        assertEquals(1.92, fuzzy("y", "yt music")!!, 1e-9)
        assertEquals(1.92, fuzzy("m", "yt music")!!, 1e-9)
    }

    @Test fun midWordScoresLowerThanWordStart() = assertEquals(0.92, fuzzy("t", "yt music")!!, 1e-9)

    @Test fun consecutiveCharsScoreThree() = assertEquals(4.98, fuzzy("ab", "ab")!!, 1e-9)

    @Test fun onlySpaceIsAWordStart() {
        assertEquals(0.97, fuzzy("m", "a-m")!!, 1e-9)
        assertEquals(1.97, fuzzy("m", "a m")!!, 1e-9)
    }

    @Test fun repeatedQueryCharNeedsTwoOccurrences() {
        assertNull(fuzzy("aa", "a"))
        assertEquals(2.97, fuzzy("aa", "aba")!!, 1e-9)
    }

    @Test fun nonEmptyQueryCannotMatchAnEmptyLabel() = assertNull(fuzzy("a", ""))

    @Test fun whitespaceOnlyQueryBehavesAsBlank() {
        val apps = listOf("A", "B", "C").map(::app)
        val f = mapOf(apps[0] to 1.0, apps[1] to 3.0, apps[2] to 2.0)
        assertEquals(listOf("B", "C", "A"), rank(" ", apps) { f.getValue(it) }.map { it.label })
    }

    @Test fun blankQueryWithEqualFrecencyKeepsInputOrder() {
        val apps = listOf("A", "B", "C").map(::app)
        assertEquals(listOf("A", "B", "C"), rank("", apps) { 1.0 }.map { it.label })
    }

    @Test fun nonMatchingAppsAreDropped() = assertTrue(
        rank("z", listOf(app("Alpha"))) { 0.0 }.isEmpty()
    )

    @Test fun emptyAppListRanksEmpty() = assertTrue(rank("a", emptyList<App>()) { 0.0 }.isEmpty())

    @Test fun frecencyCanOvercomeASmallFuzzyLead() {
        val lean = app("ab")
        val warm = app("axb")
        assertEquals(
            listOf("axb", "ab"),
            rank("ab", listOf(lean, warm)) { if (it == warm) 10.0 else 0.0 }.map { it.label }
        )
    }

    @Test fun frecencyCannotOvercomeWhenItIsSmallerThanTheLead() {
        val lean = app("ab")
        val warm = app("axb")
        assertEquals(
            listOf("ab", "axb"),
            rank("ab", listOf(lean, warm)) { if (it == warm) 1.0 else 0.0 }.map { it.label }
        )
    }

    @Test fun nonMatchingAppIsExcludedEvenWithHighFrecency() {
        val miss = app("zzz")
        val hit = app("ab")
        assertEquals(
            listOf("ab"),
            rank("ab", listOf(miss, hit)) { if (it == miss) 100.0 else 0.0 }.map { it.label }
        )
    }

    @Test fun frecencyIsResolvedOncePerApp() {
        var calls = 0
        rank("", listOf(app("A"), app("B"), app("C"))) {
            calls++
            0.0
        }
        assertEquals(3, calls)
        calls = 0
        rank("a", listOf(app("A"), app("B"))) {
            calls++
            0.0
        }
        assertEquals(2, calls)
    }

    @Test fun staleKeysOnEmptyStoredSet() =
        assertEquals(emptySet<String>(), staleKeys(emptySet(), listOf(app("a"))))

    @Test fun staleKeysOnAllListed() {
        val a = app("a")
        assertEquals(emptySet<String>(), staleKeys(setOf(a.key), listOf(a)))
    }

    @Test fun staleKeysOnNoneListed() {
        val a = app("a")
        assertEquals(setOf("x/c"), staleKeys(setOf("x/c"), listOf(a)))
    }
}
