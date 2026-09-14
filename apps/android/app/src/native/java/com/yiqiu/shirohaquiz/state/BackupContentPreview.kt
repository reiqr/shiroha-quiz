package com.yiqiu.shirohaquiz.state

/** Validated content counts; questionCount counts bank questions, not standalone snapshots. */
data class BackupContentPreview(
    val bankCount: Int,
    val questionCount: Int,
    val wrongCount: Int,
    val favoriteCount: Int,
    val recordCount: Int,
    val sourceVersion: Int?,
    val sourceKind: String,
    val exportedBy: String?,
    val exportedAt: Long?,
    val assetCount: Int,
    val slashedCount: Int = 0
)
