# Methods the web pages call through addJavascriptInterface are found by name at runtime.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes SourceFile,LineNumberTable,JavascriptInterface
-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
