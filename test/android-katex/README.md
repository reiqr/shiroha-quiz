# PR #147 Android 公式引擎对照

从同一提交、同一个 Gradle 8.7 / JDK 17 环境构建。两包使用 Release 的 R8 和资源收缩配置，仅用调试钥匙签名；不能用来发布正式版。

~~~powershell
# 在 apps/android 下，依赖已缓存时使用 --offline
.\gradlew.bat --offline :app:assembleNativeRelease -PlatexComparison=ratex
.\gradlew.bat --offline :app:assembleNativeRelease -PlatexComparison=katex
~~~

两份 APK 都保留在 `apps/android/app/build/outputs/shiroha-quiz/`，包名分别为 `com.reqir.shirohaquiz.ratex.test` 和 `com.reqir.shirohaquiz.katex.test`。用 `adb install -r <APK>` 安装；不要用正式数据做测试。未设置该参数时仍是 RaTeX 正式构建；`-PnoRatex` 仍然单独构建 Lite。

## 对照流程

1. 从已提供的 `Shiroha_Quiz_LaTeX_TestBank_20261122.zip` 解压 JSON（不要把 ZIP 当成备份导入）。两包分别导入相同的文件：先用 08、01、03、04 查渲染和回退，再用 05 查题型和交互；记录问题所在题库、题号、公式原文、期望和截图。核对深色、字号、旋转、横滑、长按复制、TalkBack 和触控笔切题/保存恢复。04 中故意无效的公式应能读到原文。
2. 在 K50 级中端机和旗舰机都运行 07：关闭乱序和自动跳题，采用相同字号、题序和切题节奏。两包交替各测三轮，轮次之间关闭对应测试包，先记录首题冷态显示，再记录 50 题切换；每轮前在练习设置页记 PSS 基线并重置帧统计，切题时等频采样，退出后在 8 秒和 30 秒采样。06 是额外压力场景，不与 07 的重复题混算兼容性。
3. 每轮记录设备/Android/WebView 版本、APK SHA256 与体积、首题和切题从操作到公式稳定可见的耗时、慢帧数/总帧数、峰值 PSS 减基线、退出后 PSS，并取三轮中位数。用 `adb shell dumpsys gfxinfo <包名> reset` 重置、`adb shell dumpsys gfxinfo <包名>` 读取帧数；用 `adb shell dumpsys meminfo <PID>` 按 PID 采样，KaTeX 要计入确认归属的 WebView 渲染进程、不重复求和。未能确认的进程或无法测出的指标标明缺测，不当成 0。

核心公式和交互都通过、50 题稳定、PSS 增量 < 200 MiB 且慢帧 < 3% 才合格。仅一个合格就选它；两者都合格时，只有 KaTeX 修复了可证实的实际题库排版差异才换引擎，否则保留 RaTeX。缺真机数据不定版。

## 自动检查

- `node test/android-katex/math-page.test.cjs`：检查页面协议、失效回退和实际内置 KaTeX 的代表性公式，不替代 Android 实机。
- 在 `apps/android` 分别以两个 `latexComparison` 参数运行 `:app:testNativeDebugUnitTest`。共用公式切分测试随两包运行，KaTeX 另有 WebView/Compose 交互测试。

KaTeX 资源取自官方 [v0.19.1 发布包](https://github.com/KaTeX/KaTeX/releases/tag/v0.19.1)，`katex.zip` SHA256：`53fc803b117c9ccf625e232e5ce2a6571d39ca87d9345917912e0b7d08839f9e`。仅打包渲染所需的 JS、CSS 和 WOFF2 字体；运行时不从网络下载。
