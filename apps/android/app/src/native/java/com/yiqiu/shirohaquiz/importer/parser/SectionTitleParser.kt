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
    private val sectionLikeRegex = Regex(
        """^\s*(?:第[一二三四五六七八九十百0-9]+(?:部分|卷|章|节|模块)|[一二三四五六七八九十百]+[、.．]\s*(?:常识判断|言语理解|语言理解|数量关系|数学能力|数学运算|判断推理|图形推理|定义判断|类比推理|逻辑判断|资料分析|材料分析|综合知识|公共基础知识|专业知识|基础知识|安全知识|理论知识|综合能力|结构化面试|公考面试|公务员面试|面试真题).*)\s*$"""
    )
    private val nonTypeSectionKeywordRegex = Regex("""(部分|试卷|常识判断|言语理解|语言理解|数量关系|数学能力|数学运算|判断推理|图形推理|定义判断|类比推理|逻辑判断|资料分析|材料分析|综合知识|公共基础知识|专业知识|基础知识|安全知识|理论知识|综合能力|结构化面试|公考面试|公务员面试|面试真题)""")
    private val genericSectionHeadingRegex = Regex(
        """^\s*(?:[一二三四五六七八九十百]+|\d{1,3})[、.．]\s*(?:.*(?:测试区|样本|题库|格式|边界|极端|客观题|主观题|材料题|集中答案|AI\s*解析|AI\s*核对|功能测试).*)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val answerSectionRegex = Regex(
        """(集中答案|集中解析|参考答案|标准答案|正确答案|答案(?:与|及)?解析|答案解析|试题答案|答题要点|参考要点|参考思路|答题思路|作答思路|评分要点|答案区|解析区|答案部分)"""
    )

    fun parse(rawLine: String): SectionInfo? {
        val title = rawLine.trim()
        if (title.isBlank()) return null
        if (title.length > 60) return null

        if (genericSectionHeadingRegex.matches(title)) return SectionInfo(title = title)
        if (answerSectionRegex.containsMatchIn(title) && !isInlineAnswerLine(title)) {
            return SectionInfo(title = title, isAnswerSection = true)
        }
        if (looksLikeNumberedQuestionTypeLine(title)) return null

        val simplified = leadingIndexRegex.replace(title, "")
            .replace(Regex("""[\s:：]+$"""), "")

        val type = when {
            Regex("""^(?:单项选择题?|单选题?|选择题)""").containsMatchIn(simplified) -> QuestionType.SINGLE
            Regex("""^(?:多项选择题?|多选题?|不定项选择题?)""").containsMatchIn(simplified) -> QuestionType.MULTIPLE
            Regex("""^(?:判断题?|是非题|对错题)""").containsMatchIn(simplified) -> QuestionType.JUDGE
            Regex("""^(?:填空题?|补全题?)""").containsMatchIn(simplified) -> QuestionType.BLANK
            Regex("""^(?:简答题?|问答题?|面试题?|结构化面试题?|公考面试题?|公务员面试题?|名词解释|论述题?|案例分析题?|综合题)""").containsMatchIn(simplified) -> QuestionType.SHORT
            else -> null
        }

        if (type != null) return SectionInfo(title = title, forcedType = type)
        if (sectionLikeRegex.matches(title) && nonTypeSectionKeywordRegex.containsMatchIn(title)) {
            return SectionInfo(title = title)
        }
        return null
    }

    private fun isInlineAnswerLine(title: String): Boolean {
        return Regex("""^\s*(?:[\[【]\s*)?(?:答案|正确答案|参考答案|标准答案|参考要点|参考思路|答题要点|答题思路|作答思路|评分要点|参考作答|答)(?:\s*[\]】])?\s*[:：]?\s*\S+""").containsMatchIn(title)
    }

    private fun looksLikeNumberedQuestionTypeLine(title: String): Boolean {
        return Regex("""^\s*\d{1,4}\s*[.、．:：)）]\s*(?:【\s*)?(?:单项选择题?|单选题?|选择题|多项选择题?|多选题?|不定项选择题?|判断题?|是非题|对错题|填空题?|补全题?|简答题?|问答题?|面试题?|结构化面试题?|公考面试题?|公务员面试题?|材料分析题?|案例分析题?|论述题?|综合题)(?:\s*】)?\s*$""").containsMatchIn(title)
    }

    fun isSectionHeading(rawLine: String): Boolean = parse(rawLine) != null

    fun forcedTypeOf(rawLine: String): QuestionType? = parse(rawLine)?.forcedType

    fun isAnswerSectionHeading(rawLine: String): Boolean = parse(rawLine)?.isAnswerSection == true
}
