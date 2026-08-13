package com.eina.app.ui.components

/**
 * Cap on typed weights, both for a set load and for bodyweight: above this threshold a value is
 * almost certainly a typo, and it would skew volume and PRs.
 */
const val MAX_WEIGHT_KG = 999.0

/**
 * Sanitises what is being typed in a weight field: digits and one decimal separator only, never
 * above [MAX_WEIGHT_KG]. If the new character exceeds it the previous text is kept, rather than
 * silently rewriting the field under the user's fingers.
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
