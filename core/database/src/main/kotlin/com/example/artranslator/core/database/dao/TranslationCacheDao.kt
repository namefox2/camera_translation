package com.example.artranslator.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.artranslator.core.database.entity.TranslationCacheEntity

@Dao
interface TranslationCacheDao {

    /** 캐시 히트: 동일 원문·언어 쌍 검색 */
    @Query("""
        SELECT * FROM translation_cache
        WHERE sourceText = :text
          AND sourceLanguage = :src
          AND targetLanguage = :tgt
        LIMIT 1
    """)
    suspend fun find(text: String, src: String, tgt: String): TranslationCacheEntity?

    /** Cloud 번역 성공 시 저장 (동일 키 존재하면 갱신) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TranslationCacheEntity)

    /** 전체 항목 수 */
    @Query("SELECT COUNT(*) FROM translation_cache")
    suspend fun count(): Int

    /** 오래된 순서로 n건 삭제 (상한 초과 시 호출) */
    @Query("""
        DELETE FROM translation_cache
        WHERE id IN (
            SELECT id FROM translation_cache
            ORDER BY timestamp ASC
            LIMIT :n
        )
    """)
    suspend fun deleteOldest(n: Int)
}
