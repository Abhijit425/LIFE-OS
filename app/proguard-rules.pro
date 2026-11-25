# kotlinx.serialization (type-safe navigation routes)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.lifeos.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lifeos.app.**$$serializer { *; }

# JSON export reads entity fields reflectively.
-keep class com.lifeos.app.data.local.entity.** { *; }
