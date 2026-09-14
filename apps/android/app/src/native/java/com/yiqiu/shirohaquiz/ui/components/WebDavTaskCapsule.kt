package com.yiqiu.shirohaquiz.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.sync.webdav.WebDavBackupManager
import com.yiqiu.shirohaquiz.sync.webdav.WebDavTaskStage

@Composable
fun WebDavTaskCapsule(visible: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember { WebDavBackupManager.getInstance(context) }
    val task by manager.task.collectAsState()
    val value = task ?: return
    if (!visible) return
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            TextButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                Text("云端备份 · ${value.message.ifBlank { "正在处理……" }}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (value.stage !in setOf(WebDavTaskStage.RUNNING, WebDavTaskStage.CANCELLING)) {
                TextButton(onClick = { manager.dismissFinishedTask() }) { Text("收起") }
            }
        }
    }
}
