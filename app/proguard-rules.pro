# Keep readable crash reports.
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# Room, Compose, Navigation, Lifecycle and Biometric ship their own consumer rules.
# This app uses no reflection or serialization libraries of its own.
-dontwarn org.jetbrains.annotations.**
