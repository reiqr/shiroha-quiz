package com.yiqiu.shirohaquiz.document

import com.yiqiu.shirohaquiz.importer.assets.QuestionImportAssetExtractor
import com.yiqiu.shirohaquiz.importer.model.ImportResult
import com.yiqiu.shirohaquiz.importer.model.ImportWarning
import com.yiqiu.shirohaquiz.importer.model.WarningLevel

object DocumentImageBinding {
    fun applyAssignments(result: ImportResult, images: List<QuestionImportAssetExtractor.ExtractedImportImage>,
                         assignments: Map<String, Int>): ImportResult {
        if (images.isEmpty()) return result
        val explicitIds = assignments.keys
        val warnings = mutableListOf<ImportWarning>()
        assignments.forEach { (id, index) ->
            if (index !in result.questions.indices) warnings += ImportWarning(WarningLevel.ERROR, null,
                "图片 ${images.firstOrNull { it.image.id == id }?.image?.order ?: ""} 指定的第 ${index + 1} 道题不存在，请检查图片归属。")
        }
        val questions = result.questions.mapIndexed { index, question ->
            val automaticallyBound = question.images.filterNot { it.id in explicitIds }
            val manuallyBound = images.filter { assignments[it.image.id] == index }.map { it.image }
            question.copy(images = (automaticallyBound + manuallyBound).distinctBy { it.id }.sortedBy { it.order })
        }
        val boundIds = questions.flatMap { it.images }.map { it.id }.toSet()
        val unbound = images.count { it.image.id !in boundIds }
        val originalWarnings = result.warnings.filterNot {
            it.message.startsWith("检测到 ${images.size} 张图片，已绑定") ||
                it.message.startsWith("有 ") && it.message.contains("张图片未能明确绑定到题目")
        }
        warnings += ImportWarning(if (unbound > 0) WarningLevel.WARNING else WarningLevel.NORMAL, null,
            "OCR 图片：共 ${images.size} 张，已绑定 ${images.size - unbound} 张${if (unbound > 0) "；$unbound 张未绑定，请核对图片归属。" else "。"}")
        return result.copy(questions = questions, warnings = originalWarnings + warnings)
    }
}
