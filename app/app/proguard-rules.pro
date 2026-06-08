# Proguard rules for HitungUang Application

# --- General Android ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# --- Kotlinx Serialization ---
# Keep serializable classes and their generated serializer helpers
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# --- Room Database ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep interface * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.RoomOpenHelper

# --- Dagger Hilt ---
# Keep Hilt custom entry points and DI injected classes
-keep @dagger.hilt.EntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# --- Domain & Database Models ---
# Keep domain models and entities intact to prevent issues with Reflection/Room mapping
-keep class com.hitunguang.feature.**.domain.model.** { *; }
-keep class com.hitunguang.core.database.entity.** { *; }

# --- Jetpack Compose ---
# Keep Compose functions and UI previews
-keepclassmembers class * {
    @androidx.compose.runtime.Composable class *;
    @androidx.compose.runtime.Composable *** *(...);
}
