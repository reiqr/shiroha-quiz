package com.yiqiu.shirohaquiz.importer.model

/** A point local to its writing region; x is 0..1 and yDp uses the region's original dp height. */
data class InkPoint(val x: Float, val yDp: Float)

data class InkStroke(
    val tool: String = "pen",
    val argb: Long = 0xFF2855B1L,
    val widthDp: Float = 2.6f,
    val points: List<InkPoint> = emptyList(),
    /** Empty for old card-relative strokes; new strokes attach to stem, original option key or scratch. */
    val anchor: String = "",
    val referenceHeightDp: Float = 0f
)

object InkGeometry {
    const val MAX_STROKES = 600
    const val MAX_POINTS_PER_STROKE = 1200
    // Legacy builds permitted 600 x 1200 points. Do not discard valid old backups.
    const val MAX_TOTAL_POINTS = MAX_STROKES * MAX_POINTS_PER_STROKE
    // New writing is capped to avoid growing ordinary study-bank JSON without bound.
    const val MAX_NEW_DRAW_POINTS = 80000

    /** Keep annotations associated with the same logical option even after answer shuffling. */
    fun validAnchor(anchor: String): Boolean =
        anchor.isEmpty() || anchor == "stem" || anchor == "images" || anchor == "cloze" ||
            anchor == "scratch" || Regex("""option:[A-Za-z0-9_]{1,20}""").matches(anchor)

    /** Preserve older strokes, but allow new anchored strokes to resize with their own region. */
    fun scaledForHeight(stroke: InkStroke, currentHeightDp: Float): InkStroke {
        if (stroke.anchor.isEmpty() || stroke.referenceHeightDp <= 0f || currentHeightDp <= 0f) return stroke
        val scale = (currentHeightDp / stroke.referenceHeightDp).coerceIn(0.1f, 10f)
        return stroke.copy(points = stroke.points.map { p -> p.copy(yDp = p.yDp * scale) })
    }

    fun distanceToStrokeDp(xDp: Float, yDp: Float, stroke: InkStroke, regionWidthDp: Float): Float {
        val points = stroke.points
        if (points.isEmpty() || regionWidthDp <= 0f) return Float.POSITIVE_INFINITY
        fun px(p: InkPoint) = p.x * regionWidthDp
        if (points.size == 1) return kotlin.math.hypot(xDp - px(points[0]), yDp - points[0].yDp)
        var minDistance = Float.POSITIVE_INFINITY
        for (index in 0 until points.lastIndex) {
            val a = points[index]
            val b = points[index + 1]
            val ax = px(a)
            val bx = px(b)
            val dx = bx - ax
            val dy = b.yDp - a.yDp
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared <= 0f) 0f else
                (((xDp - ax) * dx + (yDp - a.yDp) * dy) / lengthSquared).coerceIn(0f, 1f)
            val distance = kotlin.math.hypot(xDp - (ax + t * dx), yDp - (a.yDp + t * dy))
            if (distance < minDistance) minDistance = distance
        }
        return minDistance
    }

    fun isValid(stroke: InkStroke): Boolean =
        stroke.tool in setOf("pen", "marker") && validAnchor(stroke.anchor) &&
            stroke.widthDp.isFinite() && stroke.widthDp in 0.5f..32f &&
            stroke.points.size in 1..MAX_POINTS_PER_STROKE &&
            stroke.referenceHeightDp.isFinite() && stroke.referenceHeightDp in 0f..20000f &&
            (stroke.anchor.isEmpty() || stroke.referenceHeightDp > 0f) &&
            stroke.points.all { it.x.isFinite() && it.x in 0f..1f &&
                it.yDp.isFinite() && it.yDp in 0f..20000f }

    fun withinLimits(strokes: List<InkStroke>): Boolean =
        strokes.size <= MAX_STROKES &&
            strokes.sumOf { it.points.size } <= MAX_TOTAL_POINTS &&
            strokes.all(::isValid)
}
