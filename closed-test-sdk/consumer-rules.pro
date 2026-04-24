# --- Public API (host app may enable R8 full mode) ---
-keep class io.closedtest.sdk.ClosedTest { *; }
-keep class io.closedtest.sdk.ClosedTestOptions { *; }

# --- OkHttp (runtime platform / optional TLS stacks) ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# --- kotlinx.serialization (explicit serializers; keep metadata for models used from JSON) ---
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# --- Room (generated _Impl; entities are referenced from DB impl) ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
