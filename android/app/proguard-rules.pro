# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.freellmapi.android.** { *; }