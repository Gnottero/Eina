package com.eina.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `string list round trips through json`() {
        val list = listOf("Chest", "Triceps", "Front Delts")
        val json = converters.fromStringList(list)
        assertEquals(list, converters.toStringList(json))
    }

    @Test
    fun `string list with quotes and backslashes round trips`() {
        val list = listOf("""Quads "front"""", """Back\Lats""")
        val json = converters.fromStringList(list)
        assertEquals(list, converters.toStringList(json))
    }

    @Test
    fun `empty string list round trips`() {
        assertEquals(emptyList<String>(), converters.toStringList(converters.fromStringList(emptyList())))
    }

    @Test
    fun `weight type round trips`() {
        WeightType.values().forEach {
            assertEquals(it, converters.toWeightType(converters.fromWeightType(it)))
        }
    }

    @Test
    fun `nullable playlist type round trips`() {
        assertEquals(null, converters.toPlaylistType(converters.fromPlaylistType(null)))
        PlaylistType.values().forEach {
            assertEquals(it, converters.toPlaylistType(converters.fromPlaylistType(it)))
        }
    }
}
