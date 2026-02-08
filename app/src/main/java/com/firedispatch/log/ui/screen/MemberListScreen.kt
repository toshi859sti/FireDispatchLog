package com.firedispatch.log.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.data.repository.SettingsRepository
import com.firedispatch.log.ui.components.GlassCard
import com.firedispatch.log.ui.components.LiquidGlassBackground
import com.firedispatch.log.ui.navigation.Screen
import com.firedispatch.log.ui.viewmodel.MemberListViewModel
import com.firedispatch.log.ui.viewmodel.MemberWithRole
import com.firedispatch.log.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    navController: NavController,
    viewModel: MemberListViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val membersWithRoles by viewModel.membersWithRoles.collectAsState()
    val allowPhoneCall by settingsViewModel.allowPhoneCall.collectAsState()

    LiquidGlassBackground(
        modifier = Modifier.fillMaxSize(),
        primaryColor = Color(0xFFFF5722),  // ディープオレンジ
        secondaryColor = Color(0xFFFF9800), // オレンジ
        tertiaryColor = Color(0xFFE53935),  // 赤
        animated = true
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // AppBarは不透明に（ガラスなし）
                TopAppBar(
                    title = {
                        Text(
                            "団員名簿",
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
            ) {
                // 上部ボタン - シャープな半透明（ガラスなし）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navController.navigate(Screen.MemberEdit.route) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("名簿編集", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { navController.navigate(Screen.RoleAssignment.route) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("役職割り当て", fontWeight = FontWeight.Bold)
                    }
                }

                // 団員リスト - ガラスカード
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(membersWithRoles) { index, memberWithRole ->
                        MemberListItem(memberWithRole, index, allowPhoneCall)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberListItem(memberWithRole: MemberWithRole, index: Int = 0, allowPhoneCall: Boolean = false) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.55f)
                    )
                )
            )
            .graphicsLayer {
                shadowElevation = 10.dp.toPx()
                shape = RoundedCornerShape(20.dp)
                ambientShadowColor = Color.Black.copy(alpha = 0.15f)
                spotShadowColor = Color.Black.copy(alpha = 0.2f)
            }
            .drawBehind {
                // 下エッジを少し強調
                val stroke = 1.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(0f, size.height - stroke),
                    end = Offset(size.width, size.height - stroke),
                    strokeWidth = stroke
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            // 役職バッジ（意味色・固定幅で統一）
            Box(
                modifier = Modifier
                    .width(90.dp)  // 固定幅で4文字対応
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (memberWithRole.roleType == null) {
                            Color(0xFFE11D48)  // 少し彩度を落とした赤
                        } else {
                            Color(0xFFE11D48)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = memberWithRole.roleType?.displayName ?: "未割当",
                    color = Color.White,
                    fontSize = 16.sp,  // 13sp → 16sp
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 名前・電話番号エリア（同じラベル内）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Color(0xFFFFF3E8).copy(alpha = 0.85f)  // 薄いベージュ
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 氏名
                    Text(
                        text = memberWithRole.member.name,
                        fontSize = 18.sp,  // 16sp → 18sp
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // 電話番号（濃く）
                    Text(
                        text = memberWithRole.member.phoneNumber.ifEmpty { "-" },
                        fontSize = 16.sp,  // 14sp → 16sp
                        color = Color.Black.copy(alpha = 0.7f)  // 0.4f → 0.7f
                    )
                }
            }
        }
    }
}

/**
 * ガラススタイルのボタン
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseAlpha: Float = 0.4f
) {
    GlassCard(
        modifier = modifier,
        radius = 16.dp,
        baseAlpha = baseAlpha
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Black
            ),
            elevation = null,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }
    }
}
