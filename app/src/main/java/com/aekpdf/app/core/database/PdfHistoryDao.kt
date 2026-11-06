package com.aekpdf.app.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfHistoryDao {
    @Query("SELECT * FROM pdf_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<PdfHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PdfHistoryEntity): Long

    @Delete
    suspend fun delete(entity: PdfHistoryEntity)

    @Query("DELETE FROM pdf_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM pdf_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PdfHistoryEntity?
}
