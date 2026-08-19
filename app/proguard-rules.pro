# Keep ML Kit barcode model classes
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Room generated
-keep class com.kobe.qrbarcode.data.db.** { *; }

# Kotlin metadata
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*Annotations
