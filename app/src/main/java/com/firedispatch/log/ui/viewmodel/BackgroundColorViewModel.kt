package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.BackgroundColorPreset
import com.firedispatch.log.data.entity.ScreenBackgroundMapping
import com.firedispatch.log.data.repository.BackgroundColorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BackgroundColorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BackgroundColorRepository

    init {
        val dao = AppDatabase.getDatabase(application).backgroundColorDao()
        repository = BackgroundColorRepository(dao)
    }

    // プリセット一覧
    val presets: StateFlow<List<BackgroundColorPreset>> =
        repository.getAllPresets().stateIn(viewModelScope, emptyList())

    // 画面マッピング一覧
    val mappings: StateFlow<List<ScreenBackgroundMapping>> =
        repository.getAllMappings().stateIn(viewModelScope, emptyList())

    // プリセット作成
    fun createPreset(name: String, color1: String, color2: String, color3: String) {
        viewModelScope.launch {
            val count = repository.getPresetCount()
            if (count < 10) {
                repository.insertPreset(
                    BackgroundColorPreset(
                        name = name,
                        color1 = color1,
                        color2 = color2,
                        color3 = color3
                    )
                )
            }
        }
    }

    // プリセット更新
    fun updatePreset(preset: BackgroundColorPreset) {
        viewModelScope.launch {
            repository.updatePreset(preset)
        }
    }

    // プリセット削除
    fun deletePreset(preset: BackgroundColorPreset) {
        viewModelScope.launch {
            repository.deletePreset(preset)
        }
    }

    // 画面にプリセットを設定
    fun setScreenPreset(screenName: String, presetId: Long) {
        viewModelScope.launch {
            repository.setScreenMapping(screenName, presetId)
        }
    }

    // 画面のプリセット設定を解除
    fun removeScreenPreset(screenName: String) {
        viewModelScope.launch {
            repository.removeScreenMapping(screenName)
        }
    }

    // 画面名に対応するプリセットを取得
    suspend fun getPresetForScreen(screenName: String): BackgroundColorPreset? {
        return repository.getPresetForScreen(screenName)
    }

    // Flow を StateFlow に変換するヘルパー
    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateIn(
        scope: kotlinx.coroutines.CoroutineScope,
        initialValue: T
    ): StateFlow<T> {
        val stateFlow = MutableStateFlow(initialValue)
        scope.launch {
            collect { stateFlow.value = it }
        }
        return stateFlow
    }
}
