# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep model loader and TFLite ops
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Room / Kotlin reflection
-keep class androidx.room.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep our data classes used by Alarm/Receiver via reflection (just in case)
-keep class com.example.voicenotereminder.** { *; }
