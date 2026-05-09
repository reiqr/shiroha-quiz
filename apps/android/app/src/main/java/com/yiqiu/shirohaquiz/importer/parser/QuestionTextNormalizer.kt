package com.yiqiu.shirohaquiz.importer.parser

object QuestionTextNormalizer {
    fun normalize(raw: String): String {
        return raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u3000', ' ')
            .replace('\t', ' ')
            .replace("（", "(")
            .replace("）", ")")
            .replace("：", ":")
            .replace("．", ".")
            .replace("、", ".")
            .lineSequence()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
