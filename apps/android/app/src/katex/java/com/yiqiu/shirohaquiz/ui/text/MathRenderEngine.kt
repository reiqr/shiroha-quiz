package com.yiqiu.shirohaquiz.ui.text

import android.content.Context
import android.util.LruCache
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * 公式渲染的运行时开关，便于出问题时一键退回纯文本实现。
 */
object MathRenderSettings {
    /** 关闭后 [MathRichText] 完全退回原有纯文本渲染路径。 */
    @Volatile
    var enabled: Boolean = true

    /** 关闭后不再使用 WebView 引擎（用于排查 WebView 相关异常）。 */
    @Volatile
    var webViewEngineEnabled: Boolean = true

    /** 复用池里最多保留几个已加载 KaTeX 的 WebView。 */
    @Volatile
    var maxIdleWebViews: Int = 2
}

/**
 * WebView 复用池。
 *
 * 公式排版是 KaTeX 的 DOM 渲染，创建 WebView 的成本远高于渲染本身，
 * 因此这里保留少量「文档已加载」的实例跨题复用，滚动离开屏幕时归还而不是销毁。
 */
internal object MathWebViewPool {

    private const val MAX_ACTIVE_WEB_VIEWS = 12
    private val idle = ArrayDeque<MathWebView>()
    private val active = mutableSetOf<MathWebView>()
    private var creationFailed = false

    fun obtain(context: Context): MathWebView? {
        if (active.size >= MAX_ACTIVE_WEB_VIEWS) return null
        while (idle.isNotEmpty()) {
            val candidate = idle.removeFirst()
            if (!candidate.isDisposed && candidate.context === context) {
                active.add(candidate)
                return candidate
            }
            candidate.dispose()
        }
        if (creationFailed) return null
        return MathWebView.create(context).also {
            creationFailed = it == null
            if (it != null) active.add(it)
        }
    }

    fun recycle(view: MathWebView) {
        if (!active.remove(view)) return
        if (view.isDisposed) return
        view.prepareForReuse()
        if (view.isDisposed) return
        if (idle.size >= MathRenderSettings.maxIdleWebViews) {
            view.dispose()
            return
        }
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        idle.addLast(view)
        trimOverflow()
    }

    private fun trimOverflow() {
        while (idle.size > MathRenderSettings.maxIdleWebViews) {
            idle.removeFirst().dispose()
        }
    }

    /** Activity 销毁时调用，释放所有缓存的 WebView。 */
    fun clear() {
        creationFailed = false
        while (idle.isNotEmpty()) {
            idle.removeFirst().dispose()
        }
    }
}

/** 已渲染高度缓存：重新进入题目时可以直接按正确高度布局，避免明显跳动。 */
internal object MathHeightCache {
    private val cache = object : LruCache<MathRenderLayout, Int>(128) {}

    fun get(key: MathRenderLayout): Int = cache.get(key) ?: 0

    fun put(key: MathRenderLayout, height: Int) {
        if (height > 0) cache.put(key, height)
    }

    fun clear() = cache.evictAll()
}

/** 渲染代数：丢弃上一轮渲染遗留的延迟高度回调。 */
internal object MathRenderSequence {
    private val counter = AtomicInteger(0)
    fun next(): Int = counter.incrementAndGet()
}
