package com.kobe.qrbarcode.data.repo

import com.kobe.qrbarcode.core.generate.CodeStyle
import com.kobe.qrbarcode.core.generate.ErrorCorrection
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.core.model.ContentKind
import com.kobe.qrbarcode.data.db.CodeEntity

/** Domain view of a stored code, decoupled from the Room entity. */
data class CodeRecord(
    val id: Long,
    val content: String,
    val format: CodeFormat,
    val source: CodeSource,
    val kind: ContentKind,
    val title: String,
    val createdAt: Long,
    val isFavorite: Boolean,
    val style: CodeStyle
)

fun CodeEntity.toRecord(): CodeRecord = CodeRecord(
    id = id,
    content = content,
    format = CodeFormat.fromName(format),
    source = runCatching { CodeSource.valueOf(source) }.getOrDefault(CodeSource.SCANNED),
    kind = ContentKind.fromName(contentKind),
    title = title,
    createdAt = createdAt,
    isFavorite = favorite,
    style = CodeStyle(
        foreground = foreground,
        background = background,
        margin = margin,
        errorCorrection = ErrorCorrection.fromLevel(errorCorrection)
    )
)

fun CodeRecord.toEntity(): CodeEntity = CodeEntity(
    id = id,
    content = content,
    format = format.name,
    source = source.name,
    contentKind = kind.name,
    title = title,
    createdAt = createdAt,
    favorite = isFavorite,
    foreground = style.foreground,
    background = style.background,
    errorCorrection = style.errorCorrection.level,
    margin = style.margin
)
