package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun MultiBlankAnswerInputs(
    blankCount: Int,
    values: List<String>,
    enabled: Boolean,
    onValueChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(blankCount.coerceAtLeast(0)) { index ->
            OutlinedTextField(
                value = values.getOrNull(index).orEmpty(),
                onValueChange = { onValueChange(index, it) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("第${index + 1}空") },
                placeholder = { Text("请输入第${index + 1}空答案") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = if (index == blankCount - 1) ImeAction.Done else ImeAction.Next)
            )
        }
        Text(
            text = "请按题干中的题空顺序逐项填写。所有题空均正确时，本题才判定为正确。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MultiBlankAnswerEditor(
    blankAnswers: List<List<String>>,
    detectedBlankCount: Int,
    onChange: (List<List<String>>) -> Unit,
    onDisable: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "多空答案",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (detectedBlankCount > 0 && detectedBlankCount != blankAnswers.size) {
            NoticeCard(
                "题干检测到 ${detectedBlankCount} 个题空，当前配置了 ${blankAnswers.size} 组答案，请检查。",
                warning = true
            )
        }
        blankAnswers.forEachIndexed { blankIndex, answers ->
            GlassCard {
                Text(
                    text = "第${blankIndex + 1}空",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                val answerCount = answers.size.coerceAtLeast(3)
                repeat(answerCount) { answerIndex ->
                    OutlinedTextField(
                        value = answers.getOrNull(answerIndex).orEmpty(),
                        onValueChange = { value ->
                            val next = blankAnswers.map { it.toMutableList() }.toMutableList()
                            val group = MutableList(answerCount) { slot -> answers.getOrNull(slot).orEmpty() }
                            group[answerIndex] = value
                            next[blankIndex] = group
                            onChange(next.map { it.toList() })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                when (answerIndex) {
                                    0 -> "主答案"
                                    else -> "备选答案$answerIndex"
                                }
                            )
                        },
                        trailingIcon = if (answerIndex > 0) {
                            {
                                TextButton(onClick = {
                                    val next = blankAnswers.toMutableList()
                                    next[blankIndex] = answers.toMutableList().also { group ->
                                        if (answerIndex < group.size) group.removeAt(answerIndex)
                                    }
                                    onChange(next)
                                }) { Text("删除") }
                            }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    if (answerIndex < answerCount - 1) Spacer(Modifier.height(8.dp))
                }
                TextButton(onClick = {
                    val next = blankAnswers.toMutableList()
                    next[blankIndex] = List(answerCount) { answers.getOrNull(it).orEmpty() } + ""
                    onChange(next)
                }) { Text("增加备选答案") }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        enabled = blankIndex > 0,
                        onClick = {
                            val next = blankAnswers.toMutableList()
                            val item = next.removeAt(blankIndex)
                            next.add(blankIndex - 1, item)
                            onChange(next)
                        }
                    ) { Text("上移") }
                    TextButton(
                        enabled = blankIndex < blankAnswers.lastIndex,
                        onClick = {
                            val next = blankAnswers.toMutableList()
                            val item = next.removeAt(blankIndex)
                            next.add(blankIndex + 1, item)
                            onChange(next)
                        }
                    ) { Text("下移") }
                    TextButton(
                        onClick = {
                            val next = blankAnswers.toMutableList().also { it.removeAt(blankIndex) }
                            onChange(next)
                        }
                    ) { Text("删除此空") }
                }
            }
        }
        OutlinedButton(
            onClick = { onChange(blankAnswers + listOf(emptyList())) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("增加题空")
        }
        onDisable?.let { disable ->
            OutlinedButton(onClick = disable, modifier = Modifier.fillMaxWidth()) {
                Text("退出多空模式")
            }
        }
    }
}
