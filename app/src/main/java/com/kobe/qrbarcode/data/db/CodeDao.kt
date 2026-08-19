package com.kobe.qrbarcode.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeDao {

    @Insert
    suspend fun insert(entity: CodeEntity): Long

    @Insert
    suspend fun insertAll(entities: List<CodeEntity>): List<Long>

    @Query("DELETE FROM codes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM codes")
    suspend fun deleteAll()

    @Query("DELETE FROM codes WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("UPDATE codes SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("SELECT * FROM codes WHERE id = :id")
    fun observe(id: Long): Flow<CodeEntity?>

    @Query("SELECT * FROM codes ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CodeEntity>>

    @Query(
        """
        SELECT * FROM codes
        WHERE (:source IS NULL OR source = :source)
          AND (:favouritesOnly = 0 OR favorite = 1)
          AND (:query = '' OR content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
        """
    )
    fun observeFiltered(
        source: String?,
        favouritesOnly: Boolean,
        query: String
    ): Flow<List<CodeEntity>>

    @Query("SELECT COUNT(*) FROM codes WHERE source = :source")
    fun countBySource(source: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM codes WHERE favorite = 1")
    fun countFavorites(): Flow<Int>

    @Query("SELECT * FROM codes WHERE content = :content AND source = :source LIMIT 1")
    suspend fun findByContent(content: String, source: String): CodeEntity?
}
