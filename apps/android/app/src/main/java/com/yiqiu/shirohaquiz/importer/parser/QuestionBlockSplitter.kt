package com.yiqiu.shirohaquiz.importer.parser

data class QuestionBlock(
    val number: String,
    val lines: List<String>
)

object QuestionBlockSplitter {
    private val questionStartRegex = Regex("""^\s*(\d{1,4})\s*[.:．、]\s*(.*)$""")

    fun split(text: String): List<QuestionBlock> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val blocks = mutableListOf<QuestionBlock>()
        var currentNumber: String? = null
        var currentLines = mutableListOf<String>()

        fun flush() {
            val number = currentNumber ?: return
            if (currentLines.isNotEmpty()) {
                blocks += QuestionBlock(number, currentLines.toList())
            }
            currentNumber = null
            currentLines = mutableListOf()
        }

        lines.forEach { line ->
            val match = questionStartRegex.find(line)
            if (match != null) {
                flush()
                currentNumber = match.groupValues[1]
                val rest = match.groupValues[2].trim()
                if (rest.isNotEmpty()) currentLines += rest
            } else if (currentNumber != null) {
                currentLines += line
            }
        }
        flush()
        return blocks
    }
}
