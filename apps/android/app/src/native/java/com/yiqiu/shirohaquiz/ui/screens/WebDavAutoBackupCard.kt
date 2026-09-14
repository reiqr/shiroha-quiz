package com.yiqiu.shirohaquiz.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.yiqiu.shirohaquiz.sync.webdav.WebDavAutoBackup
import com.yiqiu.shirohaquiz.ui.components.GlassCard
import com.yiqiu.shirohaquiz.ui.components.NoticeCard

@Composable
internal fun WebDavAutoBackupCard(enabled: Boolean) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(WebDavAutoBackup.load(context)) }
    var expanded by remember { mutableStateOf(false) }
    var confirmDeletion by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    GlassCard {
        Text("自动备份", style = MaterialTheme.typography.titleLarge)
        Text(if (settings.intervalDays == 0) "默认关闭。开启后按条件在后台上传完整备份。" else "已设置自动备份，实际执行时间由 Android 调度。",
            style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起设置" else "配置自动备份") }
        if (expanded) {
            Row {
                listOf(0 to "关闭", 1 to "每天", 7 to "每周").forEach { (days, label) ->
                    FilterChip(settings.intervalDays == days, onClick = { settings = settings.copy(intervalDays = days) }, label = { Text(label) }, enabled = enabled)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(settings.wifiOnly, { settings = settings.copy(wifiOnly = it) }, enabled = enabled)
                Text("仅 Wi-Fi（非计费网络）")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(settings.chargingOnly, { settings = settings.copy(chargingOnly = it) }, enabled = enabled)
                Text("仅充电时")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(settings.deleteOldBackups, { checked ->
                    if (checked) confirmDeletion = true else settings = settings.copy(deleteOldBackups = false)
                }, enabled = enabled)
                Text("上传成功后清理旧的 Android 备份")
            }
            if (settings.deleteOldBackups) Row {
                listOf(5, 10, 20).forEach { count -> FilterChip(settings.keepCount == count,
                    onClick = { settings = settings.copy(keepCount = count) }, label = { Text("保留 $count 份") }, enabled = enabled) }
            }
            Text("建议先验证手动上传与恢复，再开启自动清理。需要加密保存应用密码；自动备份不会覆盖本地内容。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(enabled = enabled, onClick = {
                message = try { WebDavAutoBackup.save(context, settings); "自动备份设置已保存。" }
                    catch (error: Exception) { "保存失败：${error.message}" }
            }) { Text("保存自动备份设置") }
        }
        message?.let { NoticeCard(it, warning = it.contains("失败")) }
    }
    if (confirmDeletion) AlertDialog(onDismissRequest = { confirmDeletion = false }, title = { Text("开启旧备份清理？") },
        text = { Text("自动上传成功后，超过保留份数的旧 Android 备份会被删除。请先确认自己的 WebDAV 服务能够正常上传、下载和恢复。") },
        confirmButton = { TextButton(onClick = { confirmDeletion = false; settings = settings.copy(deleteOldBackups = true) }) { Text("确认开启") } },
        dismissButton = { TextButton(onClick = { confirmDeletion = false }) { Text("取消") } })
}
