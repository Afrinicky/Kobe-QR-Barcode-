package com.kobe.qrbarcode.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One scanned or generated code. The generation style is stored alongside the
 * payload so a history entry can be re-rendered exactly as it was created.
 */
@Entity(
    tableName = "codes",
    indices = [Index("createdAt"), Index("source"), Index("favorite")]
)
data class CodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val format: String,
    val source: String,
    val contentKind: String,
    val title: String,
    val createdAt: Long,
    val favorite: Boolean = false,
    val foreground: Int = 0xFF101010.toInt(),
    val background: Int = 0xFFFFFFFF.toInt(),
    val errorCorrection: String = "M",
    val margin: Int = 2
)
