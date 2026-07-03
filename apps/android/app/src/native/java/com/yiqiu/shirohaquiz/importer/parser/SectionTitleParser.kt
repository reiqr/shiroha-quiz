package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.model.QuestionType

data class SectionInfo(
    val title: String,
    val forcedType: QuestionType? = null,
    val isAnswerSection: Boolean = false
)

object SectionTitleParser {
    private val chineseIndex = "[一二三四五六七八九十百]+"
    private val leadingIndexRegex = Regex("""^\s*(?:第?${chineseIndex}[、.．、]?|第?\d{1,3}[、.．、]?|\(\s*${chineseIndex}\s*\)|（\s*${chineseIndex}\s*）)?\s*""")
    private val arabicNumberedRemainderRegex = Regex("""^\s*\d{1,4}\s*[.、．:：)）]\s*(.+)$""")
    private val sectionLikeRegex = Regex(
        """^\s*(?:第[一二三四五六七八九十百0-9]+(?:部分|卷|章|节|模块)|[一二三四五六七八九十百]+[、.．]\s*(?:常识判断|言语理解|语言理解|数量关系|数学能力|数学运算|判断推理|图形推理|定义判断|类比推理|逻辑判断|资料分析|材料分析|综合知识|公共基础知识|专业知识|基础知识|安全知识|理论知识|综合能力|结构化面试|公考面试|公务员面试|面试真题).*)\s*$"""
    )
    private val nonTypeSectionKeywordRegex = Regex("""(部分|试卷|常识判断|言语理解|语言理解|数量关系|数学能力|数学运算|判断推理|图形推理|定义判断|类比推理|逻辑判断|资料分析|材料分析|综合知识|公共基础知识|专业知识|基础知识|安全知识|理论知识|综合能力|结构化面试|公考面试|公务员面试|面试真题)""")
    private val genericSectionHeadingRegex = Regex(
        """^\s*(?:[一二三四五六七八九十百]+|\d{1,3})[、.．]\s*(?:(?:边界|极端)|.*(?:测试区|样本|题库|格式|边界(?:测试|样本|用例|情况|场景)|极端(?:测试|样本|用例|情况|场景)|客观题|主观题|材料题|集中答案|AI\s*解析|AI\s*核对|功能测试).*)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val answerSectionRegex = Regex(
        """(集中答案|集中解析|参考答案|标准答案|正确答案|答案(?:与|及)?解析|答案解析|试题答案|答题要点|参考要点|参考思路|答题思路|作答思路|评分要点|答案区|解析区|答案部分)"""
    )
    private val exactAnswerSectionTitleRegex = Regex(
        """^\s*(?:[一二三四五六七八九十百]+|\d{1,3})?[、.．]?\s*(?:集中答案|集中解析|参考答案|标准答案|正确答案|答案(?:与|及)?解析|答案解析|试题答案|答题要点|参考要点|参考思路|答题思路|作答思路|评分要点|答案区|解析区|答案部分)\s*[:：]?\s*$"""
    )
    private val inlineAnalysisLineRegex = Regex(
        """^\s*(?:(?:[\[【]\s*(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*[\]】]\s*)|(?:(?:答案解析|解题思路|解析思路|解题分析|参考解析|详解|分析|理由|解答|解析|说明)\s*(?:[:：]|\s+)))\s*\S+"""
    )
    private val questionBlankMarkerRegex = Regex("""[（(]\s*[)）]""")
    private val questionPromptKeywordRegex = Regex(
        """(?:[?？]|(?:下列|以下|哪[一项种个]|哪个|哪种|哪项|不正确|不属于|正确|错误|属于|包括|主要|应当|应该|可以|能够|能否|是否|为什么|如何))"""
    )

    fun parse(rawLine: String): SectionInfo? {
        val title = rawLine.trim()
        if (title.isBlank()) return null
        if (title.length > 60) return null

        if (isExactAnswerSectionTitle(title)) return SectionInfo(title = title, isAnswerSection = true)
        if (looksLikeNumberedQuestionStemLine(title)) return null
        if (genericSectionHeadingRegex.matches(title)) return SectionInfo(title = title)
        if (isInlineAnalysisLine(title)) return null
        if (answerSectionRegex.containsMatchIn(title) && !isInlineAnswerLine(title)) {
            return SectionInfo(title = title, isAnswerSection = true)
        }
        if (looksLikeArabicNumberedTypedQuestionLine(title)) return null

        val simplified = leadingIndexRegex.replace(title, "")
            .replace(Regex("""[\s:：]+$"""), "")

        val leadingType = QuestionTypeLabelParser.extractLeading(simplified)
        if (leadingType != null) {
            return if (leadingType.remainder.isBlank() || looksLikeTypeSectionRemainder(leadingType.remainder)) {
                SectionInfo(title = title, forcedType = leadingType.type)
            } else {
                null
            }
        }

        val type = QuestionTypeLabelParser.parseLabel(simplified)
        if (type != null) return SectionInfo(title = title, forcedType = type)
        if (sectionLikeRegex.matches(title) && nonTypeSectionKeywordRegex.containsMatchIn(title)) {
            return SectionInfo(title = title)
        }
        return null
    }

    private fun looksLikeNumberedQuestionStemLine(title: String): Boolean {
        val rest = arabicNumberedRemainderRegex.find(title)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (rest.isBlank()) return false
        if (looksLikeArabicNumberedTypedQuestionLine(title)) return true
        if (questionBlankMarkerRegex.containsMatchIn(rest)) return true
        if (questionPromptKeywordRegex.containsMatchIn(rest) && !looksLikeShortGenericSectionTitle(rest)) return true
        return false
    }

    private fun looksLikeShortGenericSectionTitle(rest: String): Boolean {
        val text = rest.trim().trimEnd(':', '：')
        if (text.length > 16) return false
        return Regex(
            """^(?:(?:边界|极端)|.*(?:测试区|样本|题库|格式|边界(?:测试|样本|用例|情况|场景)|极端(?:测试|样本|用例|情况|场景)|客观题|主观题|材料题|集中答案|AI\s*解析|AI\s*核对|功能测试).*)$""",
            RegexOption.IGNORE_CASE
        ).matches(text)
    }

    private fun looksLikeTypeSectionRemainder(remainder: String): Boolean {
        val text = remainder.trim()
        if (text.isBlank()) return true
        val scoreOrCountKeywords = "题|小题|道|分|共|共计|每题|每小题|本题|本大题|本部分|满分"
        if (Regex("""^[（(].*(?:$scoreOrCountKeywords).*[）)]$""").containsMatchIn(text)) return true
        if (Regex("""^(?:共|共计|每题|每小题|本题|本大题|本部分|以下|含|满分)""").containsMatchIn(text)) return true
        return Regex("""^(?:[，,、；;]\s*)?(?:共|共计|每题|每小题|本题|本大题|本部分|以下|含|满分)""").containsMatchIn(text)
    }

    private fun isExactAnswerSectionTitle(title: String): Boolean {
        return exactAnswerSectionTitleRegex.matches(title.trim().trim('[', ']', '【', '】', '（', '）', '(', ')').trim())
    }

    private fun isInlineAnswerLine(title: String): Boolean {
        return Regex("""^\s*(?:[\[【]\s*)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)(?:\s*[\]】])?\s*(?:[:：]|为|\s+)\s*\S+""").containsMatchIn(title)
    }

    private fun isInlineAnalysisLine(title: String): Boolean {
        return inlineAnalysisLineRegex.containsMatchIn(title)
    }

    private fun looksLikeArabicNumberedTypedQuestionLine(title: String): Boolean {
        val rest = arabicNumberedRemainderRegex.find(title)?.groupValues?.getOrNull(1).orEmpty()
        return rest.isNotBlank() && QuestionTypeLabelParser.hasLeading(rest)
    }

    fun isSectionHeading(rawLine: String): Boolean = parse(rawLine) != null

    fun forcedTypeOf(rawLine: String): QuestionType? = parse(rawLine)?.forcedType

    fun isAnswerSectionHeading(rawLine: String): Boolean = parse(rawLine)?.isAnswerSection == true
}
