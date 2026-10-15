package com.nikhil.yt.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ListExtTest {
    @Test
    fun customOrderPutsNewItemsFirstAndKeepsSavedOrder() {
        val result = listOf("new2", "b", "new1", "a", "c").inCustomOrder(listOf("c", "gone", "a", "b")) { it }
        assertEquals(listOf("new2", "new1", "c", "a", "b"), result)
    }

    @Test
    fun playTimeMixKeepsEveryItemOnceAndStartsWithFirst() {
        val items = (1..9).toList()
        val result = items.playTimeMix({ it.toLong() }, first = 4, random = Random(1))
        assertEquals(4, result.first())
        assertEquals(items.toSet(), result.toSet())
        assertEquals(items.size, result.size)
    }

    @Test
    fun playTimeMixMixesLeastPlayedIntoTheFront() {
        // 1..10 most played, 11..20 least played (play time = 100 - n)
        val items = (1..20).toList()
        val random = Random(42)
        var leastPlayedInFirstTen = 0
        repeat(1000) {
            leastPlayedInFirstTen += items.playTimeMix({ 100L - it }, random = random).take(10).count { it > 10 }
        }
        val share = leastPlayedInFirstTen / 10000.0
        assertTrue("least-played share of the first half was $share", share in 0.3..0.5)
    }
}
