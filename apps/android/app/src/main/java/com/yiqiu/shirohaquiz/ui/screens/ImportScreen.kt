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
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard
import com.yiqiu.shirohaquiz.ui.components.ShirohaHeader
import com.yiqiu.shirohaquiz.ui.components.StatusChip
import com.yiqiu.shirohaquiz.ui.components.UploadPanel
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun ImportScreen() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(ShirohaSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Lg)
    ) {
        ShirohaHeader(
            kicker = "Import",
            title = "导入题库",
            subtitle = "首发支持 txt / json / csv / docx / doc / 文字层 PDF，以及双文件导入。"
        )

        GlassCard {
            Text(
                text = "导入方式",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChip("单文件导入", selected = true)
                StatusChip("双文件导入")
                StatusChip("粘贴文本")
            }
            Spacer(Modifier.height(18.dp))
            UploadPanel(
                title = "选择题库文件",
                desc = "拖拽或点击选择文件，优先支持结构化文本和文字层 PDF。",
                icon = Icons.Rounded.UploadFile
            )
        }

        GlassCard {
            Text(
                text = "识别预览",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            NoticeCard("本次识别到 420 题，其中 17 题需要人工确认。")
        }
    }
}
