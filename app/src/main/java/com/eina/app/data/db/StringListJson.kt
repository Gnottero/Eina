package com.eina.app.data.db

// Codec JSON minimale per List<String> (no dipendenza esterna: solo array piatto di stringhe).
internal fun encodeStringListJson(items: List<String>): String {
    val sb = StringBuilder("[")
    items.forEachIndexed { index, item ->
        if (index > 0) sb.append(',')
        sb.append('"')
        for (c in item) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }
    sb.append(']')
    return sb.toString()
}

internal fun decodeStringListJson(json: String): List<String> {
    val trimmed = json.trim()
    if (trimmed.length <= 2) return emptyList()
    val inner = trimmed.substring(1, trimmed.length - 1)
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inString = false
    var i = 0
    while (i < inner.length) {
        val c = inner[i]
        if (!inString) {
            if (c == '"') inString = true
        } else if (c == '\\' && i + 1 < inner.length) {
            current.append(inner[i + 1])
            i++
        } else if (c == '"') {
            inString = false
            result.add(current.toString())
            current.clear()
        } else {
            current.append(c)
        }
        i++
    }
    return result
}
