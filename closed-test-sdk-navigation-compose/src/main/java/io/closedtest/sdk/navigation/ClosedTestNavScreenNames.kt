package io.closedtest.sdk.navigation

/**
 * Maps Navigation graph routes to stable [ClosedTest.trackScreen] names (no PII in `screen_name`; see `spec.md` §8).
 */
object ClosedTestNavScreenNames {
    /**
     * Default mapping: graph route without query/fragment.
     * Dynamic argument values in the resolved path are **not** included when you use [androidx.navigation.NavDestination.route]
     * (the graph pattern, e.g. `profile/{userId}`).
     */
    fun fromRoute(route: String?): String? {
        val trimmed = route?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val withoutQuery = trimmed.substringBefore('?').substringBefore('#').trim()
        return withoutQuery.takeIf { it.isNotEmpty() }
    }
}
