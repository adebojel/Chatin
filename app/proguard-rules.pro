# =====================================================================
# CHATIN ENTERPRISE PROGUARD & R8 PRODUCTION RULES
# =====================================================================

# 1. Android Room Database & SQLite
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.Entity
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase {
    public abstract <methods>;
}
-keep class **_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static final ** Companion;
}

# 2. Android Biometric Authentication (androidx.biometric)
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**
-keepclassmembers class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback {
    public void onAuthenticationSucceeded(androidx.biometric.BiometricPrompt$AuthenticationResult);
    public void onAuthenticationError(int, java.lang.CharSequence);
    public void onAuthenticationFailed();
}

# 3. Model Data & JSON Serialization (Moshi / Kotlin Data Classes)
-keepclassmembers class com.example.data.models.** { *; }
-keep class com.example.data.models.** { *; }
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonQualifier <fields>;
}

# 4. Java Cryptography Architecture (JCA / ECDH / AES-GCM)
-keepclassmembers class com.example.util.CryptoHelper { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# 5. Socket.io Client & Engine.io WebSocket
-keep class io.socket.** { *; }
-keep class io.socket.client.** { *; }
-keep class io.socket.engineio.client.** { *; }
-dontwarn io.socket.**
-keepclassmembers class io.socket.client.Socket {
    public io.socket.client.Socket on(java.lang.String, io.socket.emitter.Emitter$Listener);
    public io.socket.client.Socket emit(java.lang.String, java.lang.Object[]);
    public io.socket.client.Socket connect();
    public io.socket.client.Socket disconnect();
}

# 6. OkHttp & Retrofit Networking
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# 7. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }

# 8. Google Gemini Generative AI SDK & Firebase
-keep class com.google.firebase.ai.** { *; }
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Preserve source file & line numbers for stack trace debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
