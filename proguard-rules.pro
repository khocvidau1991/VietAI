# Keep data classes for Moshi/Room (dùng reflection)
-keep class com.example.data.** { *; }
-keep class com.example.api.** { *; }
-keep class com.example.service.** { *; }
-keep class com.example.speech.** { *; }

# Moshi
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep @com.squareup.moshi.JsonQualifier interface *

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Kotlin stdlib
-dontwarn kotlin.**

# Compose
-dontwarn androidx.compose.**
