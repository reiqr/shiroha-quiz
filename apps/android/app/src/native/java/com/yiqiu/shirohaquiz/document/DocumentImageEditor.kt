package com.yiqiu.shirohaquiz.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import com.yiqiu.shirohaquiz.importer.model.QuestionImage
import java.io.File
import java.util.UUID

object DocumentImageEditor {
    fun merge(first: DocumentImageAsset, second: DocumentImageAsset, directory: File): DocumentImageAsset {
        val top = decode(first.image.localPath)
        try {
            val bottom = decode(second.image.localPath)
            try {
                val width = maxOf(top.width, bottom.width).coerceAtMost(1800)
                val topHeight = (top.height.toLong() * width / top.width).toInt()
                val bottomHeight = (bottom.height.toLong() * width / bottom.width).toInt()
                require(topHeight + bottomHeight <= 6000) { "合并后的图片过高，请减少碎图或分别绑定。" }
                val combined = Bitmap.createBitmap(width, topHeight + bottomHeight, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(combined)
                    canvas.drawColor(Color.WHITE)
                    canvas.drawBitmap(top, null, Rect(0, 0, width, topHeight), null)
                    canvas.drawBitmap(bottom, null, Rect(0, topHeight, width, topHeight + bottomHeight), null)
                    directory.mkdirs()
                    val target = File(directory, "merged_${UUID.randomUUID()}.png")
                    target.outputStream().use { require(combined.compress(Bitmap.CompressFormat.PNG, 100, it)) { "合并图片保存失败。" } }
                    return first.copy(image = QuestionImage(localPath = target.absolutePath,
                        sourceName = "手动合并图片", order = first.image.order, width = width,
                        height = combined.height, sizeBytes = target.length()), targetQuestionIndex = first.targetQuestionIndex)
                } finally { combined.recycle() }
            } finally { bottom.recycle() }
        } finally { top.recycle() }
    }

    private fun decode(path: String): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "图片无法读取。" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1800) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalArgumentException("图片无法解码。")
    }
}
