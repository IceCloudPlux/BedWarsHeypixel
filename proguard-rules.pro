# =============================================================
# BedWars 强混淆配置 (ProGuard 7.9.1)
# 策略: 不收缩、不优化, 仅做重命名混淆 + 调试信息剥离
# =============================================================

-injars  'c:/Users/Admin/Desktop/BedWars/BedWars1058-master/bedwars-plugin/target/bedwars-plugin-26.7.jar'
-outjars 'c:/Users/Admin/Desktop/BedWars/BedWars1058-master/bedwars-plugin/target/bedwars-plugin-26.7-obfuscated.jar'

# 库 jar: 用于解析 Bukkit/Spigot/NMS 符号, 保证实现库接口的方法不被改名,
# 并让预验证能正确计算 StackMapTable 帧
-libraryjars 'c:/Users/Admin/.m2/repository/org/spigotmc/spigot-api/1.20.4-R0.1-SNAPSHOT/spigot-api-1.20.4-R0.1-20240423.154232-27.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.12.2-R0.1-SNAPSHOT/spigot-1.12.2-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.14.4-R0.1-SNAPSHOT/spigot-1.14.4-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.16.5-R0.1-SNAPSHOT/spigot-1.16.5-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.17.1-R0.1-SNAPSHOT/spigot-1.17.1-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.18.2-R0.1-SNAPSHOT/spigot-1.18.2-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.19.3-R0.1-SNAPSHOT/spigot-1.19.3-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.19.4-R0.1-SNAPSHOT/spigot-1.19.4-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.20.1-R0.1-SNAPSHOT/spigot-1.20.1-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.20.2-R0.1-SNAPSHOT/spigot-1.20.2-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.20.3-R0.1-SNAPSHOT/spigot-1.20.3-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.20.4-R0.1-SNAPSHOT/spigot-1.20.4-R0.1-SNAPSHOT.jar'
-libraryjars 'C:/Users/Admin/.m2/repository/org/spigotmc/spigot/1.8.8-R0.1-SNAPSHOT/spigot-1.8.8-R0.1-SNAPSHOT.jar'

-dontshrink
-dontoptimize
# 可选依赖(javassist/micrometer等)层级不完整导致预验证失败;
# 由后续 ASM 步骤(FixFrames)重新计算 StackMapTable 帧
-dontpreverify
-dontusemixedcaseclassnames
-allowaccessmodification
-adaptclassstrings

# 强混淆: 把可重命名类全部折叠进短包名 a
-repackageclasses 'a'

# 保留运行时需要的属性(注解对 Bukkit 事件注册至关重要)
-keepattributes Exceptions,InnerClasses,EnclosingMethod,Signature,*Annotation*

# ---------- 入口点 / 反射敏感区(必须保留原名) ----------

# 主类: Bukkit 反射调用 onLoad/onEnable/onDisable
-keep class com.andrei1058.bedwars.BedWars { *; }

# 公开 API: 外部插件通过 Bukkit ServicesManager 使用
-keep class com.andrei1058.bedwars.api.** { *; }

# 版本支持: BedWars.onLoad 通过 Class.forName("...support.version.X.X") 反射加载
-keep class com.andrei1058.bedwars.support.** { *; }

# relocated 第三方库(HikariCP/slf4j/sidebar/bstats): 内部反射代理与 ServiceLoader
-keep class com.andrei1058.bedwars.libs.** { *; }

# vipfeatures 公开 API 与 PaperLib
-keep class com.andrei1058.vipfeatures.** { *; }
-keep class io.papermc.** { *; }

# 保留所有 @EventHandler 方法(注解驱动的 Bukkit 事件)
-keepclassmembers class * {
    @org.bukkit.event.EventHandler *;
}

# 忽略无法解析的外部类(NMS 多版本等)警告
-dontwarn
