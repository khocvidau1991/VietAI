# ============== Keep app classes ==============
-keep class com.example.MainActivity { *; }
-keep class com.example.AppState { *; }
-keep class com.example.PipHelper { *; }

# ============== Moshi ==============
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepnames @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class kotlin.Metadata { public <methods>; }

# ============== Retrofit ==============
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.moshi.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ============== OkHttp ==============
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============== Room ==============
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============== Compose ==============
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ============== Kotlin Coroutines ==============
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ============== Media3 ==============
-dontwarn androidx.media3.**

# ============== Strip log calls in release ==============
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ============== Strip Kotlin metadata ==============
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# ============== Keep enum values ==============
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============== Giữ generic signature (cho Moshi/Retrofit khi minify) ==============
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ============== Moshi Kotlin (nếu dùng KotlinJsonAdapterFactory) ==============
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# ============== Moshi Types (tránh lỗi ParameterizedType) ==============
-keep class com.squareup.moshi.Types { *; }
-keep class com.squareup.moshi.internal.** { *; }
-dontwarn com.squareup.moshi.**
