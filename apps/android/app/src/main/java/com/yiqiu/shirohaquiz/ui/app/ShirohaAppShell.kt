package com.yiqiu.shirohaquiz.ui.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.screens.HomeScreen
import com.yiqiu.shirohaquiz.ui.screens.ImportScreen
import com.yiqiu.shirohaquiz.ui.screens.MeScreen
import com.yiqiu.shirohaquiz.ui.screens.PracticeScreen

private enum class MainTab(
    val title: String,
    val icon: ImageVector
) {
    Home("首页", Icons.Rounded.Dashboard),
    Practice("练习", Icons.Rounded.School),
    Import("导入", Icons.Rounded.ImportExport),
    Me("我的", Icons.Rounded.Settings)
}

@Composable
fun ShirohaAppShell() {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White.copy(alpha = 0.82f),
                tonalElevation = 0.dp
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF7F9FF),
                            Color(0xFFF1F4FB),
                            Color(0xFFF6F7FB)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 12.dp)
                    .blur(48.dp)
                    .background(Color(0x334F7CFF), CircleShape)
                    .padding(80.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 56.dp)
                    .blur(56.dp)
                    .background(Color(0x22FFFFFF), CircleShape)
                    .padding(68.dp)
            )

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "main_tabs"
            ) { tab ->
                when (tab) {
                    MainTab.Home -> HomeScreen()
                    MainTab.Practice -> PracticeScreen()
                    MainTab.Import -> ImportScreen()
                    MainTab.Me -> MeScreen()
                }
            }
        }
    }
}
