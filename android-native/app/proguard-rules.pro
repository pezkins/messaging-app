# Add project specific ProGuard rules here.

# ============================================
# CRITICAL: Keep generic type information (Signature attribute)
# This prevents "Class cannot be cast to ParameterizedType" errors
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# ============================================
# Gson - Keep ALL type information for serialization
# ============================================
-dontwarn sun.misc.**

# Keep Gson and all its internal classes
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# Keep TypeToken and its subclasses (CRITICAL for generic types)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep all TypeAdapter implementations
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from stripping interface information
-keep,allowobfuscation,allowshrinking class com.google.gson.TypeAdapter
-keep,allowobfuscation,allowshrinking class com.google.gson.TypeAdapterFactory
-keep,allowobfuscation,allowshrinking class com.google.gson.JsonSerializer
-keep,allowobfuscation,allowshrinking class com.google.gson.JsonDeserializer

# Keep Gson's internal reflect package - this is where TypeToken lives
-keep class com.google.gson.reflect.** { *; }
-keep class com.google.gson.internal.** { *; }

# ============================================
# App Data Models - Keep ALL fields and type info for serialization
# ============================================
-keep class com.intokapp.app.data.models.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.intokapp.app.data.models.** {
    <fields>;
    <init>(...);
}

-keep class com.intokapp.app.data.network.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.intokapp.app.data.network.** {
    <fields>;
    <init>(...);
}

# ============================================
# Retrofit - CRITICAL: Keep method signatures with generics
# ============================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Keep all runtime annotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep methods with HTTP annotations and their return types
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit's internal classes for response handling
-keep class retrofit2.Response { *; }
-keep class retrofit2.Invocation { *; }

# Keep OkHttp's Response body type resolution
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Retrofit Converter - Keep Gson converter factory
-keep class retrofit2.converter.gson.** { *; }

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
# Google Sign-In & Credentials API
# ============================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# AndroidX Credentials API - Prevents ParameterizedType cast errors
-keep class androidx.credentials.** { *; }
-keep class androidx.credentials.playservices.** { *; }
-keep interface androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# Google Identity library - Critical for Google Sign-In
-keep class com.google.android.libraries.identity.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.**

# Keep all classes that extend/implement ParameterizedType
-keep class * implements java.lang.reflect.ParameterizedType { *; }

# Keep all credential-related result types
-keep class * extends androidx.credentials.Credential { *; }
-keep class * extends androidx.credentials.CredentialOption { *; }

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

# Keep Kotlin reflect for runtime type checks
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ============================================
# Java Reflection - Prevent ClassCastException
# ============================================
-keep class java.lang.reflect.** { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}
