package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.yiqiu.shirohaquiz.ui.text.LatexDisplayFormatter

/**
 * Lite 构建（-PnoRatex）使用的纯文本实现：公式回退为 [LatexDisplayFormatter] 的符号化文本。
 * 与 src/ratex 下的 RaTeX 版保持相同签名，构建时二选一。
 */
@Composable
internal fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    plainText: (String) -> String = LatexDisplayFormatter::format
) {
    Text(
        text = plainText(text),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}
