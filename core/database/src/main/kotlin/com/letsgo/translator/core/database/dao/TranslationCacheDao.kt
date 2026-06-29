package com.letsgo.translator.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.letsgo.translator.core.database.entity.TranslationCacheEntity

@Dao
abstract class TranslationCacheDao {

    /** 캐시 히트: 동일 원문·언어 쌍 검색 */
    @Query("""
        SELECT * FROM translation_cache
        WHERE sourceText = :text
          AND sourceLanguage = :src
          AND targetLanguage = :tgt
        LIMIT 1
    """)
    abstract suspend fun find(text: String, src: String, tgt: String): TranslationCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsert(entity: TranslationCacheEntity)

    @Query("SELECT COUNT(*) FROM translation_cache")
    protected abstract suspend fun count(): Int

    @Query("""
        DELETE FROM translation_cache
        WHERE id IN (
            SELECT id FROM translation_cache
            ORDER BY timestamp ASC
            LIMIT :n
        )
    """)
    protected abstract suspend fun deleteOldest(n: Int)

    /** count → evict → upsert를 단일 트랜잭션으로 실행 (TOCTOU 방지) */
    @Transaction
    open suspend fun upsertWithEviction(entity: TranslationCacheEntity, maxSize: Int, evictCount: Int) {
        if (count() >= maxSize) {
            deleteOldest(evictCount)
        }
        upsert(entity)
    }
}
