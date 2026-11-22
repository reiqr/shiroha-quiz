package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.yiqiu.shirohaquiz.ui.text.MathHeightCache
import com.yiqiu.shirohaquiz.ui.text.MathRichText
import com.yiqiu.shirohaquiz.ui.text.MathWebViewPool

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
    MathRichText(text, modifier = modifier, style = style, color = color,
        fontWeight = fontWeight, maxLines = maxLines, overflow = overflow)
}

internal fun clearMathRenderCaches() {
    MathWebViewPool.clear()
    MathHeightCache.clear()
}
