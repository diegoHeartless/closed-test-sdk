package io.closedtest.sdk

/**
 * Initialization payload for SDK handshake.
 *
 * Use this overload when the host app wants to explicitly describe the test channel
 * during `POST /v1/init`, for example in the mutual-testing marketplace flow.
 *
 * @property ownerEmail Required organizer/developer (and tester-identity) email. Must be non-blank.
 *   Sent as `owner_email` on `POST /v1/init` for account matching and test upsert.
 * @property googleGroupUrl Required Google Group URL (or equivalent join channel). Must be non-blank.
 * @property inviteLink Required Play testing / install link. Must be non-blank.
 * @property publishableKey Optional Advanced ingest key. Leave `null` or blank for Base ingest.
 */
data class ClosedTestInit(
    val ownerEmail: String,
    val googleGroupUrl: String,
    val inviteLink: String,
    val publishableKey: String? = null,
) {
    init {
        require(ownerEmail.trim().isNotEmpty()) {
            "ClosedTestInit.ownerEmail is required and must be non-blank"
        }
        require(googleGroupUrl.trim().isNotEmpty()) {
            "ClosedTestInit.googleGroupUrl is required and must be non-blank"
        }
        require(inviteLink.trim().isNotEmpty()) {
            "ClosedTestInit.inviteLink is required and must be non-blank"
        }
    }
}
