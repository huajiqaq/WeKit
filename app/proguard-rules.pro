# ==========================================================
# Global Attributes & Basics (通用设置)
# ==========================================================
# 保留泛型、注解、行号
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,LineNumberTable,SourceFile,*Annotation*

# 保持所有 Native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保持 Parcelable 实现
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# 保持枚举类的标准方法
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==========================================================
# App Specific
# ==========================================================
-keep class moe.ouom.wekit.** { *; }
# 强制保留 Kotlin 标准库

# 无论主 DEX 用没用，都留着给 Hidden DEX 用
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class org.intellij.lang.annotations.** { *; }
-keep class org.jetbrains.annotations.** { *; }

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler {
    <init>(...);
}

# ==========================================================
# Xposed & LSPosed
# ==========================================================

-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
    public void onPackageLoaded(...);
    public void onSystemServerLoaded(...);
}

-keep,allowoptimization,allowobfuscation @io.github.libxposed.api.annotations.* class * {
    @io.github.libxposed.api.annotations.BeforeInvocation <methods>;
    @io.github.libxposed.api.annotations.AfterInvocation <methods>;
}

# 忽略相关警告
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.api.**
# 忽略被抽离隐藏的 hooks 包，它们会在运行时通过内存加载
-dontwarn moe.ouom.wekit.hooks.**

# ==========================================================
# Jetpack Compose
# ==========================================================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 保持 R 文件字段（有时在混淆资源ID时需要）
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ==========================================================
# Material Design & AndroidX
# ==========================================================
-keep class com.google.android.material.internal.** { *; }
-keep public class com.google.android.material.internal.CheckableImageButton { *; }
-dontwarn com.google.android.material.**
-dontwarn androidx.**

# ==========================================================
# Serialization (Gson & Kotlinx)
# ==========================================================
# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.Gson { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Protobuf
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# ==========================================================
# Network
# ==========================================================
-keepattributes *Annotation*
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ==========================================================
# Third Party Libs
# ==========================================================
-keep class com.android.dx.** { *; }
-keep class net.bytebuddy.** { *; }
-dontwarn com.sun.jna.**

# ==========================================================
# Side Effects & Optimizations
# ==========================================================

-keep class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(java.lang.Object, java.lang.String);
    public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
    *;
}

# 移除 Objects.requireNonNull
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# 忽略 ByteBuddy 和 Mocking 相关的类
-dontwarn net.bytebuddy.**
-dontwarn java.lang.instrument.**
-dontwarn org.mockito.**

# 忽略 FindBugs/SpotBugs 注解
-dontwarn edu.umd.cs.findbugs.**
-dontwarn javax.annotation.**

# 忽略 KotlinPoet 和 Java Model 类
-dontwarn com.squareup.kotlinpoet.**
-dontwarn javax.lang.model.**

# 忽略 ServiceProvider 相关的 OSGi 注解
-dontwarn aQute.bnd.annotation.spi.**

# 修复 Kotlin Experimental 报错 #
# 忽略旧版 Kotlin 注解缺失的警告
-dontwarn kotlin.Experimental
-dontwarn kotlin.Experimental$Level

# 针对报错中提到的 kotlinx.io 相关的忽略
-dontwarn kotlinx.io.**

# 修复 Stax2 XML 相关的警告 #
-dontwarn org.codehaus.stax2.**

# ==========================================================
# DexKit
# ==========================================================
# 保留 DexKit 核心类和方法
-keep class org.luckypray.dexkit.** { *; }
-keepclassmembers class org.luckypray.dexkit.** { *; }

# 保留 DexKit 的 JNI 方法
-keepclasseswithmembernames class org.luckypray.dexkit.** {
    native <methods>;
}

# 保留 DexKit 使用的反射相关类
-keep class * implements org.luckypray.dexkit.** { *; }

# 忽略 DexKit 警告
-dontwarn org.luckypray.dexkit.**

# ==========================================================
# Build Behavior
# ==========================================================
-dontoptimize
-dontobfuscate