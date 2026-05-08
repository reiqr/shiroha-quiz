# Shiroha Quiz 仓库优化修改清单（GPL-3.0 版）

## 一、当前项目状态

项目名称：**Shiroha Quiz**

GitHub 仓库名：

```text
shiroha-quiz
```

当前项目已经完成：

- [x] GitHub 仓库创建
- [x] 第一次代码上传
- [x] 第一次测试版 Release 发布
- [x] 初步采用 monorepo 项目结构
- [x] 已规划 Web 端、Android 端、packages 共享模块、docs 文档目录
- [x] 项目定位为通用刷题工具
- [x] 计划采用 GPL-3.0 开源许可证

当前项目已经进入开源初始阶段，后续重点应从“创建仓库”转向：

- README 首页完善
- 版本号规范
- Release 说明优化
- GPL-3.0 许可证确认
- CHANGELOG 补充
- 截图 / 宣传图 / 示例题库补充
- 用户下载和运行路径说明

---

## 二、项目命名统一

目前项目中可能存在几种写法：

```text
shiroha_quiz
shiroha-quiz
Shiroha Quiz
```

建议统一规则如下：

| 场景          | 推荐写法         |
| ----------- | ------------ |
| 项目展示名       | Shiroha Quiz |
| GitHub 仓库名  | shiroha-quiz |
| 代码包名 / 内部标识 | shiroha_quiz |

需要检查的位置：

- [ ] README.md 标题
- [ ] README.md 项目描述
- [ ] README.md 项目结构示例
- [ ] package.json 中的 name 字段
- [ ] docs 文档中的项目名称
- [ ] Release 标题和说明
- [ ] Android 工程中的应用名
- [ ] Web 页面标题
- [ ] 图标、宣传图、截图中的文字

建议 README 标题统一为：

```md
# Shiroha Quiz
```

项目目录示例建议统一为：

```text
shiroha-quiz/
├─ apps/
├─ packages/
├─ docs/
├─ README.md
├─ LICENSE
├─ CHANGELOG.md
└─ package.json
```

---

## 三、项目定位文案

建议将项目定位统一为：

> Shiroha Quiz 是一个轻量、开源、可扩展的通用刷题工具，支持多题库管理、题库导入、题型识别、刷题练习与错题复习，并计划同时支持 Web 与 Android。

README 开头可以改成：

```md
# Shiroha Quiz

Shiroha Quiz 是一个轻量、开源、可扩展的通用刷题工具，面向多题库、多题型和多端使用场景。

项目目标是提供一个简单易用的刷题环境，支持题库导入、题型识别、练习刷题、错题复习与后续移动端扩展。

当前项目仍处于早期测试阶段，功能和界面会持续迭代。
```

---

## 四、README 推荐结构

建议将 README.md 调整为下面的结构：

```md
# Shiroha Quiz

一个轻量、开源、可扩展的通用刷题工具，支持多题库管理、题库导入、题型识别、刷题练习与错题复习，并计划同时支持 Web 与 Android。

## 项目简介

## 功能特性

## 当前状态

## 项目截图

## 下载使用

## Web 端运行方式

## Android 端状态

## 支持的题库格式

## 示例题库

## 项目结构

## 开发路线

## 参与贡献

## 更新日志

## 许可证
```

---

## 五、README 功能特性写法

建议写成：

```md
## 功能特性

- 支持多题库管理
- 支持题库导入与解析
- 支持题型识别
- 支持选择题、判断题等常见题型
- 支持刷题练习流程
- 支持错题复习规划
- 支持 Web 端使用
- 规划 Android 原生客户端
- 采用 monorepo 结构，便于多端共享核心逻辑
```

如果部分功能尚未完成，可以改成：

```md
## 当前状态

- [x] 初始化 monorepo 项目结构
- [x] 收口 Web 端工程
- [x] 收口 Android 工程
- [x] 整理产品与设计文档
- [x] 发布首次测试版 Release
- [ ] 完善题库解析能力
- [ ] 完善刷题流程
- [ ] 完善错题系统
- [ ] 增加示例题库
- [ ] 完善 Android 端体验
- [ ] 发布稳定版
```

---

## 六、下载使用部分

README 中建议新增：

```md
## 下载使用

当前测试版本可在 GitHub Releases 页面下载。

> 注意：当前版本仍处于早期测试阶段，部分功能可能尚未完成，不建议用于正式考试场景。
```

后续如果有 Android APK，可以写成：

```md
## 下载使用

你可以在 GitHub Releases 页面下载最新测试版本。

当前可下载内容可能包括：

- Android APK 安装包
- Web 端构建产物
- 示例题库
- 源码压缩包

> 当前版本仍处于早期测试阶段，功能和界面可能会频繁调整。
```

---

## 七、Web 端运行方式

根据实际项目命令调整。可以先写成：

```md
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

> 具体命令请以项目实际 `package.json` 为准。

```
如果你的 package 名不是 `web`，后面需要改成实际名称。

---

## 八、Android 端状态

建议 README 增加：

```md
## Android 端状态

Android 端目前处于早期开发阶段，主要用于验证原生客户端结构、页面导航和后续移动端刷题体验。

当前重点：

- 搭建 Android 工程结构
- 验证移动端页面导航
- 规划移动端刷题流程
- 后续接入共享题库解析逻辑
- 后续支持 APK 测试版发布
```

如果暂时不想强调 Android，可以写得更保守：

```md
## Android 端状态

Android 端仍处于规划和早期验证阶段，当前项目优先完善 Web 端和核心题库解析能力。
```

---

## 九、项目结构说明

README 中建议保留并优化项目结构：

```md
## 项目结构

```text
shiroha-quiz/
├─ apps/
│  ├─ web/              # Web 端
│  └─ android/          # Android 原生端
│
├─ packages/
│  ├─ core/             # 核心刷题逻辑
│  ├─ parser/           # 题库解析逻辑
│  ├─ types/            # 通用类型定义
│  ├─ shared/           # 多端共享工具
│  └─ ui/               # 通用 UI 组件
│
├─ docs/                # 产品文档、设计文档、开发计划
├─ README.md
├─ CHANGELOG.md
├─ LICENSE
├─ package.json
└─ pnpm-workspace.yaml
```

```
注意：如果实际目录还没有完全建好，不要写成已完成，可以写成“规划结构”。

---

## 十、版本号规范

当前测试版使用了类似：

```text
v1.0.0-test
```

这个命名容易让人误解为正式稳定版。

后续建议使用：

```text
v0.1.0-alpha
v0.1.1-alpha
v0.2.0-alpha
v0.3.0-beta
v1.0.0
```

推荐版本路线：

| 版本           | 含义                     |
| ------------ | ---------------------- |
| v0.1.0-alpha | 首次公开测试版                |
| v0.1.1-alpha | 修复首次测试问题               |
| v0.2.0-alpha | 增加题库导入增强               |
| v0.3.0-beta  | Web / Android 基础功能基本可用 |
| v1.0.0       | 第一个正式稳定版               |

处理建议：

- [ ] 已发布的 `v1.0.0-test` 不必强行删除
- [ ] 后续新版本从 `v0.1.0-alpha` 或 `v0.2.0-alpha` 开始规范
- [ ] Release 标题避免同时出现“正式版”和“test”
- [ ] 正式稳定前不要使用 `v1.0.0`

---

## 十一、Release 说明模板

后续 Release 可以按这个模板写：

```md
# Shiroha Quiz v0.1.0-alpha

这是 Shiroha Quiz 的首次公开测试版本，主要用于验证项目结构、基础刷题流程、题库导入方向和多端工程组织方式。

## 当前包含

- 初始化 monorepo 项目结构
- 收口 Web 端工程
- 收口 Android 工程
- 新增 docs 文档目录
- 新增 packages 共享模块规划
- 整理产品说明与开发计划
- 发布首次公开测试版本

## 注意事项

- 当前版本仍处于早期测试阶段
- 部分功能尚未完成
- 不建议用于正式考试场景
- 后续版本会继续完善题库解析、刷题流程和错题系统

## 下一步计划

- 完善题库导入与解析
- 优化判断题、选择题、多选题识别策略
- 增加示例题库
- 完善 Web 端刷题体验
- 推进 Android 端基础功能
```

如果沿用当前测试版，可以改成：

```md
# Shiroha Quiz v1.0.0-test

这是 Shiroha Quiz 的首次发布测试版本，主要用于验证 GitHub 仓库结构、Release 发布流程和项目初始代码组织方式。

## 当前包含

- 初始项目代码
- Web 端工程
- Android 工程
- docs 文档目录
- packages 共享模块规划
- 首次 Release 资源上传

## 注意事项

- 当前版本是测试版，不是正式稳定版
- 部分功能仍在开发中
- 后续版本将调整为更规范的 alpha / beta 版本号
```

---

## 十二、GPL-3.0 许可证建议

当前项目计划采用：

```text
GNU General Public License v3.0
```

建议仓库根目录存在许可证文件：

```text
LICENSE
```

文件内容应为 GPL-3.0 正文，开头通常为：

```text
GNU GENERAL PUBLIC LICENSE
Version 3, 29 June 2007
```

### 需要检查

- [ ] 仓库根目录是否存在 `LICENSE`
- [ ] GitHub 仓库首页右侧是否识别为 `GPL-3.0 license`
- [ ] README 许可证部分是否写明 GPL-3.0
- [ ] Release 包中是否包含许可证文件
- [ ] Android 端、Web 端、packages 共享代码是否统一遵守 GPL-3.0

### README 许可证部分建议写法

```md
## 许可证

本项目采用 GPL-3.0 License 开源。

你可以自由使用、复制、修改和分发本项目代码；如果你分发修改后的版本或基于本项目的衍生作品，需要继续遵守 GPL-3.0 的相关要求。
```

### GPL-3.0 的含义

GPL-3.0 是强 copyleft 开源许可证。它通常意味着：

- 允许他人使用、学习、修改和分发代码
- 允许商业使用
- 分发修改版或衍生版时，需要继续提供相应源码
- 分发修改版或衍生版时，需要保留 GPL-3.0 或兼容许可证
- 适合希望项目改进成果继续回馈开源社区的项目

### 与 MIT 的区别

| 许可证     | 特点                  |
| ------- | ------------------- |
| MIT     | 更宽松，别人可以闭源二次开发      |
| GPL-3.0 | 更强调开源传递，衍生分发通常也需要开源 |

如果你的目标是防止别人拿去改一改闭源发布，GPL-3.0 比 MIT 更合适。

---

## 十三、CHANGELOG.md 建议

建议新增或确认根目录文件：

```text
CHANGELOG.md
```

内容可以先写：

```md
# Changelog

## v1.0.0-test

- 首次发布测试版本
- 上传项目初始代码
- 初始化 monorepo 仓库结构
- 收口 Web 端源码到 apps/web
- 收口 Android 工程到 apps/android
- 新增 docs 文档目录
- 新增 packages 共享模块规划
- 整理产品说明、UI 规划和开发计划
- 验证 GitHub Release 发布流程
```

如果后续改用 `v0.1.0-alpha`，可以继续追加：

```md
## v0.1.0-alpha

- 调整版本号规范
- 完善 README 项目说明
- 确认 GPL-3.0 许可证
- 新增或完善 CHANGELOG
- 优化 Release 说明
- 补充项目结构说明
```

---

## 十四、CONTRIBUTING.md 建议

后续可以新增：

```text
CONTRIBUTING.md
```

内容可以写：

```md
# Contributing

感谢你关注 Shiroha Quiz。

当前项目仍处于早期开发阶段，欢迎通过 Issue 提交：

- Bug 反馈
- 功能建议
- 题库格式兼容问题
- UI / 交互优化建议
- 文档改进建议

提交 Pull Request 前，建议先通过 Issue 说明修改内容，避免重复开发。
```

这个文件不是现在最紧急的，但后续开源项目完善时建议补上。

---

## 十五、Issue 模板建议

后续可以新增目录：

```text
.github/
└─ ISSUE_TEMPLATE/
   ├─ bug_report.md
   └─ feature_request.md
```

Bug 模板：

```md
# Bug 反馈

## 问题描述

请简要描述遇到的问题。

## 复现步骤

1.
2.
3.

## 预期结果

## 实际结果

## 使用环境

- 系统：
- 浏览器：
- 应用版本：

## 截图或日志

如有截图或日志，请粘贴到这里。
```

功能建议模板：

```md
# 功能建议

## 功能描述

请简要描述希望增加的功能。

## 使用场景

这个功能主要解决什么问题？

## 期望效果

你希望它最终如何工作？

## 其他说明

可以补充相关截图、参考项目或示例。
```

---

## 十六、示例题库建议

建议后续新增：

```text
examples/
└─ sample-questions.md
```

或放在：

```text
docs/examples/
```

示例题库可以包含：

```md
# 示例题库

## 单选题

1. 下列哪一项是正确答案？
A. 选项一
B. 选项二
C. 选项三
D. 选项四

答案：A

## 判断题

1. Shiroha Quiz 是一个刷题工具。

答案：正确

## 多选题

1. 以下哪些属于常见题型？
A. 单选题
B. 多选题
C. 判断题
D. 填空题

答案：ABCD
```

这样用户更容易理解题库导入格式。

---

## 十七、截图和宣传图建议

README 中建议后续增加：

```md
## 项目截图

> 截图待补充。
```

后续可以放：

```text
assets/
├─ logo.png
├─ banner.png
└─ screenshots/
   ├─ web-home.png
   ├─ quiz-page.png
   └─ android-preview.png
```

建议优先补：

- [ ] 项目 Logo
- [ ] GitHub README 顶部宣传图
- [ ] Web 首页截图
- [ ] 刷题页面截图
- [ ] 题库导入页面截图
- [ ] Android 原型截图

视觉方向建议：

- 浅蓝色
- 白色
- 简洁
- 现代 App 风格
- 不使用过多装饰元素
- 不过度二次元化
- 保留学习、题卡、刷题、进度感

---

## 十八、题库导入策略说明

README 或 docs 中可以增加一份文档：

```text
docs/question-import-strategy.md
```

建议说明：

```md
# 题库导入策略

Shiroha Quiz 的题库导入目标是优先兼容常见、规范的题库格式，同时对少量异常题目进行局部智能处理。

## 基本原则

- 标准题目优先使用规则解析
- 异常题目单独标记
- 不对整份题库盲目执行复杂策略
- 无法确认的题目进入待确认状态
- 避免强行导入错误题目

## 处理流程

1. 标准格式快速解析
2. 局部异常题识别
3. 复杂策略辅助判断
4. 无法确认时标记为待处理
5. 用户人工确认
```

---

## 十九、手机端与 GPL-3.0 注意事项

Android 端可以继续放在同一个仓库中，例如：

```text
apps/android/
```

如果 Android 端属于同一个项目，应继续遵守 GPL-3.0。

需要注意：

- [ ] Android 端源码也应纳入 GPL-3.0 项目范围
- [ ] APK 发布时建议附带或说明许可证
- [ ] 引入第三方库时，检查许可证是否与 GPL-3.0 兼容
- [ ] 如果使用闭源 SDK，需要谨慎确认其授权条款
- [ ] Release 中发布 APK 时，建议同时保留源码入口和许可证说明

Android 端 README 可以写：

```md
## Android 端状态

Android 端目前处于早期开发阶段，后续将用于提供移动端刷题、错题复习和题库管理体验。

当前项目优先完善 Web 端、题库解析能力和核心刷题逻辑，Android 端会逐步接入共享模块。
```

---

## 二十、当前最优先执行顺序

建议按下面顺序落实：

1. [ ] 统一项目名称写法
2. [ ] 修改 README 首页文案
3. [ ] 补充 README 的下载说明
4. [ ] 补充 README 的运行方式
5. [ ] 补充 README 的项目结构
6. [ ] 补充 README 的当前状态
7. [ ] 确认根目录存在 GPL-3.0 LICENSE 文件
8. [ ] 确认 GitHub 能识别 GPL-3.0 license
9. [ ] 新增或完善 CHANGELOG.md
10. [ ] 优化当前 Release 说明
11. [ ] 后续新版本改用 `v0.x.x-alpha` 或 `v0.x.x-beta`
12. [ ] 增加项目截图或宣传图
13. [ ] 增加示例题库
14. [ ] 后续补 CONTRIBUTING.md
15. [ ] 后续补 Issue 模板
16. [ ] 后续补题库导入策略文档

---

## 二十一、最低改动清单

如果只想先做最小但有效的优化，建议至少完成：

```text
README.md
LICENSE
CHANGELOG.md
Release 说明
```

最低目标：

- 用户进仓库能知道这是什么
- 用户知道当前是不是测试版
- 用户知道在哪里下载
- 开发者知道项目结构
- GitHub 能识别 GPL-3.0 许可证
- 后续版本路线不混乱
- 仓库具备基本开源规范

---

## 二十二、建议本次提交信息

修改完成后，可以用下面的 commit message：

```text
docs: improve repository documentation and license notes
```

或者中文：

```text
docs: 完善仓库说明文档和许可证说明
```

如果同时完善 LICENSE 和 CHANGELOG，可以写：

```text
docs: update gpl license notes and changelog
```
