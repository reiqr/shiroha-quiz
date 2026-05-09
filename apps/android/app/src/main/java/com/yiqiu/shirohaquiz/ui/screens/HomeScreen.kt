package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.MetricGlassCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.ShortcutGlassCard
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(ShirohaSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Shiroha Quiz",
            title = "通用刷题器",
            subtitle = "导入、练习、考试三条主线，用轻盈卡片承载复杂能力。"
        )

        GlassCard {
            Text(
                text = "当前题库",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "C1 科目一示例库",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "393 题 · 可删除 · 可替换 · 可导入新题库",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPillButton(Icons.Rounded.PlayArrow, "开始刷题")
                ActionPillButton(Icons.Rounded.CloudUpload, "导入题库", primary = false)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricGlassCard(
                label = "错题待复习",
                value = "41",
                desc = "优先回收薄弱点",
                modifier = Modifier.weight(1f)
            )
            MetricGlassCard(
                label = "最近成绩",
                value = "87%",
                desc = "考试模式稳定输出",
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutGlassCard(
                title = "顺序练习",
                icon = Icons.Rounded.AutoStories,
                desc = "按章节推进",
                modifier = Modifier.weight(1f)
            )
            ShortcutGlassCard(
                title = "开始考试",
                icon = Icons.Rounded.Schedule,
                desc = "切入专注模式",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
