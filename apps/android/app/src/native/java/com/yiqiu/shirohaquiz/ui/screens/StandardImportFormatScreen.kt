package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun StandardImportFormatScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShirohaSpacing.Xl, vertical = ShirohaSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Format",
            title = "标准导入格式",
            subtitle = "按这个格式整理题库，识别会更稳定。"
        )


        FormatSection(
            title = "一、单文件标准格式",
            body = "每道题建议包含题号、题干、选项、答案和解析。题号可以用 1.、1、【1】 等形式，但同一份题库尽量统一。",
            sample = """
1. 下列哪一项是良好学习习惯？
A. 课前预习
B. 长期熬夜
C. 抄写答案
D. 不做复盘
答案：A
解析：课前预习有助于提前了解重点内容。
            """.trimIndent()
        )

        FormatSection(
            title = "二、多选题格式",
            body = "多选题答案可以写成 AB、A B、A、B 或 A/B。建议答案集中写在“答案：ACD”这一行。",
            sample = """
2. 整理题库时，哪些做法有助于提高识别稳定性？
A. 保留清晰题号
B. 把多道题挤在一行
C. 统一选项格式
D. 单独列出答案
答案：ACD
解析：题号、选项和答案越清晰，导入越稳定。
            """.trimIndent()
        )

        FormatSection(
            title = "三、判断题格式",
            body = "判断题可使用“正确/错误”“对/错”“√/×”。不建议把答案混在很长的题干中。",
            sample = """
3. 题库导入前，统一编号和选项格式可以减少识别错误。（ ）
答案：正确
解析：统一格式有利于解析器判断题目边界。
            """.trimIndent()
        )

        FormatSection(
            title = "四、双文件导入格式",
            body = "题目文件只放题干和选项，答案文件按题号列出答案。题号需要和题目文件对应。",
            sample = """
题目文件：
1. 示例题干一……
A. 选项一
B. 选项二

2. 示例题干二……
A. 选项一
B. 选项二
C. 选项三

答案文件：
1. B
2. AC
3. 正确
            """.trimIndent()
        )

        FormatSection(
            title = "五、减少识别错误的建议",
            body = "尽量避免把多个题目挤在一行；选项前保留 A. B. C. D.；答案区和解析区保持清晰。复杂整卷真题可以先导入，再进入核对页修正。",
            sample = null
        )

        FormatSection(
            title = "六、复杂格式可先用 AI 清洗",
            body = "如果来源材料包含复制错行、答案集中、解析混排、扫描文本或整卷说明，建议先发给常见 AI / LLM 清洗成标准格式，再导入 App。",
            sample = """
请把下面的待整理文本清洗为适合题库导入的标准格式。
要求如下：
1. 保留所有题目，不要漏题、不要编造题目。
2. 按原始顺序整理，每道题单独成段。
3. 每道题尽量整理为：题号、题干、选项、答案、解析。
4. 单选题答案格式统一为：答案：A
5. 多选题答案格式统一为：答案：ABCD 这种连续大写字母。
6. 判断题答案统一为：答案：正确 或 答案：错误。
7. 填空题、简答题请保留原答案，不要擅自改写。
8. 解析如果原文没有，就不要编造；可以留空或写“解析：”。
9. 遇到无法确认的题干、选项或答案，请标记【待确认】。
10. 最终只输出清洗后的题库正文，不要输出额外解释。

下面是原始文本：
【把需要清洗的题库粘贴到这里】
            """.trimIndent()
        )

        ActionPillButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            text = "返回设置",
            primary = false,
            modifier = Modifier.height(42.dp),
            onClick = onBack
        )
    }
}

@Composable
private fun FormatSection(
    title: String,
    body: String,
    sample: String?
) {
    GlassCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!sample.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            GlassCard {
                Text(
                    text = sample,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
