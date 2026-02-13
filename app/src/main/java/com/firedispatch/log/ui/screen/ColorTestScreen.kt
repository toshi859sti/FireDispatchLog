package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.firedispatch.log.ui.components.LiquidGlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorTestScreen(
    navController: NavController
) {
    var primaryColor by remember { mutableStateOf(Color(0xFFFF5722)) }
    var secondaryColor by remember { mutableStateOf(Color(0xFFFF9800)) }
    var tertiaryColor by remember { mutableStateOf(Color(0xFFE53935)) }

    var selectedColorIndex by remember { mutableStateOf(0) }

    LiquidGlassBackground(
        modifier = Modifier.fillMaxSize(),
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        tertiaryColor = tertiaryColor,
        animated = true
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "背景色テスト",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // カラー選択ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorSelectButton(
                        text = "Primary",
                        color = primaryColor,
                        isSelected = selectedColorIndex == 0,
                        onClick = { selectedColorIndex = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    ColorSelectButton(
                        text = "Secondary",
                        color = secondaryColor,
                        isSelected = selectedColorIndex == 1,
                        onClick = { selectedColorIndex = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    ColorSelectButton(
                        text = "Tertiary",
                        color = tertiaryColor,
                        isSelected = selectedColorIndex == 2,
                        onClick = { selectedColorIndex = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // カラーピッカー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    val currentColor = when (selectedColorIndex) {
                        0 -> primaryColor
                        1 -> secondaryColor
                        else -> tertiaryColor
                    }

                    ColorPicker(
                        color = currentColor,
                        onColorChange = { newColor ->
                            when (selectedColorIndex) {
                                0 -> primaryColor = newColor
                                1 -> secondaryColor = newColor
                                2 -> tertiaryColor = newColor
                            }
                        }
                    )
                }

                // プリセットカラー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "プリセット配色",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        PresetColorButton(
                            name = "赤・オレンジ",
                            colors = listOf(
                                Color(0xFFFF5722),
                                Color(0xFFFF9800),
                                Color(0xFFE53935)
                            ),
                            onClick = {
                                primaryColor = Color(0xFFFF5722)
                                secondaryColor = Color(0xFFFF9800)
                                tertiaryColor = Color(0xFFE53935)
                            }
                        )

                        PresetColorButton(
                            name = "青・緑",
                            colors = listOf(
                                Color(0xFF2196F3),
                                Color(0xFF4CAF50),
                                Color(0xFF03A9F4)
                            ),
                            onClick = {
                                primaryColor = Color(0xFF2196F3)
                                secondaryColor = Color(0xFF4CAF50)
                                tertiaryColor = Color(0xFF03A9F4)
                            }
                        )

                        PresetColorButton(
                            name = "黒・赤・黄色",
                            colors = listOf(
                                Color(0xFF000000),
                                Color(0xFFFF0000),
                                Color(0xFFFFFF00)
                            ),
                            onClick = {
                                primaryColor = Color(0xFF000000)
                                secondaryColor = Color(0xFFFF0000)
                                tertiaryColor = Color(0xFFFFFF00)
                            }
                        )

                        PresetColorButton(
                            name = "紫・ピンク",
                            colors = listOf(
                                Color(0xFF9C27B0),
                                Color(0xFFE91E63),
                                Color(0xFF673AB7)
                            ),
                            onClick = {
                                primaryColor = Color(0xFF9C27B0)
                                secondaryColor = Color(0xFFE91E63)
                                tertiaryColor = Color(0xFF673AB7)
                            }
                        )
                    }
                }

                // カラーコード表示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "カラーコード",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Primary: ${colorToHex(primaryColor)}",
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            "Secondary: ${colorToHex(secondaryColor)}",
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            "Tertiary: ${colorToHex(tertiaryColor)}",
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSelectButton(
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    Color.White.copy(alpha = 0.8f)
                } else {
                    Color.White.copy(alpha = 0.5f)
                }
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
            )
            Text(
                text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    var red by remember(color) { mutableStateOf(color.red) }
    var green by remember(color) { mutableStateOf(color.green) }
    var blue by remember(color) { mutableStateOf(color.blue) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // プレビュー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(red, green, blue))
                .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
        )

        // Red
        ColorSlider(
            label = "Red",
            value = red,
            color = Color.Red,
            onValueChange = {
                red = it
                onColorChange(Color(red, green, blue))
            }
        )

        // Green
        ColorSlider(
            label = "Green",
            value = green,
            color = Color.Green,
            onValueChange = {
                green = it
                onColorChange(Color(red, green, blue))
            }
        )

        // Blue
        ColorSlider(
            label = "Blue",
            value = blue,
            color = Color.Blue,
            onValueChange = {
                blue = it
                onColorChange(Color(red, green, blue))
            }
        )
    }
}

@Composable
fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.width(50.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f)
            )
        )
        Text(
            "${(value * 255).toInt()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
fun PresetColorButton(
    name: String,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return "#%02X%02X%02X".format(red, green, blue)
}
