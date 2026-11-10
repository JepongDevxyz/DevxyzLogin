# DevxyzLogin ProGuard rules (release minification is disabled in v1).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.devxyz.login.model.** { *; }
