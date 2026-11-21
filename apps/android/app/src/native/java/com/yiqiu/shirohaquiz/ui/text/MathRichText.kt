package com.yiqiu.shirohaquiz.ui.text

import android.content.Context
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
 * - 渲染失败、WebView 不可用、或开关关闭时，自动回退到 [LatexDisplayFormatter] 的纯文本结果，
 *   原始公式内容不会被吞掉。
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
        maxLines = maxLines
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
    maxLines: Int
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val engineSupported = remember(context) {
        MathRenderSettings.webViewEngineEnabled && MathEngineAvailability.isSupported(context)
    }
    val fallbackText = remember(rawText) { LatexDisplayFormatter.format(rawText) }
    val resolvedColor = if (color == Color.Unspecified) LocalContentColor.current else color

    if (!engineSupported) {
        Text(
            text = fallbackText,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            textAlign = textAlign,
            style = style
        )
        return
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

    BoxWithConstraints(modifier) {
        // 宽度不受限时无法排版（WebView 需要一个确定宽度），直接退回纯文本
        if (!constraints.hasBoundedWidth || maxWidth.value < 8f) {
            Text(
                text = fallbackText,
                style = style,
                color = color,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
                textAlign = textAlign
            )
            return@BoxWithConstraints
        }

        val availableWidthDp = maxWidth.value
        val availableWidthPx = constraints.maxWidth
        val cacheKey = remember(renderStyle, availableWidthPx, segments) {
            "${renderStyle.cacheKey()}|$availableWidthPx|${segments.hashCode()}"
        }
        val render = remember(segments, renderStyle) {
            val generation = MathRenderSequence.next()
            generation to MathRenderPayload.buildScript(segments, renderStyle, generation)
        }

        var measuredHeightDp by remember(cacheKey) {
            mutableIntStateOf(MathHeightCache.get(cacheKey))
        }
        var requiredWidthDp by remember(cacheKey) { mutableIntStateOf(0) }

        val needsScroll = requiredWidthDp > 0 && requiredWidthDp > availableWidthDp + 1f
        val scrollState = rememberScrollState()

        Box(
            modifier = if (needsScroll) {
                Modifier.fillMaxWidth().horizontalScroll(scrollState)
            } else {
                Modifier.fillMaxWidth()
            }
        ) {
            // 高度尚未测量出来之前，用透明纯文本占位，避免整屏跳动
            if (measuredHeightDp <= 0) {
                Text(
                    text = fallbackText,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = fontWeight,
                    textAlign = textAlign,
                    style = style
                )
            }

            AndroidView(
                factory = { ctx -> MathTextContainer(ctx) },
                update = { container ->
                    val webView = container.ensureEngine() ?: return@AndroidView
                    webView.render(render.second, render.first) { result ->
                        if (result.heightPx > 0 && result.heightPx != measuredHeightDp) {
                            measuredHeightDp = result.heightPx
                            MathHeightCache.put(cacheKey, result.heightPx)
                        }
                        if (requiredWidthDp == 0 && result.overflow &&
                            result.scrollWidthPx > availableWidthDp + 1f
                        ) {
                            requiredWidthDp = result.scrollWidthPx
                        }
                    }
                },
                onRelease = { container -> container.releaseEngine() },
                modifier = Modifier
                    .then(
                        if (needsScroll) {
                            Modifier.width(requiredWidthDp.dp)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    )
                    .height((if (measuredHeightDp > 0) measuredHeightDp else 1).dp)
            )
        }
    }
}

/** 承载 WebView 的容器：WebView 创建失败时不会崩溃，而是让上层继续用占位文本。 */
internal class MathTextContainer(context: Context) : FrameLayout(context) {

    private var engine: MathWebView? = null

    fun ensureEngine(): MathWebView? {
        engine?.let { return it }
        val created = MathWebViewPool.obtain(context) ?: return null
        engine = created
        addView(created, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        return created
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
