package com.example.artranslator.core.translation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quotaDataStore by preferencesDataStore(name = "translation_quota")

@Singleton
class TranslationQuotaManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DAILY_FREE = 100
        const val AD_GRANT = 100
        private val KEY_REMAINING = intPreferencesKey("remaining")
        private val KEY_DATE = stringPreferencesKey("date")
        private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private fun today() = dateFmt.format(Date())

    suspend fun getRemaining(): Int {
        val prefs = context.quotaDataStore.data.first()
        return if (prefs[KEY_DATE] != today()) DAILY_FREE
        else prefs[KEY_REMAINING] ?: DAILY_FREE
    }

    /** 번역 1회 차감. 쿼터 없으면 false 반환 */
    suspend fun consume(): Boolean {
        var allowed = false
        context.quotaDataStore.edit { prefs ->
            val today = today()
            val current = if (prefs[KEY_DATE] != today) DAILY_FREE else (prefs[KEY_REMAINING] ?: DAILY_FREE)
            if (current > 0) {
                prefs[KEY_DATE] = today
                prefs[KEY_REMAINING] = current - 1
                allowed = true
            }
        }
        return allowed
    }

    /** 광고 시청 후 호출 — 100회 추가 */
    suspend fun grantAdReward() {
        context.quotaDataStore.edit { prefs ->
            val today = today()
            val current = if (prefs[KEY_DATE] != today) DAILY_FREE else (prefs[KEY_REMAINING] ?: DAILY_FREE)
            prefs[KEY_DATE] = today
            prefs[KEY_REMAINING] = current + AD_GRANT
        }
    }
}
