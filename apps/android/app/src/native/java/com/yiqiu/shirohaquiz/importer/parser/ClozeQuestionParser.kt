package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.ClozeItem
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionType

/**
 * 保守识别英语完形填空：有明确的完形标题、文章空位、至少两组独立的 A-D 选项。
 * 不改写普通选择题或阅读理解。无法确定的答案留空，并给出警告。
 */
object ClozeQuestionParser {
    data class Extraction(val remainingText: String, val questions: List<Question>)

    private val heading = Regex(
        """^\s*(?:(?:第?[一二三四五六七八九十0-9]+)\s*[、.．:：)]\s*)?(?:英语)?(?:完形填空|完型填空|Cloze\s*(?:Test|Passage)?)(?:\s*[（(].*[)）])?\s*[:：.]?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val nextSection = Regex(
        """^\s*(?:(?:第?[一二三四五六七八九十0-9]+)\s*[、.．:：)]\s*)?(?:阅读理解|单项选择|单选题|多项选择|多选题|判断题|填空题|简答题|书面表达|作文|翻译题|任务型阅读|Reading\s+Comprehension)(?:\s*[（(].*[)）])?\s*[:：.]?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val optionGroup = Regex("""^\s*(?:[（(]\s*)?(\d{1,3})\s*(?:[）).、:：]|\s+)\s*(.+)$""")
    private val optionMarker = Regex("""(?<![A-Za-z0-9_])([A-Da-d])\s*[.．、:：)）]\s*""")
    private val explicitAnswer = Regex("""^\s*(?:参考答案|正确答案|标准答案|答案|Answer(?:s|\s+Key)?)\s*[:：]?\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val numberedAnswer = Regex("""(?<![0-9A-Za-z])(?:第\s*)?(\d{1,3})\s*(?:题)?\s*[.、:：)）=-]\s*([A-Da-d])\b""")
    private val numericBlank = Regex("""(?<![A-Za-z0-9])(?:[（(]\s*\d{1,3}\s*[)）]|\d{1,3}\s*[_＿]{2,}|[_＿]{2,}\s*\d{1,3})""")
    private val underlineBlank = Regex("""[_＿]{3,}""")
    private val numberedBlank = Regex("""(?<![A-Za-z0-9])[（(]\s*(\d{1,3})\s*[)）]|(?<![A-Za-z0-9])(\d{1,3})\s*[_＿]{2,}""")

    fun extract(text: String, separateAnswerText: String = ""): Extraction {
        if (text.isBlank()) return Extraction(text, emptyList())
        val lines = text.lines()
        val consumed = BooleanArray(lines.size)
        val questions = mutableListOf<Question>()
        var cursor = 0
        while (cursor < lines.size) {
            if (!heading.matches(lines[cursor].trim())) {
                cursor++
                continue
            }
            val begin = cursor
            var end = begin + 1
            while (end < lines.size && !nextSection.matches(lines[end].trim()) && !heading.matches(lines[end].trim())) {
                end++
            }
            // 同一节末尾如果没有下一节标题，允许末尾出现连续的完整答案表。
            val parsed = parseBlock(lines.subList(begin, end), separateAnswerText)
            if (parsed != null) {
                questions += parsed
                for (index in begin until end) consumed[index] = true
            }
            cursor = end
        }
        return Extraction(
            remainingText = lines.filterIndexed { index, _ -> !consumed[index] }.joinToString("\n").trim(),
            questions = questions
        )
    }

    private fun parseBlock(lines: List<String>, separateAnswerText: String): Question? {
        val first = lines.firstOrNull() ?: return null
        val groups = linkedMapOf<Int, List<Option>>()
        var optionsStart = -1
        val answerLines = mutableListOf<String>()
        var inAnswer = false
        var row = 1
        while (row < lines.size) {
            val line = lines[row].trim()
            val answerHead = explicitAnswer.find(line)
            if (answerHead != null) {
                inAnswer = true
                answerLines += answerHead.groupValues[1]
                row++
                continue
            }
            if (inAnswer) {
                answerLines += line
                row++
                continue
            }
            val groupHead = optionGroup.find(line)
            if (groupHead != null) {
                val number = groupHead.groupValues[1].toIntOrNull()
                val raw = groupHead.groupValues[2]
                val inlineOptions = parseOptions(raw)
                if (number != null && inlineOptions.size in 3..4 && groups[number] == null) {
                    if (optionsStart < 0) optionsStart = row
                    groups[number] = listOf("A", "B", "C", "D").map { key ->
                        inlineOptions.firstOrNull { it.key == key } ?: Option(key, "")
                    }
                    row++
                    continue
                }
                // 部分试卷将 A/B/C/D 四项分别放在四行。
                if (number != null && groups[number] == null) {
                    val optionLines = (row + 1 until minOf(row + 5, lines.size)).map { lines[it].trim() }
                    val verticalOptions = optionLines.flatMap(::parseOptions)
                    if (verticalOptions.size == 4 && verticalOptions.map { it.key } == listOf("A", "B", "C", "D")) {
                        if (optionsStart < 0) optionsStart = row
                        groups[number] = verticalOptions
                        row += 5
                        continue
                    }
                }
            }
            row++
        }
        if (groups.size < 2 || optionsStart <= 1) return null
        val numbered = groups.keys.toList()
        if (numbered != numbered.sorted()) return null
        val article = lines.subList(1, optionsStart).joinToString("\n").trim()
        if (article.length < 20) return null
        val markerCount = numericBlank.findAll(article).count()
        val genericCount = underlineBlank.findAll(article).count()
        // 有明确题空证据才能认定为完形，避免仅凭选项数量误识别阅读理解。
        if (markerCount < 2 && genericCount < 2) return null

        val localAnswers = parseAnswerMap(answerLines.joinToString("\n"), numbered)
        val externalAnswers = parseAnswerMap(separateAnswerText, numbered)
        val answers = localAnswers + externalAnswers.filterKeys { it !in localAnswers }
        // 若文章明确写了 (1)…(20)，缺失的选项组也要保留，不能把后续答案前移。
        val articleNumbers = numberedBlank.findAll(article).mapNotNull { match ->
            match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull()
        }.toList()
        val expectedNumbers = if (articleNumbers.size >= 2 &&
            articleNumbers.distinct().size == articleNumbers.size &&
            articleNumbers == articleNumbers.sorted() &&
            articleNumbers.containsAll(numbered)) {
            articleNumbers
        } else numbered
        val items = expectedNumbers.map { number ->
            val options = groups[number] ?: listOf("A", "B", "C", "D").map { Option(it, "") }
            ClozeItem(number, options, answers[number].orEmpty())
        }
        val warnings = mutableListOf<String>()
        if (items.size != groups.size) {
            warnings += "完形填空文章有${items.size}处题空，但仅识别到${groups.size}组选项；缺少的空位已保留，请补全选项"
        }
        if (markerCount > 0 && markerCount != items.size && genericCount != items.size) {
            warnings += "完形填空文章检测到${markerCount}处编号空位，但当前有${items.size}个题空，请人工核对"
        }
        val missing = items.count { it.answer.isBlank() }
        if (missing > 0) warnings += "完形填空共${items.size}空，其中${missing}空未匹配到答案，请人工核对"
        return Question(
            number = "${expectedNumbers.first()}-${expectedNumbers.last()}",
            type = QuestionType.CLOZE,
            question = article,
            answer = items.map { it.answer },
            clozeItems = items,
            category = first.trim(),
            warnings = warnings
        )
    }

    private fun parseOptions(text: String): List<Option> {
        val markers = optionMarker.findAll(text).toList()
        if (markers.isEmpty()) return emptyList()
        return markers.mapIndexedNotNull { index, marker ->
            val next = markers.getOrNull(index + 1)?.range?.first ?: text.length
            val value = text.substring(marker.range.last + 1, next).trim()
            if (value.isBlank()) null else Option(marker.groupValues[1].uppercase(), value)
        }.takeIf { it.map(Option::key).distinct().size == it.size }.orEmpty()
    }

    private fun parseAnswerMap(text: String, numbers: List<Int>): Map<Int, String> {
        if (text.isBlank()) return emptyMap()
        val matches = numberedAnswer.findAll(text).mapNotNull { m ->
            val number = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            number to m.groupValues[2].uppercase()
        }.filter { it.first in numbers }.toList()
        if (matches.isNotEmpty()) return matches.toMap()
        val payload = text.trim().replace(Regex("""^(?:答案|参考答案|标准答案|正确答案|Answers?|Answer\s+Key)\s*[:：]?\s*""", RegexOption.IGNORE_CASE), "")
        val lettersOnly = payload.replace(Regex("""[\s,，、;；/|]+"""), "").uppercase()
        if (lettersOnly.length == numbers.size && lettersOnly.all { it in 'A'..'D' }) {
            return numbers.zip(lettersOnly.map(Char::toString)).toMap()
        }
        return emptyMap()
    }
}
