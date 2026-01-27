# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ============================================
# Keep BuildConfig
# ============================================
-keepclassmembers class **.BuildConfig {
    public static <fields>;
}

# ============================================
# Keep custom model classes for JSON parsing
# ============================================
-keep class why.xee.kidsview.data.model.** { *; }
-keepclassmembers class why.xee.kidsview.data.model.** {
    <fields>;
}

# ============================================
# Retrofit & Gson
# ============================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit does reflection on generic parameters
-keepattributes Signature, Exceptions
-keepattributes *Annotation*

# Keep Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep all Retrofit service interfaces and their methods
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson specific classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson uses generic type information stored in a class file when working with fields
-keepattributes Signature

# Gson may be used to serialize/deserialize generic types
-keepattributes Signature

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================
# OkHttp
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ============================================
# Hilt / Dagger
# ============================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# Keep all Hilt entry points
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# Keep ViewModel classes that Hilt injects
-keep class why.xee.kidsview.ui.viewmodel.** { *; }

# ============================================
# Jetpack Compose
# ============================================
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep,includedescriptorclasses class androidx.compose.ui.platform.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime
-keepclassmembers class androidx.compose.runtime.** {
    *;
}

# ============================================
# YouTube Player
# ============================================
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-dontwarn com.pierfrancescosoffritti.androidyoutubeplayer.**

# ============================================
# AdMob / Google Play Services
# ============================================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep AdMob classes
-keep public class com.google.android.gms.ads.**{
   public *;
}
-keep public class com.google.ads.**{
   public *;
}

# Keep AdManager class (important for alpha builds)
-keep class why.xee.kidsview.utils.AdManager { *; }
-keepclassmembers class why.xee.kidsview.utils.AdManager {
    *;
}

# ============================================
# Firebase / Firestore
# ============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore
-keepattributes Signature
-keepattributes Exceptions
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class com.google.firebase.firestore.** { *; }

# ============================================
# Room Database
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Room annotation processors
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# ============================================
# Coil Image Loading
# ============================================
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ============================================
# Lottie
# ============================================
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ============================================
# Kotlin
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ============================================
# Keep native methods
# ============================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# Keep Parcelable implementations
# ============================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================
# Keep Serializable classes
# ============================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# Keep R class
# ============================================
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ============================================
# Preserve line numbers for debugging
# ============================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# Keep annotations
# ============================================
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ============================================
# Remove logging in release builds (optional)
# ============================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
