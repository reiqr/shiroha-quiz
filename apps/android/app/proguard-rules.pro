# Compose — keep runtime classes accessed via reflection
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Keep data classes marked with Compose annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <fields>;
    @androidx.compose.runtime.Stable <fields>;
}

# Navigation arguments
-keep class * extends androidx.navigation.NavArgs { *; }

# App namespace — keep data models that may be serialized
-keep class com.yiqiu.shirohaquiz.state.** { <fields>; }
-keep class com.reqir.shirohaquiz.state.** { <fields>; }

# Keep Serializable/Parcelable used by navigation
-keepclassmembers class * implements java.io.Serializable { *; }
-keepclassmembers class * implements android.os.Parcelable { *; }

# WebView JS 桥接：Kotlin 侧方法通过反射暴露给页面脚本，必须保留名称
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.yiqiu.shirohaquiz.ui.text.MathWebView$Bridge { *; }
