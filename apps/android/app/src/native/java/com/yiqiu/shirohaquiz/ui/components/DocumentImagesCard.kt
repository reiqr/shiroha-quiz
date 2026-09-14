package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.document.DocumentImageAsset
import com.yiqiu.shirohaquiz.document.DocumentRecognitionManager

@Composable
fun DocumentImagesCard(images: List<DocumentImageAsset>, enabled: Boolean) {
    if (images.isEmpty()) return
    var expanded by remember { mutableStateOf(true) }
    var pendingMerge by remember { mutableStateOf<String?>(null) }
    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("识别图片 · ${images.count { !it.excluded }} 张", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起" else "展开") }
        }
        Text("默认按原文位置绑定。需要调整时，可指定导入预览中第几道题；这里填写列表序号，不是原文题号。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (expanded) {
            LazyColumn(Modifier.fillMaxWidth().height(480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(images, key = { _, asset -> asset.image.id }) { index, asset ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("图片 ${index + 1}${if (asset.excluded) " · 已排除" else ""}", style = MaterialTheme.typography.titleSmall)
                        QuestionImagesBlock(listOf(asset.image), maxPreviewHeight = 180.dp)
                        var ordinal by remember(asset.marker, asset.targetQuestionIndex) {
                            mutableStateOf(asset.targetQuestionIndex?.plus(1)?.toString().orEmpty())
                        }
                        OutlinedTextField(value = ordinal, onValueChange = { value ->
                            if (value.length <= 5 && value.all(Char::isDigit)) {
                                ordinal = value
                                val number = value.toIntOrNull()
                                if (value.isEmpty() || number != null && number > 0) DocumentRecognitionManager.assignImage(asset.marker, number)
                            }
                        }, label = { Text("指定第几道题（留空按原文位置）") }, enabled = enabled && !asset.excluded,
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(enabled = enabled, onClick = { DocumentRecognitionManager.excludeImage(asset.marker, !asset.excluded) }) {
                                Text(if (asset.excluded) "恢复图片" else "排除图片")
                            }
                            TextButton(enabled = enabled && index > 0, onClick = { DocumentRecognitionManager.moveImage(asset.marker, -1) }) { Text("上移") }
                            TextButton(enabled = enabled && index + 1 < images.size, onClick = { DocumentRecognitionManager.moveImage(asset.marker, 1) }) { Text("下移") }
                            TextButton(enabled = enabled && !asset.excluded && index + 1 < images.size && !images[index + 1].excluded,
                                onClick = { pendingMerge = asset.marker }) { Text("合并下一张") }
                        }
                    }
                }
            }
        }
    }
    pendingMerge?.let { marker ->
        AlertDialog(onDismissRequest = { pendingMerge = null }, title = { Text("合并相邻图片？") },
            text = { Text("仅在两张图片确实属于同一图表或材料时合并。将按当前顺序上下拼接，请检查合并后的题目归属。") },
            confirmButton = { TextButton(onClick = { pendingMerge = null; DocumentRecognitionManager.mergeImageWithNext(marker) }) { Text("确认合并") } },
            dismissButton = { TextButton(onClick = { pendingMerge = null }) { Text("取消") } })
    }
}
