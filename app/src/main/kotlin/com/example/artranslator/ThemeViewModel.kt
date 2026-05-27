package com.example.artranslator

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artranslator.core.ui.theme.ThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@HiltViewModel
class ThemeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val THEME_KEY = stringPreferencesKey("theme_type")

    val currentTheme: StateFlow<ThemeType> = context.dataStore.data
        .map { prefs ->
            val name = prefs[THEME_KEY] ?: ThemeType.DEFAULT.name
            runCatching { ThemeType.valueOf(name) }.getOrDefault(ThemeType.DEFAULT)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeType.DEFAULT)

    fun setTheme(theme: ThemeType) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[THEME_KEY] = theme.name
            }
        }
    }
}
