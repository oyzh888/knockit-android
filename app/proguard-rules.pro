# Knockit ProGuard / R8 rules
# Add project-specific rules here.

# Keep Room entity classes
-keep class com.knockit.app.data.model.** { *; }

# Keep Gson serialization targets (if any JSON models are added later)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
