package io.closedtest.sdk.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import io.closedtest.sdk.ClosedTest

/**
 * Handle returned by [installClosedTestScreenTracking]; call [close] to detach the listener.
 */
class ClosedTestScreenTrackingHandle internal constructor(
    private val navController: NavController,
    private val listener: NavController.OnDestinationChangedListener,
) : AutoCloseable {
    override fun close() {
        navController.removeOnDestinationChangedListener(listener)
    }
}

/**
 * Tracks [ClosedTest.trackScreen] on every navigation destination change.
 * Prefer [ClosedTestScreenTracking] in Compose when possible.
 */
fun NavController.installClosedTestScreenTracking(
    enabled: Boolean = true,
    screenName: (NavDestination) -> String? = { ClosedTestNavScreenNames.fromRoute(it.route) },
): ClosedTestScreenTrackingHandle? {
    if (!enabled) return null
    val listener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            screenName(destination)?.let { ClosedTest.trackScreen(it) }
        }
    addOnDestinationChangedListener(listener)
    return ClosedTestScreenTrackingHandle(this, listener)
}

/**
 * Automatic `screen_view` events for Jetpack Navigation Compose.
 *
 * Place next to your `NavHost`:
 * ```
 * navController.ClosedTestScreenTracking()
 * NavHost(navController, startDestination = "home") { ... }
 * ```
 *
 * For routes that may embed user ids in the path, pass a custom [screenName] mapper to static slugs.
 */
@Composable
fun NavController.ClosedTestScreenTracking(
    enabled: Boolean = true,
    screenName: (NavDestination) -> String? = { ClosedTestNavScreenNames.fromRoute(it.route) },
) {
    DisposableEffect(this, enabled) {
        val handle = installClosedTestScreenTracking(enabled = enabled, screenName = screenName)
        onDispose { handle?.close() }
    }
}
