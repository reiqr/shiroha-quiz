# Shiroha Quiz

Shiroha Quiz 是一个轻量、开源、可扩展的通用刷题项目，面向多题库、多题型和多端使用场景。

它当前的目标不是只做一个网页工具，而是逐步发展成一套完整的刷题系统：

- Web 端：用于题库导入、题库管理、格式验证和能力迭代
- Android 端：用于原生、高质感、重体验的刷题与考试
- 共享层：后续沉淀题库解析、刷题核心逻辑和通用数据结构

## 项目定位

Shiroha Quiz 希望提供一个稳定、通用、可持续扩展的刷题环境，支持：

- 多题库管理
- 题库导入
- 题型识别
- 刷题练习
- 错题复习
- 多端扩展

当前项目仍处于早期测试与快速迭代阶段。

## 当前能力

- 支持多题库管理
- 支持题库导入与解析
- 支持单选、多选、判断等常见题型
- 支持刷题练习流程
- 支持错题复习规划
- 支持 Web 端完整功能
- Android 端通过 WebView 壳运行（过渡方案）
- 采用 monorepo 结构，Web 和 Android 共享核心代码

## 当前状态

目前已经完成：

- [x] 初始 monorepo 仓库结构整理
- [x] 通用 Web 工程收口到 `apps/web`
- [x] Android 原生工程收口到 `apps/android`
- [x] 中文产品、UI、线框、组件、执行计划文档整理完成
- [x] Android Compose 导航壳和首批原型页面可编译通过
- [x] Android WebView 壳方案落地，直接加载 Web 端内容
- [x] 首次公开版本本地提交完成
- [ ] 持续完善题库导入与解析逻辑
- [ ] 持续完善刷题练习体验
- [ ] 完善错题、记录与考试流程
- [ ] 增加更多示例题库与展示素材

## 支持的导入方向

当前计划支持：

- `txt`
- `json`
- `csv`
- `docx`
- `doc`
- 文字层 `pdf`
- 双文件导入

当前阶段暂不做 OCR PDF。

## 默认示例题库

当前默认示例题库为 `C1` 驾考题库，用于：

- 导入逻辑验证
- 默认演示
- 功能测试

相关说明见：

- [docs/web/C1题库来源与说明.md](./docs/web/C1题库来源与说明.md)

## 仓库结构

```text
shiroha-quiz/
├─ apps/
│  ├─ web/        # 通用 Web 参考实现
│  └─ android/    # Android 原生工程
├─ packages/
│  ├─ core/       # 规划中的刷题核心逻辑
│  ├─ parser/     # 规划中的题库解析逻辑
│  ├─ types/      # 规划中的通用数据结构
│  ├─ shared/     # 规划中的通用工具
│  └─ ui/         # 规划中的通用 UI 规范
├─ docs/          # 产品、设计、架构、执行计划文档
├─ assets/        # 图标、截图、设计素材
├─ package.json
├─ pnpm-workspace.yaml
├─ CHANGELOG.md
├─ CONTRIBUTING.md
├─ LICENSE
└─ README.md
```

## Web 端运行方式

安装依赖：

```bash
pnpm install
```

启动开发环境：

```bash
pnpm dev
```

如果只运行 Web 端：

```bash
pnpm --filter web dev
```

## Web 端说明

`apps/web` 当前是通用网页版参考实现，主要承担：

- 题库导入能力验证
- 数据结构参考
- 题型识别规则验证
- 同时作为 Android WebView 壳的内容来源

## Android 端状态

`apps/android` 当前采用 WebView 壳方案作为过渡：

- 启动入口为 `WebShellActivity`，直接加载内置的 Web 端页面
- Web 资源（HTML / JS / CSS）存放在 `assets/web/` 目录下
- 无需联网，本地即可运行

这是过渡方案，后续会逐步替换为原生页面：

- 优势：Web 端和 Android 端共用同一套核心代码，迭代快
- 后续：Web 功能稳定后，逐步用 Compose 原生页面替换关键流程

## 开发路线

当前推荐路线：

1. 持续完善 Web 端刷题流程（WebView 壳同步受益）
2. 稳定题库导入与解析规则
3. 完善错题、记录与考试功能
4. Web 端稳定后，逐步推进 Android 原生页面替换
5. 接入导出能力和更多题库格式

## 下载与使用

当前仓库已完成第一次公开整理，但仍处于早期阶段。

GitHub Releases 页面：

- [https://github.com/reiqr/shiroha-quiz/releases](https://github.com/reiqr/shiroha-quiz/releases)

当前发布内容通常包括：

- Android APK
- Web 构建产物
- 示例题库
- 源码压缩包

当前版本不建议直接用于正式考试场景。

## 参与贡献

欢迎通过 Issue 提交：

- Bug 反馈
- 功能建议
- 题库格式兼容问题
- UI / 交互优化建议
- 文档改进建议

详细说明见：

- [CONTRIBUTING.md](./CONTRIBUTING.md)

## 更新日志

- [CHANGELOG.md](./CHANGELOG.md)

## 许可证

本项目采用 `GPL-3.0` 开源。

你可以自由使用、学习、修改和分发本项目代码；如果你分发修改后的版本或基于本项目的衍生作品，需要继续遵守 GPL-3.0 的相关要求。
