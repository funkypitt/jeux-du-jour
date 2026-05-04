# Moshi
-keep class com.freedomfighter.jeuxdujour.ui.connexions.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
