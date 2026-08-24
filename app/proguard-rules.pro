# ============================================================
#  KotlinAZ — R8 qaydaları
# ============================================================

-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**

# ---------- kotlinx.serialization ----------
# Serializator sinifləri refleksiya ilə deyil, kompilyator plagini ilə
# yaradılır — amma R8 onların adlarını dəyişməməlidir.

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Tətbiqin öz modelləri (Block sealed iyerarxiyası daxil)
-keep,includedescriptorclasses class az.kotlinaz.app.data.model.**$$serializer { *; }
-keepclassmembers class az.kotlinaz.app.data.model.** {
    *** Companion;
    *** INSTANCE;
}
-keepclasseswithmembers class az.kotlinaz.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class az.kotlinaz.app.data.model.** { *; }

# Kompilyator xidmətinin sorğu/cavab modelləri KotlinCompiler.kt daxilində
# private-dır; @Serializable annotasiyası ilə işarələnən hər şeyi saxlayırıq.
-keep @kotlinx.serialization.Serializable class az.kotlinaz.app.** { *; }
-keepclassmembers class az.kotlinaz.app.data.KotlinCompiler* {
    *** Companion;
    *** INSTANCE;
}
-keep,includedescriptorclasses class az.kotlinaz.app.data.**$$serializer { *; }
-keepclassmembers class az.kotlinaz.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class az.kotlinaz.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Compose ----------
-dontwarn androidx.compose.**

# ---------- Digər ----------
-dontwarn org.slf4j.**
