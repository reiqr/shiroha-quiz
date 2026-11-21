package com.yiqiu.shirohaquiz.ui.text

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.io.IOException

internal data class MathRenderResult(
    val heightPx: Int,
    val overflow: Boolean,
    val scrollWidthPx: Int
)

/**
 * 只负责「排版渲染」的 WebView：不接收任何触摸事件，不加载任何网络资源。
 *
 * 关键约束：
 * 1. 触摸事件全部返回 false，让手势回到 Compose，保证 LazyColumn 滚动与选项点击都不受影响。
 * 2. 所有请求都由 [shouldInterceptRequest] 决定，只允许读 `assets/katex/`，
 *    其它请求一律返回空响应，因此离线可用且不可能访问 CDN。
 * 3. 题目文本以 `textContent` 与 JSON 字符串形式注入，绝不拼 HTML，不执行题库内容里的脚本。
 */
internal class MathWebView private constructor(context: Context) : WebView(context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var pageReady = false
    private var loadRequested = false
    private var pendingScript: String? = null
    private var pendingGeneration = 0
    private var pendingCallback: ((MathRenderResult) -> Unit)? = null
    private var lastScript: String? = null

    private val bridge = Bridge()

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        setBackgroundColor(Color.TRANSPARENT)
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        isFocusable = false
        isFocusableInTouchMode = false
        isClickable = false
        isLongClickable = false
        isHapticFeedbackEnabled = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            databaseEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
            loadWithOverviewMode = false
            useWideViewPort = false
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptCanOpenWindowsAutomatically = false
            setGeolocationEnabled(false)
        }

        addJavascriptInterface(bridge, JS_BRIDGE_NAME)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = true

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse {
                val url = request?.url?.toString().orEmpty()
                return loadLocalAsset(url) ?: blockedResponse()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                flushPending()
            }
        }
    }

    private fun loadLocalAsset(url: String): WebResourceResponse? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        if (!uri.host.equals(ASSET_HOST, ignoreCase = true)) return null
        val path = uri.path.orEmpty().trimStart('/')
        if (path.isEmpty() || !path.startsWith(ASSET_PREFIX)) return null
        if (path.contains("..")) return null
        return try {
            val stream = context.assets.open(path)
            val mime = mimeTypeOf(path)
            WebResourceResponse(mime, if (isTextMime(mime)) "utf-8" else null, stream)
        } catch (e: IOException) {
            Log.w(TAG, "asset missing: $path", e)
            null
        }
    }

    private fun blockedResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }

    private fun mimeTypeOf(path: String): String {
        return when {
            path.endsWith(".html", true) -> "text/html"
            path.endsWith(".css", true) -> "text/css"
            path.endsWith(".js", true) -> "application/javascript"
            path.endsWith(".woff2", true) -> "font/woff2"
            path.endsWith(".woff", true) -> "font/woff"
            path.endsWith(".ttf", true) -> "font/ttf"
            path.endsWith(".json", true) -> "application/json"
            else -> "application/octet-stream"
        }
    }

    private fun isTextMime(mime: String): Boolean {
        return mime.startsWith("text/") || mime.contains("javascript") || mime.contains("json")
    }

    /** 只渲染，不消费触摸：手势必须回到 Compose，避免抢走滚动与选项点击。 */
    override fun onTouchEvent(event: MotionEvent): Boolean = false

    /** 连事件分发都不参与，确保 ViewGroup 不会把 WebView 当作触摸目标。 */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        flushPending()
    }

    fun render(script: String, generation: Int, callback: (MathRenderResult) -> Unit) {
        pendingGeneration = generation
        pendingCallback = callback
        if (script == lastScript && pendingScript == null) {
            // 同样的内容已经渲染过，只更新回调，避免重复回流
            return
        }
        lastScript = script
        pendingScript = script
        if (!pageReady) {
            ensurePageLoaded()
            return
        }
        flushPending()
    }

    private fun ensurePageLoaded() {
        if (loadRequested) return
        loadRequested = true
        runCatching { loadUrl(PAGE_URL) }
            .onFailure { Log.w(TAG, "failed to load math page", it) }
    }

    private fun flushPending() {
        val script = pendingScript ?: return
        if (!pageReady || !isAttachedToWindow) return
        pendingScript = null
        runCatching { evaluateJavascript(script, null) }
            .onFailure { Log.w(TAG, "failed to run render script", it) }
    }

    private fun deliverResult(generation: Int, result: MathRenderResult) {
        // 代数不匹配说明这是上一轮渲染的延迟回调，直接丢弃
        if (generation != pendingGeneration) return
        if (result.heightPx <= 0) return
        pendingCallback?.invoke(result)
    }

    /** 归还到复用池之前调用：清空页面内容，避免下一个使用位置先闪出旧公式。 */
    fun prepareForReuse() {
        pendingScript = null
        pendingCallback = null
        lastScript = null
        pendingGeneration = 0
        if (pageReady) {
            runCatching {
                evaluateJavascript(
                    "try{document.getElementById('root').textContent='';}catch(e){}",
                    null
                )
            }
        }
    }

    fun dispose() {
        pendingScript = null
        pendingCallback = null
        runCatching {
            stopLoading()
            (parent as? android.view.ViewGroup)?.removeView(this)
            destroy()
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onRenderFinished(
            generation: Int,
            heightPx: Int,
            overflow: Int,
            scrollWidthPx: Int
        ) {
            mainHandler.post {
                deliverResult(
                    generation,
                    MathRenderResult(
                        heightPx = heightPx,
                        overflow = overflow != 0,
                        scrollWidthPx = scrollWidthPx
                    )
                )
            }
        }

        @JavascriptInterface
        fun onEngineReady() {
            // 仅用于调试与健康检查，无需回调
        }
    }

    companion object {
        private const val TAG = "ShirohaMath"
        private const val JS_BRIDGE_NAME = "ShirohaMath"
        private const val ASSET_HOST = "shiroha.math"
        private const val ASSET_PREFIX = "katex/"
        private const val PAGE_PATH = "katex/shiroha-math.html"
        private val PAGE_URL = "https://$ASSET_HOST/$PAGE_PATH"

        fun create(context: Context): MathWebView? {
            return runCatching { MathWebView(context).also { it.configure() } }
                .onFailure { Log.w(TAG, "unable to create math webview", it) }
                .getOrNull()
        }
    }
}
