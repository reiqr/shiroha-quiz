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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yiqiu.shirohaquiz.ui.components.ActionPillButton
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.QuizOptionCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun PracticeScreen() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(ShirohaSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Practice",
            title = "沉浸刷题",
            subtitle = "让题干更靠前、选项更清楚、操作更顺手。"
        )

        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("第 12 题", selected = true)
                StatusChip("单选题")
                StatusChip("顺序练习")
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "雨天临近弯道行驶时，最稳妥的做法是？",
                style = MaterialTheme.typography.headlineSmall,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            QuizOptionCard("A", "保持原速，快速通过弯道", selected = false)
            Spacer(Modifier.height(10.dp))
            QuizOptionCard("B", "提前减速，平顺转向", selected = true)
            Spacer(Modifier.height(10.dp))
            QuizOptionCard("C", "紧急制动后再打方向", selected = false)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPillButton(Icons.Rounded.CheckCircle, "提交答案")
                ActionPillButton(Icons.Rounded.TextSnippet, "查看解析", primary = false)
            }
        }
    }
}
