# Shiroha Quiz 文档索引

这里整理当前仍建议阅读的文档入口。每个主要文档按主题放在独立文件夹中，Markdown 用于 GitHub 阅读，Word 用于本地查看或分发；有 PDF 的文档会在同一文件夹内同时提供。历史计划和旧方案统一放在 `archive/`，避免和当前主线混在一起。

当前主线：Web 版与原生 Android Compose 版均已加入 WebDAV 云备份与单题 AI 解析追问，原生版已更新到 `v0.9.10-native`，重点能力包括文档识别（MinerU 在线 OCR）、WebDAV 云备份、AI 导入核对 / 补解析、单题 AI 追问和答案区恢复。原生解析器外部回归当前维护 63 个有效 parser 用例，编号已扩展到 64。具体功能状态以根目录 [README](../README.md) 和 [CHANGELOG](../CHANGELOG.md) 为准。

## 目录

- [用户文档](README.md#docs-user)
- [原生 Android](README.md#docs-native)
- [工程与发布](README.md#docs-engineering)
- [计划归档参考](README.md#docs-plan)
- [归档文档](README.md#docs-archive)

<a name="docs-user"></a>

## 用户文档

- 标准题库格式示例：推荐题库格式模板，含文本、Excel、普通 JSON 示例。  
  [Markdown](./标准题库格式示例/标准题库格式示例.md) / [Word](./标准题库格式示例/标准题库格式示例.docx) / [PDF](./标准题库格式示例/标准题库格式示例.pdf)
- 题库导入格式支持说明：支持哪些文件格式、每种格式怎么写。  
  [Markdown](./题库导入格式支持说明/Shiroha_Quiz_题库导入格式支持说明.md) / [Word](./题库导入格式支持说明/Shiroha_Quiz_题库导入格式支持说明.docx)
- 题库导入策略与使用指南：面向普通用户的导入入口、AI 辅助、预览检查和备份恢复说明。<br>
  [Markdown](./题库导入策略与使用指南/Shiroha Quiz 题库导入策略与使用指南.md) / [Word](./题库导入策略与使用指南/Shiroha Quiz 题库导入策略与使用指南.docx)
- 题目导入解析方法说明：解释软件如何识别题号、选项、答案、解析、集中答案区和异常提示。<br>
  [Markdown](./题目导入解析方法说明/Shiroha Quiz 题目导入解析方法说明.md) / [Word](./题目导入解析方法说明/Shiroha Quiz 题目导入解析方法说明.docx)
- WebDAV 云备份使用指南：把题库备份到自己的 WebDAV 云盘，含双端配置、云端恢复与自动备份说明。<br>
  [Markdown](./WebDAV云备份使用指南/Shiroha Quiz WebDAV 云备份使用指南.md)

<a name="docs-native"></a>

## 原生 Android

- [原生开发进度](./native/原生开发进度.md)
- [解析器回归测试说明](./native/解析器回归测试说明.md)
- [测试体系整理说明](./native/测试体系整理说明.md)
- [解析器回归测试剩余关注项](./native/解析器回归测试未覆盖功能点.md)
- [安卓组件与状态说明](./native/安卓组件与状态说明.md)
- [原生 Compose 视觉规范](./native/Shiroha_Quiz_原生Android_Compose视觉规范_v30.md)
- [安卓设计 Token 规范](./native/安卓设计Token规范.md)
- [原生安卓图片素材使用建议](./native/Shiroha Quiz 原生安卓图片素材使用建议.md)

<a name="docs-engineering"></a>

## 工程与发布

- [架构说明](./universal/架构说明.md)
- [WebDAV 同步实现方案](./WebDAV同步实现方案/Shiroha%20Quiz%20WebDAV%20同步实现方案.md)
- [Git Release 操作速查](./universal/Git-Release-操作速查.md)

<a name="docs-plan"></a>

## 计划归档参考

这里收录仍有参考价值的阶段计划，但不代表当前必须按这些旧计划执行。

- [后续功能开发计划 33-34](./archive/native/Shiroha_Quiz_后续功能开发计划33-34.md)

<a name="docs-archive"></a>

## 归档文档

- [历史原生计划](./archive/native/)
- [历史 Web 计划](./archive/web/)
- [历史通用架构建议](./archive/universal/)
- [阶段 32-33 计划归档](./archive/native/Shiroha_Quiz_后续功能开发计划32-33.md)
