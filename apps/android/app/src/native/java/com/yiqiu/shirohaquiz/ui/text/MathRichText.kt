package com.yiqiu.shirohaquiz.ui.text

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 统一的「文本 + LaTeX 公式」显示组件。
 *
 * - 不含公式时走原生 [Text]，与改造前完全一致（零额外开销）。
 * - 含公式时用离线 KaTeX（assets 内置）在一个复用 WebView 里整段排版，
 *   中文与行内公式的基线、分式、矩阵、极限等都由 KaTeX 正确处理。
 * - WebView 不可用或渲染失败时，原生文本继续显示题库原文。
 */
@Composable
fun MathRichText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val segments = remember(text) { LatexSegmenter.split(text) }
    val hasMath = remember(segments) { segments.any { it is LatexSegment.Math } }

    if (!hasMath || !MathRenderSettings.enabled) {
        Text(
            text = LatexDisplayFormatter.format(text),
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            style = style
        )
        return
    }

    MathWebText(
        segments = segments,
        rawText = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
private fun MathWebText(
    segments: List<LatexSegment>,
    rawText: String,
    modifier: Modifier,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight?,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    textAlign: TextAlign?,
    maxLines: Int,
    overflow: TextOverflow
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val accessibleText = remember(rawText) {
        AnnotatedString(LatexDisplayFormatter.format(rawText).ifBlank { rawText })
    }
    var engineFailed by remember(context) { mutableStateOf(false) }
    val resolvedColor = if (color == Color.Unspecified) LocalContentColor.current else color
    val fallback: @Composable () -> Unit = {
        Text(
            text = rawText,
            modifier = Modifier.clearAndSetSemantics {},
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            style = style
        )
    }

    val baseFontSize = if (fontSize == TextUnit.Unspecified) style.fontSize else fontSize
    val baseLineHeight = if (lineHeight == TextUnit.Unspecified) style.lineHeight else lineHeight
    val fontSizeDp = density.toCssPx(baseFontSize, 16f)
    val lineHeightDp = density.toCssPx(baseLineHeight, 0f).let { resolved ->
        if (resolved > 0f) resolved else fontSizeDp * 1.5f
    }
    val weightValue = (fontWeight ?: style.fontWeight ?: FontWeight.Normal).weight
    val alignSpec = (textAlign ?: style.textAlign).toCssAlign()
    val colorArgb = resolvedColor.toArgb()
    val clampedLines = if (maxLines in 1..Int.MAX_VALUE - 1) maxLines else 0

    val renderStyle = remember(fontSizeDp, lineHeightDp, colorArgb, weightValue, alignSpec, clampedLines) {
        MathRenderStyle(
            fontSizePx = fontSizeDp,
            lineHeightPx = lineHeightDp,
            colorArgb = colorArgb,
            fontWeight = weightValue,
            textAlign = alignSpec,
            maxLines = clampedLines
        )
    }

    BoxWithConstraints(modifier.semantics { text = accessibleText }) {
        if (!MathRenderSettings.webViewEngineEnabled || engineFailed ||
            !constraints.hasBoundedWidth || maxWidth < 8.dp
        ) {
            fallback()
            return@BoxWithConstraints
        }

        val layout = remember(segments, renderStyle, maxWidth) {
            MathRenderLayout(segments, renderStyle, maxWidth.value)
        }
        val generation = remember(layout) { MathRenderSequence.next() }
        val script = remember(generation) { MathRenderPayload.buildScript(layout, generation) }
        var measurement by remember(generation) { mutableStateOf<MathRenderResult?>(null) }
        val cachedHeightDp = remember(generation) { MathHeightCache.get(layout) }
        val result = measurement
        val scrollState = key(rawText) { rememberScrollState() }

        Box(Modifier.fillMaxWidth()) {
            if (result == null) fallback()

            val overflowWidth = result?.takeIf { it.overflow }?.scrollWidthPx
            val webViewport = if (overflowWidth != null) {
                Modifier.fillMaxWidth().horizontalScroll(scrollState)
            } else {
                Modifier.fillMaxWidth()
            }
            Box(webViewport) {
                AndroidView(
                    factory = { ctx -> MathTextContainer(ctx) },
                    update = { container ->
                        val webView = container.ensureEngine()
                        if (webView == null) {
                            engineFailed = true
                        } else {
                            webView.render(script, generation) { measured ->
                                if (measured == null) {
                                    engineFailed = true
                                } else {
                                    measurement = measured
                                    MathHeightCache.put(layout, measured.heightPx)
                                }
                            }
                        }
                    },
                    onRelease = { container -> container.releaseEngine() },
                    modifier = (overflowWidth?.let { Modifier.width(it.dp) } ?: Modifier.fillMaxWidth())
                        .height((result?.heightPx ?: cachedHeightDp.coerceAtLeast(1)).dp)
                        .alpha(if (result == null) 0f else 1f)
                )
            }
        }
    }
}

/** 容器在生命周期内只尝试一次创建，由 Compose 在失败时显示原文。 */
internal class MathTextContainer(context: Context) : FrameLayout(context) {

    private var engine: MathWebView? = null
    private var requestedEngine = false

    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    fun ensureEngine(): MathWebView? {
        if (!requestedEngine) {
            requestedEngine = true
            engine = MathWebViewPool.obtain(context)?.also {
                addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }
        }
        return engine
    }

    fun releaseEngine() {
        val current = engine ?: return
        engine = null
        removeView(current)
        MathWebViewPool.recycle(current)
    }
}

/** WebView 的 CSS 像素等价于 Compose 的 dp，因此这里换算到 dp 而非物理像素。 */
private fun Density.toCssPx(value: TextUnit, fallbackPx: Float): Float {
    if (value == TextUnit.Unspecified) return fallbackPx
    val raw = value.value
    if (raw <= 0f) return fallbackPx
    return when {
        value.isSp -> raw * fontScale
        value.isEm -> raw * (fallbackPx.takeIf { it > 0f } ?: 16f)
        else -> raw / density
    }
}

private fun TextAlign?.toCssAlign(): String = when (this) {
    TextAlign.Center -> "center"
    TextAlign.Right, TextAlign.End -> "right"
    TextAlign.Justify -> "justify"
    else -> "left"
}
