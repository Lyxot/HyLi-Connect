# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

-keep class xyz.hyli.connect.bean.MotionEventBean { *; }

-keep class xyz.hyli.connect.hook.HookFramework { *; }
-keep class xyz.hyli.connect.hook.HookMyself { *; }
-keep class xyz.hyli.connect.hook.HookSystemUI { *; }
-keep class xyz.hyli.connect.hook.utils.HookShellUtils { *; }
-keep class xyz.hyli.connect.hook.** { *; }

-keep class * implements android.os.IInterface {*;}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class * implements java.io.Serializable {*;}
-keepclassmembers class * implements android.os.Parcelable {
    public <fields>;
    private <fields>;
}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}