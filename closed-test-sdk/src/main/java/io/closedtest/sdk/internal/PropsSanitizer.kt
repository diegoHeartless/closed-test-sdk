package io.closedtest.sdk.internal

/** Removes keys blocked by the ingest spec (PII list) from `track_event` props (case-insensitive keys). */
internal object PropsSanitizer {

    private val forbidden = setOf(
        "email", "mail", "phone", "telephone", "msisdn", "password", "passwd", "secret",
        "token", "access_token", "refresh_token", "api_key", "authorization", "cookie",
        "ssn", "credit_card", "card_number", "iban", "gps", "latitude", "longitude",
        "lat", "lng", "precise_location", "street_address", "address_line", "full_name",
        "first_name", "last_name", "birthdate", "dob", "government_id", "passport",
        "driver_license", "health", "diagnosis", "imei", "serial", "android_id",
    )

    fun sanitize(props: Map<String, String>?): Map<String, String>? {
        if (props.isNullOrEmpty()) return null
        val out = LinkedHashMap<String, String>()
        for ((k, v) in props) {
            if (k.lowercase() in forbidden) continue
            out[k] = v
        }
        return out.ifEmpty { null }
    }
}
