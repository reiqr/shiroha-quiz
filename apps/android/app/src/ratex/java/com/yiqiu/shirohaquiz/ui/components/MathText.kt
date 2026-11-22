package com.yiqiu.shirohaquiz.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yiqiu.shirohaquiz.ui.text.LatexSegment
import com.yiqiu.shirohaquiz.ui.text.LatexSegmenter
import com.yiqiu.shirohaquiz.ui.text.formatPlainMathText
import io.ratex.RaTeXException
import io.ratex.RaTeXSpan

@Composable
internal fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val segments = remember(text) { LatexSegmenter.split(text) }
    if (segments.none { it is LatexSegment.Math }) {
        Text(formatPlainMathText(text), modifier = modifier, style = style, color = color,
            fontWeight = fontWeight, maxLines = maxLines, overflow = overflow)
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }.toArgb()
    val textSize = with(density) { (if (style.fontSize.isSpecified) style.fontSize else 16.sp).toPx() }
    val lineHeight = with(density) { if (style.lineHeight.isSpecified) style.lineHeight.toPx() else 0f }
    val weight = (fontWeight ?: style.fontWeight ?: FontWeight.Normal).weight
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val rendered by produceState<CharSequence>(text, text, textSize, textColor, width) {
            value = text
            val builder = SpannableStringBuilder()
            for ((index, segment) in segments.withIndex()) {
                if (segment is LatexSegment.PlainText) {
                    builder.append(segment.text)
                    continue
                }
                segment as LatexSegment.Math
                val original = segment.raw
                val span = try {
                    val fontDp = textSize / density.density
                    val fullSize = RaTeXSpan.create(context, segment.latex, fontDp, textColor, segment.display)
                    val spanWidth = fullSize.getSize(Paint(), original, 0, original.length, null)
                    if (width > 0 && spanWidth > width) {
                        RaTeXSpan.create(context, segment.latex, fontDp * width / spanWidth * 0.98f, textColor, segment.display)
                    } else fullSize
                } catch (_: RaTeXException) {
                    null
                } catch (_: LinkageError) {
                    null
                }
                if (span == null) {
                    builder.append(original)
                } else {
                    if (segment.display && builder.isNotEmpty() && builder.last() != '\n') builder.append('\n')
                    val start = builder.length
                    builder.append(original.replace('\n', ' ').replace('\r', ' '))
                    builder.setSpan(span, start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val next = segments.getOrNull(index + 1)
                    if (segment.display && next != null &&
                        !(next is LatexSegment.PlainText && next.text.startsWith('\n'))
                    ) builder.append('\n')
                }
            }
            value = builder
        }
        AndroidView(
            factory = { TextView(it).apply { includeFontPadding = false } },
            update = { view ->
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                view.setTextColor(textColor)
                view.typeface = if (Build.VERSION.SDK_INT >= 28) {
                    Typeface.create(Typeface.DEFAULT, weight, false)
                } else {
                    Typeface.create(Typeface.DEFAULT, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
                }
                view.setLineSpacing((lineHeight - view.paint.fontSpacing).coerceAtLeast(0f), 1f)
                view.maxLines = maxLines
                view.ellipsize = if (overflow == TextOverflow.Ellipsis) TextUtils.TruncateAt.END else null
                view.text = rendered
            }
        )
    }
}

internal fun clearMathRenderCaches() = Unit
