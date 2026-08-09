package com.eina.app.domain

import com.eina.app.domain.Superset.Member
import org.junit.Assert.assertEquals
import org.junit.Test

class SupersetTest {

    private fun members(vararg pairs: Pair<Long, Int?>) = pairs.map { Member(it.first, it.second) }

    @Test
    fun `chi entra in un giro si sposta accanto ai compagni`() {
        val list = members(1L to 1, 2L to null, 3L to 1, 4L to null)
        val result = Superset.regroup(list, movedId = 4L, group = 1)

        assertEquals(listOf(1L, 3L, 4L, 2L), result.map { it.id })
        assertEquals(1, result.first { it.id == 4L }.group)
    }

    @Test
    fun `un giro nuovo nasce con un membro solo e non si scioglie`() {
        val list = members(1L to null, 2L to null)
        val result = Superset.regroup(list, movedId = 2L, group = Superset.nextGroup(list.map { it.group }))

        assertEquals(listOf(1L, 2L), result.map { it.id })
        assertEquals(1, result.first { it.id == 2L }.group)
    }

    @Test
    fun `uscendo da un giro di due chi resta torna esercizio singolo`() {
        val list = members(1L to 7, 2L to 7, 3L to null)
        val result = Superset.regroup(list, movedId = 2L, group = null)

        assertEquals(null, result.first { it.id == 1L }.group)
        assertEquals(null, result.first { it.id == 2L }.group)
        // Chi esce non si sposta: resterebbe un movimento inspiegabile.
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
    }

    @Test
    fun `un giro di tre regge la partenza di uno`() {
        val list = members(1L to 7, 2L to 7, 3L to 7)
        val result = Superset.regroup(list, movedId = 3L, group = null)

        assertEquals(listOf(7, 7, null), result.map { it.group })
    }

    @Test
    fun `cambiare giro sposta l'esercizio nel nuovo e scioglie quello rimasto solo`() {
        val list = members(1L to 1, 2L to 1, 3L to 2, 4L to 2, 5L to 2)
        val result = Superset.regroup(list, movedId = 2L, group = 2)

        assertEquals(listOf(1L, 3L, 4L, 5L, 2L), result.map { it.id })
        assertEquals(null, result.first { it.id == 1L }.group)
        assertEquals(2, result.first { it.id == 2L }.group)
    }

    @Test
    fun `le lettere seguono l'ordine in cui i giri compaiono`() {
        val letters = Superset.letters(listOf(null, 5, 5, null, 2, 2))

        assertEquals(mapOf(5 to "A", 2 to "B"), letters)
    }

    @Test
    fun `il numero di gruppo nuovo non riusa quelli in uso`() {
        assertEquals(1, Superset.nextGroup(listOf(null, null)))
        assertEquals(4, Superset.nextGroup(listOf(3, 3, null, 1)))
    }

    @Test
    fun `spostare un membro del giro sposta tutto il blocco`() {
        val list = members(1L to null, 2L to 3, 3L to 3, 4L to null)
        val result = Superset.moveBlock(list, movedId = 3L, delta = 1)

        assertEquals(listOf(1L, 4L, 2L, 3L), result.map { it.id })
    }

    @Test
    fun `un blocco gia in cima non si sposta piu su`() {
        val list = members(1L to 3, 2L to 3, 3L to null)

        assertEquals(list, Superset.moveBlock(list, movedId = 2L, delta = -1))
    }

    @Test
    fun `dissolveOrphans scioglie solo i giri rimasti soli`() {
        val result = Superset.dissolveOrphans(members(1L to 1, 2L to 2, 3L to 2))

        assertEquals(listOf(null, 2, 2), result.map { it.group })
    }
}
