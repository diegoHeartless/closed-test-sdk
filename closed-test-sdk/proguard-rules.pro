# Library release minify is off by default; these rules apply if a consumer enables shrinking on the SDK artifact.
# Prefer consumer-rules.pro for API stability; keep this file aligned with consumer-rules.pro.

-dontwarn okhttp3.internal.platform.**
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
