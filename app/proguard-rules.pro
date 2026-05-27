# Add project specific ProGuard rules here.

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers class ** {
    @retrofit2.http.* <methods>;
}

# Keep Gson models
-keep class com.example.artranslator.core.translation.** { *; }

# Keep Room entities
-keep class com.example.artranslator.core.database.entity.** { *; }

# Keep Hilt generated code
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Keep CameraX
-keep class androidx.camera.** { *; }
