package com.eina.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightInputTest {

    @Test
    fun `accetta cifre e un solo separatore decimale`() {
        assertEquals("52.5", sanitizeWeightInput("52.", "52,5"))
        assertEquals("52.5", sanitizeWeightInput("52.", "52.5"))
        assertEquals("525", sanitizeWeightInput("52", "5a2b5"))
    }

    @Test
    fun `campo svuotabile`() {
        assertEquals("", sanitizeWeightInput("52", ""))
    }

    @Test
    fun `rifiuta i valori oltre il tetto tenendo il testo precedente`() {
        assertEquals("999", sanitizeWeightInput("999", "9999"))
        assertEquals("999.9", sanitizeWeightInput("999.9", "999.99"))
        assertEquals("999", sanitizeWeightInput("999", "1000"))
    }
}
