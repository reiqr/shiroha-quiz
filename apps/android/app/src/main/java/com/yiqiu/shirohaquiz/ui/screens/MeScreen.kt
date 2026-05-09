package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun MeScreen() {
    Column(
        modifier = Modifier.padding(ShirohaSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Me",
            title = "设置与资料",
            subtitle = "这里后面接导出、备份、解析策略和主题设置。"
        )
        GlassCard {
            Text(
                text = "当前阶段先把三大核心页做稳，设置页暂时保留为轻量占位。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
