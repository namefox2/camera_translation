package com.example.artranslator.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cloud Translation API 결과 영구 캐시.
 *
 * 온라인에서 번역한 결과를 저장해 두면, 이후 오프라인 상태에서도
 * 동일 문장을 Cloud 품질로 재사용할 수 있습니다.
 *
 * 상한: 최대 500건 유지 (초과 시 가장 오래된 100건 삭제)
 */
@Entity(
    tableName = "translation_cache",
    indices = [Index(
        value = ["sourceText", "sourceLanguage", "targetLanguage"],
        unique = true
    )]
)
data class TranslationCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)
