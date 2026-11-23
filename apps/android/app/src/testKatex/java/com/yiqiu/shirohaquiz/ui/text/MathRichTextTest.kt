package com.yiqiu.shirohaquiz.ui.text

import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceResponse
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.components.QuizOptionCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionResultStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowWebView
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "mdpi")
class MathRichTextTest {
    private val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    private val composeView = ComposeView(controller.get())
    private val looper = shadowOf(Looper.getMainLooper())
    private val frameClock = BroadcastFrameClock()
    // Each Robolectric test needs its own dispatcher. The process-wide Android
    // dispatcher can retain a Choreographer from a previous test.
    private val dispatcher = StandardTestDispatcher()
    private val uiContext = dispatcher + frameClock
    private val recomposer = Recomposer(uiContext)
    private val recomposeJob = CoroutineScope(uiContext).launch {
        recomposer.runRecomposeAndApplyChanges()
    }

    @After
    fun close() {
        composeView.disposeComposition()
        recomposer.cancel()
        recomposeJob.cancel()
        dispatcher.scheduler.runCurrent()
        controller.pause().stop().destroy()
        MathWebViewPool.clear()
        MathHeightCache.clear()
    }

    @Test
    fun offscreenPreviewWaitsForViewportBeforeBorrowingAWebView() {
        val scroll = ScrollState(0)
        show {
            Column(Modifier.height(240.dp).verticalScroll(scroll)) {
                Spacer(Modifier.height(400.dp))
                MathRichText("题干 \$x^2\$")
            }
        }
        assertNull(webView())

        scroll.dispatchRawDelta(400f)
        frame()
        requireNotNull(webView())

        scroll.dispatchRawDelta(-400f)
        frame()
        assertNull(webView())
    }

    @Test
    fun widthChangeReissuesRenderAndRejectsOldHeightAndScriptReply() {
        val width = mutableIntStateOf(240)
        show {
            Box {
                Box(Modifier.width(width.intValue.dp)) { MathRichText("题干 \$x^2\$") }
            }
        }
        val webView = requireNotNull(webView())
        val shadow = shadowOf(webView)
        pageFinished(shadow, webView)
        val oldGen = generation(shadow)
        val oldReply = shadow.lastEvaluatedJavascriptCallback
        assertTrue(shadow.lastEvaluatedJavascript.contains("\"widthDp\":240"))
        oldReply.onReceiveValue("true")
        report(shadow, oldGen, 24, scrollWidth = 340)
        frame()
        val scroll = nodes().mapNotNull {
            it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
        }.singleOrNull()
        assertEquals(340, (webView.parent as View).measuredWidth)
        requireNotNull(scroll)
        assertEquals(100f, scroll.maxValue(), 0f)
        assertEquals(oldGen, generation(shadow))
        assertSame(oldReply, shadow.lastEvaluatedJavascriptCallback)

        width.intValue = 180
        frame()
        val intermediateGen = generation(shadow)
        assertNotEquals(oldGen, intermediateGen)
        val staleReply = shadow.lastEvaluatedJavascriptCallback

        width.intValue = 160
        frame()
        val newGen = generation(shadow)
        assertNotEquals(intermediateGen, newGen)
        assertTrue(shadow.lastEvaluatedJavascript.contains("\"widthDp\":160"))

        shadow.lastEvaluatedJavascriptCallback.onReceiveValue("true")
        report(shadow, newGen, 52)
        staleReply.onReceiveValue("null")
        report(shadow, intermediateGen, 88)
        report(shadow, oldGen, 99)
        frame()
        val textNode = nodes().single { it.config.getOrNull(SemanticsProperties.Text) != null }
        assertEquals(160f, textNode.boundsInRoot.width, 0f)
        assertEquals(52f, textNode.boundsInRoot.height, 0f)
        assertTrue(nodes().none { it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null })
    }

    @Test
    fun formulaHasExactlyOneReadableTextBeforeAndAfterRendering() {
        show { MathRichText("题干 \$x^2\$") }
        assertEquals(1, labels().count { it == "题干 x²" })
        val webView = requireNotNull(webView())
        val shadow = shadowOf(webView)
        pageFinished(shadow, webView)
        shadow.lastEvaluatedJavascriptCallback.onReceiveValue("true")
        report(shadow, generation(shadow), 32)
        frame()
        assertEquals(1, labels().count { it == "题干 x²" })
        assertEquals(32f, nodes().single {
            it.config.getOrNull(SemanticsProperties.Text) != null
        }.boundsInRoot.height, 0f)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, webView.importantForAccessibility)
    }

    @Test
    fun formulaMergesIntoExistingClickableOptionSemantics() {
        var clicks = 0
        show {
            QuizOptionCard(
                label = "A", text = "\$x^2\$", selected = true,
                resultStyle = QuizOptionResultStyle.Wrong, compact = true,
                onClick = { clicks++ }
            )
        }
        val option = nodes().single { it.config.getOrNull(SemanticsActions.OnClick) != null }
        assertEquals("已选中，回答错误", option.config[SemanticsProperties.StateDescription])
        assertEquals(listOf("A.", "x²"), option.config[SemanticsProperties.Text].map { it.text })
        assertTrue(option.config[SemanticsActions.OnClick].action!!.invoke())
        assertEquals(1, clicks)
    }

    @Test
    @Config(shadows = [BrokenProviderShadow::class])
    fun unavailableProviderKeepsTextAndDoesNotRetryOnOtherBlocksOrResize() {
        val width = mutableIntStateOf(240)
        BrokenProviderShadow.creationAttempts = 0
        show {
            Column(Modifier.width(width.intValue.dp)) {
                MathRichText("题干 \$x^2\$")
                MathRichText("解析 \$x^2\$")
            }
        }
        assertFallback()
        assertTrue(labels().contains("解析 x²"))
        width.intValue = 160
        frame()
        assertFallback()
        assertEquals(1, BrokenProviderShadow.creationAttempts)
    }

    @Test
    fun mainPageErrorShowsFallbackAndDestroysFailedView() {
        show { MathRichText("题干 \$x^2\$") }
        val webView = requireNotNull(webView())
        val shadow = shadowOf(webView)
        val request = object : WebResourceRequest {
            override fun getUrl() = Uri.parse(shadow.lastLoadedUrl)
            override fun isForMainFrame() = true
            override fun isRedirect() = false
            override fun hasGesture() = false
            override fun getMethod() = "GET"
            override fun getRequestHeaders() = emptyMap<String, String>()
        }
        val response = WebResourceResponse("text/plain", "utf-8", null)
        response.setStatusCodeAndReasonPhrase(404, "Not Found")
        shadow.webViewClient.onReceivedHttpError(webView, request, response)
        frame()
        assertFallback()
        assertTrue(shadow.wasDestroyCalled())
    }

    @Test
    fun unsuccessfulScriptReplyShowsFallback() {
        show { MathRichText("题干 \$x^2\$") }
        val webView = requireNotNull(webView())
        val shadow = shadowOf(webView)
        pageFinished(shadow, webView)
        shadow.lastEvaluatedJavascriptCallback.onReceiveValue("false")
        frame()
        assertFallback()
        assertTrue(shadow.wasDestroyCalled())
    }

    @Test
    fun missingScriptReplyOrMeasurementTimesOut() {
        show { MathRichText("题干 \$x^2\$") }
        val webView = requireNotNull(webView())
        val shadow = shadowOf(webView)
        pageFinished(shadow, webView)
        looper.idleFor(Duration.ofSeconds(15))
        frame()
        assertFallback()
        assertTrue(shadow.wasDestroyCalled())
    }

    @Implements(WebView::class)
    class BrokenProviderShadow : ShadowWebView() {
        @Implementation
        override fun getSettings(): WebSettings {
            creationAttempts++
            throw IllegalStateException("Injected WebView initialization failure")
        }

        companion object {
            var creationAttempts = 0
        }
    }

    private fun show(content: @Composable () -> Unit) {
        composeView.setParentCompositionContext(recomposer)
        controller.get().setContentView(composeView)
        composeView.setContent { Box { content() } }
        frame()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun frame() {
        repeat(3) {
            looper.idleFor(Duration.ofMillis(16))
            Snapshot.sendApplyNotifications()
            dispatcher.scheduler.runCurrent()
            frameClock.sendFrame(SystemClock.uptimeMillis() * 1_000_000)
            dispatcher.scheduler.runCurrent()
            looper.idle()
            val spec = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
            composeView.measure(spec, spec)
            composeView.layout(0, 0, 400, 400)
            descendants(composeView).filterIsInstance<RootForTest>().singleOrNull()
                ?.measureAndLayoutForTest()
        }
    }

    private fun webView(): WebView? = descendants(composeView).filterIsInstance<WebView>().firstOrNull()

    private fun nodes(): List<SemanticsNode> {
        val root = descendants(composeView).filterIsInstance<RootForTest>().single()
            .semanticsOwner.rootSemanticsNode
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return walk(root)
    }

    private fun labels(): List<String> =
        nodes().flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }

    private fun assertFallback() {
        assertNull(webView())
        val text = nodes().single {
            it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == "题干 x²"
        }
        assertTrue(text.boundsInRoot.height > 1f)
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
        }
    }

    private fun pageFinished(shadow: ShadowWebView, webView: WebView) {
        shadow.webViewClient.onPageFinished(webView, shadow.lastLoadedUrl)
    }

    private fun generation(shadow: ShadowWebView): Int =
        Regex("\"gen\":(\\d+)").find(shadow.lastEvaluatedJavascript)!!.groupValues[1].toInt()

    private fun report(shadow: ShadowWebView, generation: Int, height: Int, scrollWidth: Int = 0) {
        val bridge = shadow.getJavascriptInterface("ShirohaMath")
        val method = bridge.javaClass.methods.single { it.name == "onRenderFinished" }
        method.isAccessible = true
        method.invoke(bridge, generation, height, if (scrollWidth > 0) 1 else 0, scrollWidth)
    }
}
