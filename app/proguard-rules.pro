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
# --- R8 (A6) ---
# Libraries bring their own rules (kotlinx-serialization, Room, WorkManager, Coil, Compose, Koin needs
# none); AGP's default file keeps JNI methods and @JavascriptInterface methods.
# :adblock-client's native code looks MatchResult up by name (FindClass) and calls this constructor;
# its consumer rule only keeps the constructor, so keep the class name too.
-keep class io.github.edsuns.adblockclient.MatchResult {
    <init>(boolean, java.lang.String, java.lang.String);
}
