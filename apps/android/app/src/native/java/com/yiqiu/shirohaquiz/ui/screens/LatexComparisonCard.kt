package com.yiqiu.shirohaquiz.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import java.io.IOException

internal enum class LatexComparisonBank(val asset: String, val count: Int, val label: String) {
    Functional("01-latex-functional-14.json", 14, "功能验收 · 14 题"),
    Stress("02-latex-stress-50.json", 50, "连续切题 · 50 题")
}

internal fun startLatexComparison(context: Context, sample: LatexComparisonBank): Boolean {
    val raw = try {
        context.assets.open("latex-comparison/${sample.asset}").bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        return false
    }
    val fixture = QuizRepository.parseImportJsonBank(raw) ?: return false
    if (fixture.questions.size != sample.count) return false
    val group = "LaTeX 真机对照"
    val name = "内置测试｜${sample.label}"
    val bank = QuizRepository.banks.firstOrNull { it.groupName == group && it.name == name } ?: run {
        QuizRepository.importBank(context, name, fixture.questions, group)
        QuizRepository.banks.firstOrNull { it.groupName == group && it.name == name } ?: return false
    }
    QuizRepository.setActiveBank(context, bank.id)
    return QuizRepository.startPracticeSession(
        questionCount = sample.count,
        allowedTypes = fixture.questions.map { it.type }.toSet(),
        sourceQuestions = fixture.questions,
        sourceLabel = name,
        randomize = false
    )
}

@Composable
internal fun LatexComparisonCard(onGoPractice: () -> Unit) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    GlassCard {
        Column {
            Text("LaTeX 真机对照", style = MaterialTheme.typography.titleMedium)
            Text("点选后按固定题序开始新一轮，不用导入文件。", style = MaterialTheme.typography.bodySmall)
            LatexComparisonBank.entries.forEach { sample ->
                TextButton(onClick = {
                    failed = !startLatexComparison(context, sample)
                    if (!failed) onGoPractice()
                }) {
                    Text(sample.label)
                }
            }
            if (failed) {
                Text("未能载入内置题库。", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
