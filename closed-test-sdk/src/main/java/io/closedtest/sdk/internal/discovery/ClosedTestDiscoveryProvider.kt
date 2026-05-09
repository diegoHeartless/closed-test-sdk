package io.closedtest.sdk.internal.discovery

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import io.closedtest.sdk.BuildConfig

/**
 * Маркер интеграции SDK для **ProofFlow**: authority устанавливается как
 * `applicationId + "[suffix]"` (см. [io.closedtest.sdk.ClosedTest.DISCOVERY_AUTHORITY_SUFFIX]).
 *
 * Доступ на чтение метаданных только для UID процессов с package name из whitelist
 * (публичный package **ProofFlow** и типичный debug-suffix). Прочим вызывающим —
 * пустой курсор (без исключения).
 */
internal class ClosedTestDiscoveryProvider : ContentProvider() {

    private var discoveryEnabled: Boolean = true

    override fun attachInfo(context: android.content.Context, info: android.content.ProviderInfo?) {
        super.attachInfo(context, info)
        discoveryEnabled = readDiscoveryEnabledFlag(context)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val ctx = context ?: return emptyCursor(projection)
        if (!discoveryEnabled) return emptyCursor(projection)
        if (!isCallerAllowed(ctx)) return emptyCursor(projection)

        val cols = normalizeProjection(projection)
        val cursor = MatrixCursor(cols)
        val row =
            cols.map { key ->
                when (key) {
                    COLUMN_SDK_VERSION -> BuildConfig.SDK_VERSION
                    COLUMN_HOST_PACKAGE -> ctx.packageName
                    else -> null
                }
            }.toTypedArray()
        cursor.addRow(row)
        return cursor
    }

    override fun getType(uri: Uri): String =
        "${android.content.ContentResolver.CURSOR_DIR_BASE_TYPE}/vnd.io.closedtest.discovery"

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("closed-test discovery provider is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("closed-test discovery provider is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("closed-test discovery provider is read-only")

    private fun emptyCursor(projection: Array<out String>?): Cursor =
        MatrixCursor(normalizeProjection(projection))

    private fun normalizeProjection(projection: Array<out String>?): Array<String> =
        if (projection.isNullOrEmpty()) {
            DEFAULT_PROJECTION
        } else {
            projection.filter { it in SUPPORTED_COLUMNS }.toTypedArray().takeUnless { it.isEmpty() }
                ?: DEFAULT_PROJECTION
        }

    private fun isCallerAllowed(context: android.content.Context): Boolean {
        val uid = Binder.getCallingUid()
        val callerPackages =
            context.packageManager.getPackagesForUid(uid) ?: return false
        return callerPackages.any { pkg -> pkg in AllowedProofFlowPackages.ALL }
    }

    private fun readDiscoveryEnabledFlag(context: android.content.Context): Boolean =
        runCatching {
            val ai =
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA,
                )
            val bundle = ai.metaData ?: return true
            if (!bundle.containsKey(META_DISCOVERY_ENABLED)) return true
            bundle.getBoolean(META_DISCOVERY_ENABLED, true)
        }.getOrElse { true }

    companion object {
        const val COLUMN_SDK_VERSION: String = "sdk_version"
        const val COLUMN_HOST_PACKAGE: String = "host_package"

        /** Выключить маркер: `<meta-data android:name="io.closedtest.sdk.discovery_enabled" android:value="false"/>` */
        const val META_DISCOVERY_ENABLED: String = "io.closedtest.sdk.discovery_enabled"

        private val DEFAULT_PROJECTION = arrayOf(COLUMN_SDK_VERSION, COLUMN_HOST_PACKAGE)
        private val SUPPORTED_COLUMNS = setOf(COLUMN_SDK_VERSION, COLUMN_HOST_PACKAGE)
    }
}

/** Package names приложения ProofFlow, которым разрешено читать маркер (см. ProofFlow `TEST_DISCOVERY.md` §3.4). */
internal object AllowedProofFlowPackages {
    val ALL: Set<String> =
        buildSet {
            add("com.ground.proofflow")
            add("com.ground.proofflow.debug")
        }
}
