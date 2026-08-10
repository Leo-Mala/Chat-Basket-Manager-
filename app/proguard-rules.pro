# Basket Manager release rules

# Gson serializes domain snapshots reflectively.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.models.** { *; }

# Room entities/DAOs are generated and referenced through annotations.
-keep class com.example.data.database.** { *; }

# Keep Kotlin metadata used by serializers and diagnostics.
-keep class kotlin.Metadata { *; }

# Preserve source line information for actionable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
