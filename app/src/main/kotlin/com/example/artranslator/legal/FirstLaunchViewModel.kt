package com.example.artranslator.legal

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.legalDataStore by preferencesDataStore(name = "legal_prefs")

@HiltViewModel
class FirstLaunchViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val KEY_PERMISSION_NOTICE_SHOWN = booleanPreferencesKey("permission_notice_shown")
    private val KEY_PRIVACY_AGREED = booleanPreferencesKey("privacy_agreed")

    // true = 팝업을 보여줘야 함
    private val _showPermissionNotice = MutableStateFlow(false)
    val showPermissionNotice: StateFlow<Boolean> = _showPermissionNotice.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = context.legalDataStore.data.first()
            val alreadyShown = prefs[KEY_PERMISSION_NOTICE_SHOWN] ?: false
            _showPermissionNotice.value = !alreadyShown
        }
    }

    /** 사용자가 고지 팝업을 확인하고 동의했을 때 호출 */
    fun onPermissionNoticeAcknowledged() {
        _showPermissionNotice.value = false
        viewModelScope.launch {
            context.legalDataStore.edit { prefs ->
                prefs[KEY_PERMISSION_NOTICE_SHOWN] = true
                prefs[KEY_PRIVACY_AGREED] = true
            }
        }
    }
}
