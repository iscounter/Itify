# Keep extractor and Rhino pieces used reflectively.
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-keepclassmembers class * implements android.os.Parcelable { public static final ** CREATOR; }
