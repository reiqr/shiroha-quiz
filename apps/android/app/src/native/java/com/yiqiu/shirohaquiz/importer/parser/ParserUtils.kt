package com.yiqiu.shirohaquiz.importer.parser

internal fun normalizeQuestionIndex(raw: String): String {
    val clean = raw.trim()
    if (clean.all { it.isDigit() }) return clean
    return chineseNumberToInt(clean)?.toString().orEmpty()
}

internal fun chineseNumberToInt(raw: String): Int? {
    if (raw.isBlank()) return null
    val normalized = raw.trim().replace("零", "").replace("〇", "")
    if (normalized.isBlank()) return 0
    val digitMap = mapOf(
        '零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9
    )
    if ('百' in normalized) {
        val parts = normalized.split('百', limit = 2)
        val hundreds = parts.getOrNull(0)?.takeIf { it.isNotBlank() }?.let { digitMap[it.first()] } ?: 1
        val tail = parts.getOrNull(1).orEmpty()
        return hundreds * 100 + (chineseNumberToInt(tail) ?: 0)
    }
    if ('十' in normalized) {
        val parts = normalized.split('十', limit = 2)
        val tens = parts.getOrNull(0)?.takeIf { it.isNotBlank() }?.let { digitMap[it.first()] } ?: 1
        val ones = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { digitMap[it.first()] } ?: 0
        return tens * 10 + ones
    }
    return digitMap[normalized.first()]
}

private val binaryJudgeStrongPromptRegex = Regex("""(判断|判定|正误|对错|是非|是否|能否|可否|正确与否|错误与否|对还是错|对不对|错不错|正确吗|错误吗)""")
private val binaryOptionChoicePromptRegex = Regex("""(下列|以下|哪(?:一)?(?:项|个|些|种|条)|选择|选出|应选|正确的是|错误的是|不正确的是|不属于|属于|包括|不包括|符合|不符合|最佳|最合适|最恰当)""")
private val binaryJudgeLoosePromptRegex = Regex("""(判断|正确|错误|对错|是非|是否|正误|√|✓|✔|☑|×|✗|✖)""")
private val judgeOptionTextSet = setOf("正确", "错误", "对", "错", "是", "否", "√", "✓", "✔", "☑", "×", "✗", "✖", "❌", "True", "False", "true", "false")

internal fun isJudgeOptionPair(optionKeys: List<String>, optionTexts: List<String>): Boolean {
    return optionKeys.map { it.uppercase() } == listOf("A", "B") &&
        optionTexts.map { it.trim() }.all { it in judgeOptionTextSet }
}

internal fun shouldInferJudgeFromBinaryOptions(stem: String, answerText: String = ""): Boolean {
    val cleanStem = stem.trim()
    if (binaryJudgeStrongPromptRegex.containsMatchIn(cleanStem)) return true
    if (binaryOptionChoicePromptRegex.containsMatchIn(cleanStem)) return false
    return binaryJudgeLoosePromptRegex.containsMatchIn(cleanStem + answerText)
}

