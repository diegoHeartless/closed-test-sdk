package io.closedtest.sdk.internal

/** Client-side mirror of server telegram username normalization (ingest roster contacts). */
internal object TelegramUsername {
    private val USERNAME_RE = Regex("^[a-zA-Z][a-zA-Z0-9_]{4,31}$")

    fun normalize(raw: String): String? {
        var v = raw.trim()
        if (v.isEmpty()) return null
        val lower = v.lowercase()
        if (lower.startsWith("https://t.me/") || lower.startsWith("http://t.me/")) {
            v = v.substringAfterLast('/')
        } else if (lower.startsWith("t.me/")) {
            v = v.removePrefix("t.me/")
        }
        if (v.startsWith("@")) {
            v = v.removePrefix("@")
        }
        v = v.trim()
        return if (USERNAME_RE.matches(v)) v else null
    }
}
