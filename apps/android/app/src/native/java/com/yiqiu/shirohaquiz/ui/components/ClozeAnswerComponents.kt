package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.importer.model.ClozeSupport
import com.yiqiu.shirohaquiz.importer.model.Question

/** 一篇文章一次展示，每个空位分别选择，不将多个答案合并成单选。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClozeAnswerInputs(
    question: Question,
    values: List<String>,
    enabled: Boolean,
    showCorrect: Boolean,
    onAnswer: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = ClozeSupport.padAnswers(values, question.clozeItems.size)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showCorrect && ClozeSupport.hasCompleteReference(question)) {
            Text(
                "本篇答对 ${ClozeSupport.correctCount(question, selected)} / ${question.clozeItems.size} 空",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        question.clozeItems.forEachIndexed { index, item ->
            Text("第 ${item.number} 空", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.options.forEach { option ->
                    val key = option.key.uppercase()
                    FilterChip(
                        selected = selected[index] == key,
                        onClick = { onAnswer(index, key) },
                        enabled = enabled,
                        label = { Text("${option.key}. ${option.text}") }
                    )
                }
            }
            if (showCorrect) {
                val answer = item.answer.ifBlank { "未识别" }
                val verdict = if (item.answer.isBlank()) "待核对" else if (selected[index].isBlank()) "未作答" else if (selected[index] == item.answer) "正确" else "错误"
                Text("正确答案：$answer · $verdict", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 题库核对界面：逐空修改选项文本、参考答案，而不是编辑合并后的单一答案串。 */
@Composable
fun ClozeAnswerEditor(question: Question, onChange: (Question) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        question.clozeItems.forEachIndexed { index, item ->
            Text("第 ${item.number} 空", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            item.options.forEachIndexed { optionIndex, option ->
                OutlinedTextField(
                    value = option.text,
                    onValueChange = { text ->
                        val nextOptions = item.options.mapIndexed { j, old -> if (j == optionIndex) old.copy(text = text) else old }
                        val next = question.clozeItems.mapIndexed { i, old -> if (i == index) old.copy(options = nextOptions) else old }
                        onChange(question.copy(clozeItems = next, answer = next.map { it.answer }))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${option.key} 选项内容") },
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))
            }
            OutlinedTextField(
                value = item.answer,
                onValueChange = { answer ->
                    val normalized = answer.trim().uppercase().take(1)
                    val next = question.clozeItems.mapIndexed { i, old -> if (i == index) old.copy(answer = normalized) else old }
                    onChange(question.copy(clozeItems = next, answer = next.map { it.answer }))
                },
                label = { Text("第 ${item.number} 空正确答案（A-D）") },
                singleLine = true
            )
        }
    }
}
