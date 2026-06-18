package com.yiqiu.shirohaquiz.importer.parser

import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker

object CompactQuestionRepair {
    private data class OptionMarker(
        val key: Char,
        val markerStart: Int,
        val contentStart: Int,
        val joinedInsideAsciiWord: Boolean = false
    )

    private data class ProtectedText(
        val text: String,
        val replacements: Map<String, String>
    )

    private val inlineAnswerRegex = Regex("""\S\s+(?:[【\[]?\s*(?:答案|正确答案|参考答案|标准答案)\s*[:：])""")
    private val inlineAnalysisRegex = Regex("""\S\s+(?:[【\[]?\s*(?:解析|答案解析|说明)\s*[:：])""")
    private val punctuatedMarkerRegex = Regex("""([A-Ga-g])\s*[.、．:：)）]""")
    private val bracketMarkerRegex = Regex("""[\(（\[【〔〖《]\s*([A-Ga-g])\s*[\)）\]】〕〗》]""")

    /**
     * 紧凑排版只作为兜底候选：必须存在至少两个连续、可解释的选项标记。
     * 单个 A./B.，英文缩写、文件名、网址内部的字母点号都不会触发。
     */
    fun hasCompactPattern(raw: String): Boolean {
        if (raw.isBlank()) return false
        return raw.lineSequence().any { rawLine ->
            val line = rawLine.trim()
            line.length >= 4 && (
                findBestCompactOptionRun(protectImageMarkers(line).text) != null ||
                    inlineAnswerRegex.containsMatchIn(line) ||
                    inlineAnalysisRegex.containsMatchIn(line)
                )
        }
    }

    /** 供题块拆分器判断“这一行是否真的包含紧凑选项组”。 */
    fun hasCompactOptionSequence(line: String): Boolean {
        if (line.isBlank()) return false
        return findBestCompactOptionRun(protectImageMarkers(line.trim()).text) != null
    }

    /** 返回紧凑选项组中实际识别到的字母，供单题局部兜底做结构校验。 */
    internal fun compactOptionKeys(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        val keys = linkedSetOf<String>()
        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach
            val run = findBestCompactOptionRun(protectImageMarkers(line).text) ?: return@forEach
            run.forEach { marker -> keys += marker.key.uppercaseChar().toString() }
        }
        return keys
    }

    /** 标准格式只认行首独立选项；英文缩写如 E.U / D.H. 不算。 */
    fun isStandardOptionLine(line: String): Boolean {
        val normalized = line.trimStart()
        val marker = findLeadingMarker(normalized) ?: return false
        return !looksLikeDottedAbbreviation(normalized, marker) &&
            !CodeLikeTextGuard.looksLikeLeadingCodeOption(normalized, marker.markerStart, marker.contentStart)
    }

    fun repair(raw: String): String {
        if (raw.isBlank()) return raw
        if (!hasCompactPattern(raw)) return raw
        return raw.lineSequence()
            .flatMap { repairLine(it).lineSequence() }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun repairLine(rawLine: String): String {
        val original = rawLine.trimEnd()
        if (original.isBlank()) return original

        val protected = protectImageMarkers(original)
        val optionRun = findBestCompactOptionRun(protected.text)
        var repaired = if (optionRun == null) {
            protected.text
        } else {
            splitByOptionRun(protected.text, optionRun)
        }

        // 答案、解析粘在题干或最后一个选项后时再做轻量换行。
        repaired = repaired
            .replace(
                Regex("""[ \t]+([【\[]?\s*(?:答案|正确答案|参考答案|标准答案)\s*[:：])"""),
                "\n$1"
            )
            .replace(
                Regex("""[ \t]+([【\[]?\s*(?:解析|答案解析|说明)\s*[:：])"""),
                "\n$1"
            )

        return restoreImageMarkers(repaired, protected)
    }

    private fun splitByOptionRun(line: String, markers: List<OptionMarker>): String {
        val pieces = mutableListOf<String>()
        val prefix = line.substring(0, markers.first().markerStart).trim()
        if (prefix.isNotBlank()) pieces += prefix

        markers.forEachIndexed { index, marker ->
            val end = markers.getOrNull(index + 1)?.markerStart ?: line.length
            val optionChunk = line.substring(marker.markerStart, end).trim()
            if (optionChunk.isNotBlank()) pieces += optionChunk
        }
        return pieces.joinToString("\n")
    }

    private fun findBestCompactOptionRun(line: String): List<OptionMarker>? {
        val candidates = findCandidateMarkers(line)
        if (candidates.size < 2) return null

        val runs = mutableListOf<List<OptionMarker>>()
        for (startIndex in candidates.indices) {
            val run = mutableListOf(candidates[startIndex])
            var expected = keyIndex(candidates[startIndex].key) + 1
            var cursor = startIndex + 1
            while (cursor < candidates.size && expected in 1..6) {
                val candidate = candidates[cursor]
                val index = keyIndex(candidate.key)
                if (index == expected) {
                    run += candidate
                    expected += 1
                    cursor += 1
                } else if (index <= expected - 1) {
                    break
                } else {
                    break
                }
            }
            if (isValidCompactRun(line, run)) runs += run
        }

        return runs.maxWithOrNull(
            compareBy<List<OptionMarker>> { it.size }
                .thenBy { if (it.first().key.uppercaseChar() == 'A') 1 else 0 }
                .thenBy { -it.first().markerStart }
        )
    }

    private fun isValidCompactRun(line: String, run: List<OptionMarker>): Boolean {
        if (run.size < 2) return false
        val first = run.first()
        val prefix = line.substring(0, first.markerStart).trim()

        // 题干后同行选项必须从 A 开始；从 B/C 开始只允许该行本身就是选项续行。
        if (prefix.isNotBlank() && first.key.uppercaseChar() != 'A') return false
        if (first.joinedInsideAsciiWord) return false

        val codeLikeChunks = run.count { marker ->
            val end = run.firstOrNull { it.markerStart > marker.markerStart }?.markerStart ?: line.length
            CodeLikeTextGuard.looksLikeCodeOptionChunk(
                line = line,
                markerStart = marker.markerStart,
                contentStart = marker.contentStart,
                endExclusive = end
            )
        }
        if (codeLikeChunks >= 2) return false

        run.forEachIndexed { index, marker ->
            if (marker.joinedInsideAsciiWord && index == 0) return false
            val end = run.getOrNull(index + 1)?.markerStart ?: line.length
            val content = line.substring(marker.contentStart, end)
                .trim()
                .trim(';', '；')
                .trim()
            if (content.isBlank()) return false
        }
        return true
    }

    private fun findCandidateMarkers(line: String): List<OptionMarker> {
        val markers = mutableListOf<OptionMarker>()

        punctuatedMarkerRegex.findAll(line).forEach { match ->
            val keyGroup = match.groups[1] ?: return@forEach
            val start = keyGroup.range.first
            val marker = OptionMarker(
                key = keyGroup.value.first(),
                markerStart = start,
                contentStart = match.range.last + 1,
                joinedInsideAsciiWord = isJoinedInsideAsciiWord(line, start)
            )
            if (
                !CodeLikeTextGuard.isIndexInStringOrComment(line, marker.markerStart) &&
                isCandidateBoundaryAllowed(line, marker) &&
                !looksLikeDottedAbbreviation(line, marker) &&
                !CodeLikeTextGuard.looksLikeLeadingCodeOption(line, marker.markerStart, marker.contentStart)
            ) {
                markers += marker
            }
        }

        bracketMarkerRegex.findAll(line).forEach { match ->
            val keyGroup = match.groups[1] ?: return@forEach
            val marker = OptionMarker(
                key = keyGroup.value.first(),
                markerStart = match.range.first,
                contentStart = match.range.last + 1,
                joinedInsideAsciiWord = false
            )
            if (
                !CodeLikeTextGuard.isIndexInStringOrComment(line, marker.markerStart) &&
                isCandidateBoundaryAllowed(line, marker) &&
                !CodeLikeTextGuard.looksLikeLeadingCodeOption(line, marker.markerStart, marker.contentStart)
            ) markers += marker
        }

        return markers
            .distinctBy { it.markerStart to it.key.uppercaseChar() }
            .sortedBy { it.markerStart }
    }

    private fun findLeadingMarker(line: String): OptionMarker? {
        punctuatedMarkerRegex.find(line)?.takeIf { it.range.first == 0 }?.let { match ->
            val keyGroup = match.groups[1] ?: return@let
            return OptionMarker(
                key = keyGroup.value.first(),
                markerStart = 0,
                contentStart = match.range.last + 1
            )
        }
        bracketMarkerRegex.find(line)?.takeIf { it.range.first == 0 }?.let { match ->
            val keyGroup = match.groups[1] ?: return@let
            return OptionMarker(
                key = keyGroup.value.first(),
                markerStart = 0,
                contentStart = match.range.last + 1
            )
        }
        return null
    }

    private fun isCandidateBoundaryAllowed(line: String, marker: OptionMarker): Boolean {
        if (marker.markerStart == 0) return true
        val previous = line.getOrNull(marker.markerStart - 1) ?: return true
        if (previous == '_') return false
        // DOCX/OCR 常见为 “A.182B.242C.36 D.72”。数字后的大写 B/C/D
        // 只能作为从 A 开始的连续选项组后续标记参与校验，不能单独触发修复。
        if (previous.isDigit()) return marker.key.isUpperCase()

        val keyIsLowercase = marker.key.isLowerCase()
        if (keyIsLowercase && isAsciiLetter(previous)) return false

        // 大写粘连标记（A.oneB.two）只作为连续选项组中的后续项保留。
        if (isAsciiLetter(previous) && marker.key.isUpperCase()) return true

        return true
    }

    private fun isJoinedInsideAsciiWord(line: String, markerStart: Int): Boolean {
        val previous = line.getOrNull(markerStart - 1) ?: return false
        return isAsciiLetter(previous)
    }

    private fun looksLikeDottedAbbreviation(line: String, marker: OptionMarker): Boolean {
        val markerText = line.substring(marker.markerStart, marker.contentStart)
        if ('.' !in markerText && '．' !in markerText) return false

        val next = line.getOrNull(marker.contentStart) ?: return false
        if (!isAsciiLetter(next)) return false
        val nextNext = line.getOrNull(marker.contentStart + 1)

        // e.g. / D.H. / U.S. 这类连续字母点号。
        if (nextNext == '.' || nextNext == '．') return true

        // E.U / U.S 这类大写缩写，第二个字母后直接结束、空格或标点。
        if (next.isUpperCase() && (nextNext == null || nextNext.isWhitespace() || nextNext in abbreviationTailPunctuation)) {
            return true
        }
        return false
    }

    private fun protectImageMarkers(line: String): ProtectedText {
        val ranges = QuestionImageMarker.rangesIn(line)
        if (ranges.isEmpty()) return ProtectedText(line, emptyMap())
        val replacements = linkedMapOf<String, String>()
        val builder = StringBuilder()
        var cursor = 0
        ranges.forEachIndexed { index, range ->
            if (range.first < cursor) return@forEachIndexed
            builder.append(line.substring(cursor, range.first))
            val token = "@@SHIROHA_IMG_MARKER_$index@@"
            replacements[token] = line.substring(range)
            builder.append(token)
            cursor = range.last + 1
        }
        if (cursor < line.length) builder.append(line.substring(cursor))
        return ProtectedText(builder.toString(), replacements)
    }

    private fun restoreImageMarkers(text: String, protected: ProtectedText): String {
        var restored = text
        protected.replacements.forEach { (token, marker) ->
            restored = restored.replace(token, marker)
        }
        return restored
    }

    private fun keyIndex(key: Char): Int = key.uppercaseChar() - 'A'

    private fun isAsciiLetter(char: Char): Boolean = char in 'A'..'Z' || char in 'a'..'z'

    private val abbreviationTailPunctuation = setOf(
        ',', '，', ';', '；', ':', '：', ')', '）', ']', '】', '}', '》', '/', '\\'
    )
}
