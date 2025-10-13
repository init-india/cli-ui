# Keep all Kotlin classes
-keep class kotlin.** { *; }
-keep class org.jetbrains.** { *; }

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Room database
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep encrypted preferences
-keep class androidx.security.** { *; }

# Keep biometric classes
-keep class androidx.biometric.** { *; }

# Keep CLI classes
-keep class com.smartcli.** { *; }

# Generic ProGuard rules
-dontwarn
-ignorewarnings
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
