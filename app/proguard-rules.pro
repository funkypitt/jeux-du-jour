# Moshi — keep all data classes used for JSON deserialization
-keep class com.freedomfighter.jeuxdujour.ui.connexions.GroupLoader$PuzzleJson { *; }
-keep class com.freedomfighter.jeuxdujour.ui.connexions.GroupLoader$GroupJson { *; }
-keep class com.freedomfighter.jeuxdujour.ui.home.MotDuJourEntry { *; }

# Moshi — keep @Json annotated fields
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Moshi Kotlin reflection adapter
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
