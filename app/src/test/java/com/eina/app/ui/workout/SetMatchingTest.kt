package com.eina.app.ui.workout

import com.eina.app.data.db.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetMatchingTest {

    @Test
    fun `a warmup does not shift the working sets`() {
        val current = listOf(SetType.WARMUP, SetType.NORMAL, SetType.FAILURE)
        val previous = listOf(SetType.NORMAL, SetType.DROP)

        assertNull(matchingSetIndex(0, current, previous))
        assertEquals(0, matchingSetIndex(1, current, previous))
        assertEquals(1, matchingSetIndex(2, current, previous))
    }

    @Test
    fun `warmups align with warmups by their own ordinal`() {
        val current = listOf(SetType.WARMUP, SetType.WARMUP, SetType.NORMAL)
        val previous = listOf(SetType.WARMUP, SetType.NORMAL, SetType.WARMUP)

        assertEquals(0, matchingSetIndex(0, current, previous))
        assertEquals(2, matchingSetIndex(1, current, previous))
        assertEquals(1, matchingSetIndex(2, current, previous))
    }

    @Test
    fun `all non-warmup types share the working set sequence`() {
        val current = listOf(SetType.FAILURE, SetType.DROP, SetType.NORMAL)
        val candidate = listOf(SetType.NORMAL, SetType.FAILURE, SetType.DROP)

        assertEquals(0, matchingSetIndex(0, current, candidate))
        assertEquals(1, matchingSetIndex(1, current, candidate))
        assertEquals(2, matchingSetIndex(2, current, candidate))
    }
}
