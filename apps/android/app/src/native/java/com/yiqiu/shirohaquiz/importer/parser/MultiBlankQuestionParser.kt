package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.MultiBlankSupport
import com.yiqiu.shirohaquiz.importer.model.Option
import com.yiqiu.shirohaquiz.importer.model.QuestionType

data class MultiBlankParseResult(
    val questionText: String,
    val blankAnswers: List<List<String>> = emptyList(),
    val answer: List<String>,
    val warnings: List<String> = emptyList()
)

object MultiBlankQuestionParser {
    private val filledBracketRegex = Regex("""([（(])\s*([^()（）\r\n]{1,80}?)\s*([）)])""")
    private val fillIntentRegex = Regex("""(填空|填入|补全|补充完整|括号内|括号里|空白处|横线上)""")
    private val filledBlankPrefixRegex = Regex("""(?:为|是|有|包括|分别为|称为|叫做|等于|达到|取值为|填入|填写)\s*$""")
    private val objectiveLetterRegex = Regex("""^[A-Ga-g]$""")
    private val likelyUnitOnlyRegex = Regex("""^(?:MPa|kPa|Pa|℃|°C|K|m|cm|mm|km|m/s|km/h|kg|g|mol|s|min|h|%|V|A)$""", RegexOption.IGNORE_CASE)

    fun extract(
        stem: String,
        type: QuestionType,
        options: List<Option>,
        answerText: String,
        normalizedAnswer: List<String>
    ): MultiBlankParseResult {
        if (type != QuestionType.BLANK || options.isNotEmpty()) {
            return MultiBlankParseResult(stem, answer = normalizedAnswer)
        }

        val explicitBlankCount = MultiBlankSupport.countExplicitBlanks(stem)
        if (explicitBlankCount > 1) {
            if (answerText.isBlank()) {
                return MultiBlankParseResult(
                    questionText = stem,
                    blankAnswers = List(explicitBlankCount) { emptyList() },
                    answer = emptyList(),
                    warnings = listOf("检测到${explicitBlankCount}个题空，未识别逐空答案，请人工核对")
                )
            }
            val parts = MultiBlankSupport.splitReliableParts(answerText, explicitBlankCount)
            return if (parts != null) {
                val groups = parts.map { listOf(it) }
                MultiBlankParseResult(
                    questionText = stem,
                    blankAnswers = groups,
                    answer = MultiBlankSupport.compatibilityAnswer(groups)
                )
            } else {
                MultiBlankParseResult(
                    questionText = stem,
                    answer = normalizedAnswer,
                    warnings = listOf("检测到${explicitBlankCount}个题空，答案数量无法对应，请人工核对")
                )
            }
        }

        if (explicitBlankCount > 0) {
            return MultiBlankParseResult(stem, answer = normalizedAnswer)
        }

        val matches = filledBracketRegex.findAll(stem).filter { match ->
            val content = match.groupValues[2].trim()
            isPlausibleFilledBlank(content)
        }.toList()
        if (matches.size < 2) return MultiBlankParseResult(stem, answer = normalizedAnswer)

        val answerParts = MultiBlankSupport.splitReliableParts(answerText, matches.size)
        val hasStrongIntent = fillIntentRegex.containsMatchIn(stem)
        val repeatedStructure = matches.size >= 3 && matches.count { match ->
            val prefix = stem.substring(0, match.range.first).takeLast(16)
            filledBlankPrefixRegex.containsMatchIn(prefix)
        } >= 2
        val answerMatches = answerParts != null && answerParts.zip(matches).all { (answer, match) ->
            normalizeComparable(answer) == normalizeComparable(match.groupValues[2])
        }
        if (!hasStrongIntent && !repeatedStructure && !answerMatches) {
            return MultiBlankParseResult(stem, answer = normalizedAnswer)
        }

        val groups = matches.mapIndexed { index, match ->
            buildList {
                add(match.groupValues[2].trim())
                answerParts?.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            }.distinct()
        }
        val cleanStem = replaceMatchesWithBlanks(stem, matches)
        return MultiBlankParseResult(
            questionText = cleanStem,
            blankAnswers = groups,
            answer = MultiBlankSupport.compatibilityAnswer(groups)
        )
    }

    private fun isPlausibleFilledBlank(content: String): Boolean {
        if (content.isBlank() || content.length > 60) return false
        if (objectiveLetterRegex.matches(content)) return false
        if (likelyUnitOnlyRegex.matches(content)) return false
        if (content.count { it == '。' || it == '；' || it == ';' } > 0) return false
        return content.any { it.isLetterOrDigit() || it.code > 127 }
    }

    private fun replaceMatchesWithBlanks(text: String, matches: List<MatchResult>): String {
        var result = text
        matches.asReversed().forEach { match ->
            val replacement = if (match.groupValues[1] == "（") "（ ）" else "( )"
            result = result.replaceRange(match.range, replacement)
        }
        return result
    }

    private fun normalizeComparable(value: String): String {
        return value.trim().lowercase().replace(Regex("""[\s　]+"""), "")
    }
}
