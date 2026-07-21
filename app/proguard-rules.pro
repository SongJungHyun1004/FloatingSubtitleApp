# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Hilt ---
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keep class * extends dagger.hilt.android.GeneratedComponentHolder { *; }
-keep @interface dagger.hilt.android.qualifiers.* { *; }
-keepattributes *Annotation*

# --- Kotlin & Reflections ---
-keepattributes kotlin.Metadata
-keepattributes *Annotation*

# --- Coroutines ---
-keep class kotlinx.coroutines.** { *; }

# --- Compose ---
-keep class androidx.compose.** { *; }

# --- Vosk (offline speech recognition) ---
-keep class org.vosk.** { *; }

# --- ML Kit Translation ---
-keep class com.google.mlkit.** { *; }

# Keep any custom @Keep annotations if used
-keep @interface *Keep* { *; }
-keep @*Keep* class * { *; }
-keepclassmembers class * {
    @*Keep* *;
}
-keepclassmembers class * {
    @*Keep* <methods>;
}
-keepclassmembers class * {
    @*Keep* <fields>;
}
