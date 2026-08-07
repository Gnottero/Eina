package com.eina.app.ui.components

/**
 * Tetto ai pesi digitabili, sia sul carico di una serie sia sul peso corporeo: oltre questa
 * soglia un valore e' quasi certamente un errore di battitura, e falserebbe volume e PR.
 */
const val MAX_WEIGHT_KG = 999.0

/**
 * Ripulisce quello che si sta scrivendo in un campo peso: solo cifre e un separatore decimale,
 * e mai un valore oltre [MAX_WEIGHT_KG]. Se il nuovo carattere sfora, si tiene il testo precedente
 * invece di correggerlo di nascosto sotto le dita.
 */
fun sanitizeWeightInput(current: String, typed: String): String {
    val filtered = buildString {
        var separatorSeen = false
        typed.forEach { char ->
            when {
                char.isDigit() -> append(char)
                (char == '.' || char == ',') && !separatorSeen -> {
                    separatorSeen = true
                    append('.')
                }
            }
        }
    }
    if (filtered.isEmpty()) return ""
    val value = filtered.toDoubleOrNull() ?: return current
    return if (value > MAX_WEIGHT_KG) current else filtered
}
