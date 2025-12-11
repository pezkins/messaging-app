# Add project specific ProGuard rules here.

# ============================================
# Gson - Keep type information for serialization
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep,allowobfuscation,allowshrinking class com.google.gson.TypeAdapter
-keep,allowobfuscation,allowshrinking class com.google.gson.TypeAdapterFactory
-keep,allowobfuscation,allowshrinking class com.google.gson.JsonSerializer
-keep,allowobfuscation,allowshrinking class com.google.gson.JsonDeserializer

# ============================================
# App Data Models - Keep all fields for serialization
# ============================================
-keep class com.intokapp.app.data.models.** { *; }
-keep class com.intokapp.app.data.network.** { *; }

# ============================================
# Retrofit
# ============================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ============================================
# OkHttp
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# ============================================
# Kotlin Coroutines
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================
# Google Sign-In
# ============================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================
# Firebase
# ============================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ============================================
# Keep generic type information for Kotlin
# ============================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
