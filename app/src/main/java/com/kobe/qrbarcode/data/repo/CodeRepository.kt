package com.kobe.qrbarcode.data.repo

import com.kobe.qrbarcode.core.generate.CodeStyle
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.core.model.ParsedContent
import com.kobe.qrbarcode.data.db.CodeDao
import com.kobe.qrbarcode.data.db.CodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The single entry point to stored history and favourites. */
@Singleton
class CodeRepository @Inject constructor(private val dao: CodeDao) {

    fun recent(limit: Int = 6): Flow<List<CodeRecord>> =
        dao.observeRecent(limit).map { list -> list.map { it.toRecord() } }

    fun filtered(
        source: CodeSource?,
        favouritesOnly: Boolean,
        query: String
    ): Flow<List<CodeRecord>> =
        dao.observeFiltered(source?.name, favouritesOnly, query.trim())
            .map { list -> list.map { it.toRecord() } }

    fun observe(id: Long): Flow<CodeRecord?> = dao.observe(id).map { it?.toRecord() }

    fun scannedCount(): Flow<Int> = dao.countBySource(CodeSource.SCANNED.name)

    fun generatedCount(): Flow<Int> = dao.countBySource(CodeSource.GENERATED.name)

    fun favouriteCount(): Flow<Int> = dao.countFavorites()

    suspend fun record(
        parsed: ParsedContent,
        format: CodeFormat,
        source: CodeSource,
        style: CodeStyle = CodeStyle(),
        favorite: Boolean = false
    ): Long = dao.insert(
        CodeEntity(
            content = parsed.raw,
            format = format.name,
            source = source.name,
            contentKind = parsed.kind.name,
            title = parsed.title,
            createdAt = System.currentTimeMillis(),
            favorite = favorite,
            foreground = style.foreground,
            background = style.background,
            errorCorrection = style.errorCorrection.level,
            margin = style.margin
        )
    )

    suspend fun recordAll(entries: List<Triple<ParsedContent, CodeFormat, CodeSource>>) {
        val now = System.currentTimeMillis()
        dao.insertAll(
            entries.mapIndexed { index, (parsed, format, source) ->
                CodeEntity(
                    content = parsed.raw,
                    format = format.name,
                    source = source.name,
                    contentKind = parsed.kind.name,
                    title = parsed.title,
                    createdAt = now + index
                )
            }
        )
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clearAll() = dao.deleteAll()

    suspend fun clear(source: CodeSource) = dao.deleteBySource(source.name)
}
