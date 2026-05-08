# ProGuard rules for DiaryMind
-keep class com.diarymind.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
