# Shiroha Quiz 原生 Android v30.3 主题 Token 整理

## 版本号

`v30.3.0 

## 改动范围

仅处理原生 Android / Compose 版：

`apps/android/app/src/native/`

## 主要改动

1. 新增并整理主题 Token：
   
   - `ShirohaColors`
   - `ShirohaDimens`
   - `ShirohaMotion`

2. 将浅色主题的核心颜色改为从 `ShirohaColors` 读取。

3. 将部分常用尺寸与动效参数集中管理：
   
   - Hero 卡片高度
   - Hero 图片尺寸
   - 步骤胶囊尺寸
   - 按钮高度与图标尺寸
   - 底部导航尺寸
   - 页面切换动画时长
   - 开屏页显示与淡出时长

4. 保持现有浅色视觉效果不变，为后续夜间模式做准备。

## 未改动范围

- 未新增夜间模式入口。
- 未改练习 / 考试业务逻辑。
- 未改题库导入解析。
- 未改题库数据结构。
- 未改 Web 版。
- 未改 WebView 壳。

## 注意事项

本包基于 `shiroha_native_v30_2_1_splash_asset_name_v27.zip` 继续叠加，保留 v27 开屏页和此前 v25 视觉修正。后续若进入 v30.4 夜间模式，应继续基于 v28，不要回退到旧包。

## 中文 Commit Message

```text
theme: 整理原生端主题 Token
```

## English Commit Message

```text
theme: organize native theme tokens
```
