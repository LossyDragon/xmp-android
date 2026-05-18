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

-keep public class org.helllabs.libxmp.model.AudioStats {
    public *;
}

-keep public class org.helllabs.libxmp.model.ChannelInfo {
    public *;
}


-keep public class org.helllabs.libxmp.model.FrameInfo {
    public *;
}


-keep public class org.helllabs.libxmp.model.ModInfo {
    public *;
}

-keep public class org.helllabs.libxmp.model.ModVars {
    public *;
}

-keep public class * extends android.app.Application

# Keep source file and line number information in crash reports
-renamesourcefileattribute MyApplication
-keepattributes SourceFile,LineNumberTable

# Open source app, no need for class obfuscation. (Bypasses other proguard rules for libs)
-keepnames class ** { *; }
