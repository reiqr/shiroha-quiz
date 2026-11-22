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
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
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
 * 2. 所有请求都由 WebViewClient 拦截，只允许读 `assets/katex/`，
 *    其它请求一律返回空响应，因此离线可用且不可能访问 CDN。
 * 3. 题目文本以 `textContent` 与 JSON 字符串形式注入，绝不拼 HTML，不执行题库内容里的脚本。
 */
internal class MathWebView private constructor(context: Context) : WebView(context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var pageReady = false
    private var loadRequested = false
    private var pendingScript: String? = null
    private var pendingGeneration = 0
    private var pendingCallback: ((MathRenderResult?) -> Unit)? = null
    private var scriptSucceeded = false
    private var measurement: MathRenderResult? = null
    private val renderTimeout = Runnable { fail("math page or render timed out") }

    var isDisposed = false
        private set

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
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

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
                val uri = request?.url
                return if (uri != null && isLocalAsset(uri)) {
                    loadLocalAsset(uri) ?: emptyResponse(404, "Not Found")
                } else {
                    emptyResponse(403, "Forbidden")
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (isDisposed || url != PAGE_URL) return
                pageReady = true
                flushPending()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame || isLocalAsset(request.url)) {
                    fail("math page or asset failed to load (${error.errorCode})")
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.isForMainFrame || isLocalAsset(request.url)) {
                    fail("math page or asset returned HTTP ${errorResponse.statusCode}")
                }
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                fail("math WebView renderer exited")
                return true
            }
        }
    }

    private fun isLocalAsset(uri: Uri): Boolean =
        uri.scheme == "https" &&
            uri.host.equals(ASSET_HOST, ignoreCase = true) &&
            uri.path.orEmpty().startsWith("/$ASSET_PREFIX") &&
            !uri.path.orEmpty().contains("..")

    private fun loadLocalAsset(uri: Uri): WebResourceResponse? {
        val path = uri.path.orEmpty().trimStart('/')
        return try {
            val stream = context.assets.open(path)
            val mime = mimeTypeOf(path)
            WebResourceResponse(mime, if (isTextMime(mime)) "utf-8" else null, stream)
        } catch (e: IOException) {
            Log.w(TAG, "asset missing: $path", e)
            null
        }
    }

    private fun emptyResponse(status: Int, reason: String): WebResourceResponse {
        return WebResourceResponse(
            "text/plain", "utf-8", status, reason, emptyMap(), ByteArrayInputStream(ByteArray(0))
        )
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

    fun render(script: String, generation: Int, callback: (MathRenderResult?) -> Unit) {
        if (isDisposed) {
            callback(null)
            return
        }
        pendingCallback = callback
        if (generation == pendingGeneration) return

        pendingGeneration = generation
        pendingScript = script
        scriptSucceeded = false
        measurement = null
        mainHandler.removeCallbacks(renderTimeout)
        mainHandler.postDelayed(renderTimeout, RENDER_TIMEOUT_MS)
        if (!loadRequested) {
            loadRequested = true
            runCatching { loadUrl(PAGE_URL) }
                .onFailure { fail("failed to load math page", it) }
        }
        flushPending()
    }

    private fun flushPending() {
        val script = pendingScript ?: return
        if (!pageReady || !isAttachedToWindow) return
        val generation = pendingGeneration
        pendingScript = null
        runCatching {
            evaluateJavascript(script) { reply ->
                if (generation == pendingGeneration && !isDisposed) {
                    if (reply == "true") {
                        scriptSucceeded = true
                        publishMeasurement()
                    } else {
                        fail("math render script did not complete")
                    }
                }
            }
        }.onFailure { fail("failed to run math render script", it) }
    }

    private fun deliverResult(generation: Int, result: MathRenderResult) {
        if (generation != pendingGeneration || isDisposed || result.heightPx <= 0) return
        measurement = result
        publishMeasurement()
    }

    private fun publishMeasurement() {
        val result = measurement ?: return
        if (!scriptSucceeded) return
        mainHandler.removeCallbacks(renderTimeout)
        pendingCallback?.invoke(result)
    }

    private fun fail(message: String, cause: Throwable? = null) {
        if (isDisposed) return
        Log.w(TAG, message, cause)
        val callback = pendingCallback
        dispose()
        callback?.invoke(null)
    }

    private fun resetRender() {
        mainHandler.removeCallbacks(renderTimeout)
        pendingScript = null
        pendingCallback = null
        pendingGeneration = 0
        measurement = null
        scriptSucceeded = false
    }

    /** 归还到复用池之前调用：清空页面内容，避免下一个使用位置先闪出旧公式。 */
    fun prepareForReuse() {
        resetRender()
        if (pageReady) {
            runCatching {
                evaluateJavascript("document.getElementById('root').textContent='';", null)
            }.onFailure { fail("failed to clear math page", it) }
        }
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        resetRender()
        runCatching { stopLoading() }
        (parent as? android.view.ViewGroup)?.removeView(this)
        runCatching { destroy() }
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
    }

    companion object {
        private const val TAG = "ShirohaMath"
        private const val RENDER_TIMEOUT_MS = 5_000L
        private const val JS_BRIDGE_NAME = "ShirohaMath"
        private const val ASSET_HOST = "shiroha.math"
        private const val ASSET_PREFIX = "katex/"
        private const val PAGE_PATH = "katex/shiroha-math.html"
        private val PAGE_URL = "https://$ASSET_HOST/$PAGE_PATH"

        fun create(context: Context): MathWebView? {
            var view: MathWebView? = null
            return runCatching {
                MathWebView(context).also {
                    view = it
                    it.configure()
                }
            }.onFailure {
                Log.w(TAG, "unable to create math webview", it)
                view?.dispose()
            }.getOrNull()
        }
    }
}
