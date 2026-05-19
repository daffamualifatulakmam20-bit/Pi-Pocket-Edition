# ProGuard rules for Pi-Pocket Edition

# Keep libsu
-keep class com.topjohnwu.superuser.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Compose
-dontwarn androidx.compose.**
