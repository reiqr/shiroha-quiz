package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.ai.ShirohaAiClient
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiAnalysisFillPanel(
    question: Question,
    currentAnalysis: String,
    onApplyAnalysis: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = QuizRepository.aiAnalysisEnabled
) {
    if (!enabled) return

    val scope = rememberCoroutineScope()
    var loading by remember(question.id, currentAnalysis) { mutableStateOf(false) }
    var suggestion by remember(question.id) { mutableStateOf<String?>(null) }
    var errorText by remember(question.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionPillButton(
            icon = Icons.Rounded.AutoAwesome,
            text = when {
                loading -> "AI 解析生成中"
                currentAnalysis.trim().isBlank() -> "AI 补解析"
                else -> "AI 优化解析"
            },
            primary = false,
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            fillWidthContent = true,
            onClick = {
                val apiBaseUrl = QuizRepository.aiApiBaseUrl
                val apiKey = QuizRepository.aiApiKey
                val modelName = QuizRepository.aiModelName
                val draftQuestion = question.copy(analysis = currentAnalysis.trim())
                when {
                    apiBaseUrl.isBlank() || apiKey.isBlank() || modelName.isBlank() -> {
                        errorText = "请先在 我的 → AI 设置 中配置接口。"
                    }
                    draftQuestion.answer.isEmpty() -> {
                        errorText = "请先填写答案，再生成解析。"
                    }
                    draftQuestion.question.trim().isBlank() -> {
                        errorText = "请先填写题干，再生成解析。"
                    }
                    else -> {
                        loading = true
                        errorText = null
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    ShirohaAiClient.generateAnalysis(
                                        apiBaseUrl = apiBaseUrl,
                                        apiKey = apiKey,
                                        modelName = modelName,
                                        questions = listOf(draftQuestion),
                                        timeoutSeconds = QuizRepository.aiTimeoutSeconds
                                    ).firstOrNull()
                                }
                            }
                            result.onSuccess { item ->
                                val text = item?.analysis.orEmpty().trim()
                                if (text.isBlank()) {
                                    errorText = "AI 没有返回可用解析。"
                                } else {
                                    suggestion = text
                                }
                            }.onFailure { error ->
                                errorText = error.message ?: "请检查接口配置或网络。"
                            }
                            loading = false
                        }
                    }
                }
            }
        )

        if (loading) {
            NoticeCard("AI 正在根据题干、选项和答案生成解析建议，请稍候。", warning = false)
        }
        errorText?.takeIf { it.isNotBlank() }?.let { message ->
            NoticeCard("AI 补解析失败：$message", warning = true)
        }
        suggestion?.takeIf { it.isNotBlank() }?.let { text ->
            AiAnalysisSuggestionCard(
                analysis = text,
                onApply = {
                    onApplyAnalysis(text)
                    errorText = null
                }
            )
        }
    }
}

@Composable
private fun AiAnalysisSuggestionCard(
    analysis: String,
    onApply: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShirohaRadius.Lg),
        color = ShirohaColors.CardWhite68,
        border = BorderStroke(ShirohaDimens.Hairline, ShirohaColors.LineSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI 建议解析",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = analysis,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "AI 结果只写入当前编辑框，仍需手动保存题目。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ActionPillButton(
                    icon = Icons.Rounded.CheckCircle,
                    text = "采纳到解析",
                    primary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    fillWidthContent = true,
                    onClick = onApply
                )
            }
        }
    }
}
