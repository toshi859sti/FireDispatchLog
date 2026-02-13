package com.firedispatch.log.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.BackgroundColorPreset
import com.firedispatch.log.data.repository.BackgroundColorRepository
import com.firedispatch.log.ui.theme.TailwindColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 画面に背景色を適用するComposable
 * プリセットが設定されていない場合は、デフォルト背景またはコンテンツのみを表示
 */
@Composable
fun ScreenBackground(
    screenName: String,
    modifier: Modifier = Modifier,
    defaultBackground: (@Composable (content: @Composable () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var preset by remember { mutableStateOf<BackgroundColorPreset?>(null) }
    var hasPreset by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(screenName) {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val repository = BackgroundColorRepository(database.backgroundColorDao())

            val loadedPreset = repository.getPresetForScreen(screenName)
            withContext(Dispatchers.Main) {
                preset = loadedPreset
                hasPreset = loadedPreset != null
            }
        }
    }

    // プリセットの読み込みが完了するまで待機
    if (hasPreset == null) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
        return
    }

    // プリセットがある場合はLiquidGlassBackgroundでアニメーション表示
    if (hasPreset == true && preset != null) {
        LiquidGlassBackground(
            modifier = modifier.fillMaxSize(),
            primaryColor = TailwindColors.getColor(preset!!.color1),
            secondaryColor = TailwindColors.getColor(preset!!.color2),
            tertiaryColor = TailwindColors.getColor(preset!!.color3),
            animated = true
        ) {
            content()
        }
    } else if (defaultBackground != null) {
        // プリセットがなく、デフォルト背景が指定されている場合
        defaultBackground(content)
    } else {
        // プリセットもデフォルト背景もない場合
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
    }
}
