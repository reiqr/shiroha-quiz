# WebDAV 请求回归

在仓库根目录使用 Node.js 18+ 运行，无需安装依赖：

```powershell
node --test test/web-webdav/request.test.cjs
```

测试读取 Web 端实际请求与诊断函数，使用模拟 fetch，不访问真实服务，不使用真实凭据。

覆盖 #131：HTTPS 页面请求 HTTP WebDAV 时确实调用 fetch；成功响应正常返回；网络失败只推断可能的混合内容拦截；超时和 HTTP 状态码保持原有诊断；定时器在各路径均清理。

这些测试验证应用行为。浏览器是否允许混合内容、服务端 CORS 配置和真实服务连通性仍以使用环境为准。
