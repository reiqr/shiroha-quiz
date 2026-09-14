# Web 单题 AI 端到端回归

仅使用自建安全题库和假密钥，每个用例新建 Playwright context，不接触用户浏览器数据。`page.route` 拦截 `https://mock-ai.invalid/**`，返回 OpenAI 格式解析 JSON / 追问正文；其它 API 请求一律阻断。不安装包，不调用真实账户或收费 API，不改应用文件。

其他电脑需预先具备 Node、可被 `require('playwright')` 解析的 Playwright 包及已安装的 Chromium 浏览器（或显式指定现有 Chromium/Edge）。脚本不安装依赖、不下载浏览器；本机 runtime 不随仓库分发。加载包时先 `require('playwright')`，仅缺包时兜底当前机器的 bundled 目录。

在仓库根目录通用运行：

```powershell
node ./test/web-ai-practice/runner.cjs
node ./test/web-ai-practice/runner.cjs --self-test
```

当前机器 PowerShell 示例（无需 npm，已存在 Playwright、无缓存 Chromium，使用已有 Edge）：

```powershell
& 'C:\Users\REiQEr\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' 'E:\codex\exercise\shiroha_quiz\test\web-ai-practice\runner.cjs' --executable-path 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
```

默认服务仅监听 `127.0.0.1` 随机端口，root 固定 `apps/web`，拒绝目录穿越和越界 junction；内置题库脚本不会加载。已有本地静态服务可加 `--base-url http://127.0.0.1:8080/`（目录应直接提供 Web 页面；只接受 loopback HTTP）。

- `--self-test`：只验证框架、mock、网络隔离、输出路径限制及自带服务的穿越拒绝，不代表业务回归通过。
- `--list` / `--filter quota`：列出用例 / 按名称子串筛选。
- `--timeout 10000` / `--headed`：单操作超时毫秒 / 显示独立测试浏览器。
- `--executable-path 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'`：Playwright 浏览器缓存缺失时，显式指定已安装的 Chromium/Edge；仍用 Playwright、临时 profile 和独立 context，不安装浏览器。
- `--report reports/latest.jsonl`：可选机器报告，路径必须在本目录；默认只输出 stdout JSONL，不写产物。

退出码：`0` 全通过，`1` 业务测试失败，`2` 参数、Playwright/浏览器或框架错误。stdout 每行 JSON，包含 `start`、`case`、`summary`；失败含错误栈、拦截请求和页面异常。

覆盖按需生成、追问/切题隔离、收起重开、新轮次/重置/退出清空及迟到响应隔离、取消/失败保留草稿、重生成成功才清空对话、仅保存解析、拒绝覆写确认、历史记录不变、quota 回滚、图片未发送强制低置信、多空等价答案及简答自评路径。未配置专测真实点击前往 AI 设置，验证表单可见、退出 focus 但保留原 practice/answerState 对象、答案及进度，返回练习不重新开始、不请求 API。富文本专测覆盖旧解析表格替换、题干/选项富字段不变、features 重算、公式反斜杠及保存后继续追问。

状态适配集中在 `harness.cjs`：优先直接读取页面 global lexical `state/practice`；当前应用若仍有最外层 IIFE，仅在浏览器脚本响应内注入只读快照函数，报告 `readOnlyIifeBridge: true`，磁盘源码和业务函数不改。现有启动归一化会丢弃 richContent，富文本专测在启动后仅为自建题目初始化富字段，随后真实点击练习/保存。迟到响应用例仅对 mock fetch 去掉 abort signal，模拟取消后的响应竞态，验证会话身份保护。已有解析保存时自动确认覆写（拒绝覆写专测除外）。
