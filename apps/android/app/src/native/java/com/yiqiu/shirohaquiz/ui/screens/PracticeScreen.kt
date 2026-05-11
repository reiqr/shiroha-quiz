package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionCard
import com.yiqiu.shirohaquiz.ui.components.QuestionImagesBlock
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun PracticeScreen() {
    val bank = QuizRepository.activeBank()
    val question = QuizRepository.currentPracticeQuestion()
    val result = QuizRepository.practiceLastResult

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(ShirohaSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Practice",
            title = "原生练习页",
            subtitle = if (bank == null || bank.questions.isEmpty()) {
                "当前还没有可练习的题目。先去导入页导入一份原生题库。"
            } else {
                "当前题库：${bank.name}，共 ${bank.questions.size} 题。练习提交后会自动写入错题本和学习记录。"
            }
        )

        if (question == null) {
            GlassCard {
                NoticeCard("还没有可练习题目。请先在原生导入页完成一次标准题库导入。")
            }
            return
        }

        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("第 ${QuizRepository.practiceIndex + 1} 题", selected = true)
                StatusChip(typeLabel(question.type))
                StatusChip("原生题库")
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (question.images.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                QuestionImagesBlock(question.images, maxPreviewHeight = 360.dp, showMeta = true)
            }
            Spacer(Modifier.height(18.dp))

            when (question.type) {
                QuestionType.SINGLE,
                QuestionType.MULTIPLE,
                QuestionType.JUDGE -> {
                    question.options.forEach { option ->
                        QuizOptionCard(
                            label = option.key,
                            text = option.text,
                            selected = QuizRepository.selectedAnswer.contains(option.key),
                            onClick = {
                                QuizRepository.toggleAnswer(
                                    key = option.key,
                                    multiple = question.type == QuestionType.MULTIPLE
                                )
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                QuestionType.BLANK,
                QuestionType.SHORT -> {
                    NoticeCard("这道题属于 ${typeLabel(question.type)}，当前练习页先展示参考答案和解析，后续再补原生主观题作答。")
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "上一题",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.previousQuestion() }
                )
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    "下一题",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.nextQuestion() }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    Icons.Rounded.CheckCircle,
                    "提交答案",
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = { QuizRepository.submitPracticeQuestion() }
                )
                ActionPillButton(
                    Icons.AutoMirrored.Rounded.TextSnippet,
                    "查看解析",
                    primary = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    fillWidthContent = true,
                    onClick = {
                        if (QuizRepository.practiceLastResult == null) {
                            QuizRepository.submitPracticeQuestion()
                        }
                    }
                )
            }

            if (result != null) {
                Spacer(Modifier.height(16.dp))
                NoticeCard(
                    text = if (result.correct) "回答正确" else "回答错误",
                    warning = !result.correct
                )
                Spacer(Modifier.height(8.dp))
                NoticeCard("正确答案：${result.answerText}", warning = false)
                if (question.analysis.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "解析：${question.analysis}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun typeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE -> "单选题"
    QuestionType.MULTIPLE -> "多选题"
    QuestionType.JUDGE -> "判断题"
    QuestionType.BLANK -> "填空题"
    QuestionType.SHORT -> "简答题"
}
