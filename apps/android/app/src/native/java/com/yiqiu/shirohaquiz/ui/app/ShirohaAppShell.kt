package com.yiqiu.shirohaquiz.ui.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.ui.screens.AboutScreen
import com.yiqiu.shirohaquiz.ui.screens.BankDetailScreen
import com.yiqiu.shirohaquiz.ui.screens.BankListScreen
import com.yiqiu.shirohaquiz.ui.screens.BankReviewScreen
import com.yiqiu.shirohaquiz.ui.screens.ExamScreen
import com.yiqiu.shirohaquiz.ui.screens.HomeScreen
import com.yiqiu.shirohaquiz.ui.screens.ImportScreen
import com.yiqiu.shirohaquiz.ui.screens.MeScreen
import com.yiqiu.shirohaquiz.ui.screens.PracticeScreen
import com.yiqiu.shirohaquiz.ui.screens.RecordDetailScreen
import com.yiqiu.shirohaquiz.ui.screens.RecordsScreen
import com.yiqiu.shirohaquiz.ui.screens.StandardImportFormatScreen
import com.yiqiu.shirohaquiz.ui.screens.WrongBookScreen
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaDimens
import com.yiqiu.shirohaquiz.ui.theme.ShirohaMotion
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius

private val ShirohaPageEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val showInBottomBar: Boolean = true
) {
    Home("首页", Icons.Rounded.Dashboard),
    Practice("练习", Icons.Rounded.School),
    Import("导入", Icons.Rounded.ImportExport),
    Me("我的", Icons.Rounded.Settings),
    Exam("考试", Icons.Rounded.School, showInBottomBar = false),
    BankList("题库管理", Icons.Rounded.Dashboard, showInBottomBar = false),
    BankDetail("题库详情", Icons.Rounded.Dashboard, showInBottomBar = false),
    BankReview("题库核对", Icons.Rounded.Dashboard, showInBottomBar = false),
    WrongBook("错题本", Icons.Rounded.School, showInBottomBar = false),
    Records("记录", Icons.Rounded.Dashboard, showInBottomBar = false),
    RecordDetail("记录详情", Icons.Rounded.Dashboard, showInBottomBar = false),
    StandardFormat("标准格式", Icons.Rounded.ImportExport, showInBottomBar = false),
    About("关于", Icons.Rounded.Settings, showInBottomBar = false)
}

@Composable
fun ShirohaAppShell() {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var detailBankId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailRecordId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomAppBar(
                containerColor = ShirohaColors.BottomBar,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ShirohaDimens.BottomBarHorizontalPadding, vertical = ShirohaDimens.BottomBarVerticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(ShirohaDimens.BottomNavItemGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainTab.entries.filter { it.showInBottomBar }.forEach { tab ->
                        ShirohaBottomNavItem(
                            tab = tab,
                            selected = currentTab == tab,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentTab != tab) currentTab = tab
                            }
                        )
                    }
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
                            ShirohaColors.BgGradientTop,
                            ShirohaColors.BgGradientMiddle,
                            ShirohaColors.BgGradientBottom
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (fadeIn(
                        animationSpec = tween(durationMillis = ShirohaMotion.PageTransitionMillis, easing = ShirohaPageEaseOut)
                    ) + slideInVertically(
                        animationSpec = tween(durationMillis = ShirohaMotion.PageTransitionMillis, easing = ShirohaPageEaseOut),
                        initialOffsetY = { ShirohaMotion.PageTransitionOffsetPx }
                    )) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = ShirohaMotion.PageFadeOutMillis)
                    )
                },
                contentAlignment = Alignment.TopStart,
                label = "main_tabs"
            ) { tab ->
                when (tab) {
                    MainTab.Home -> HomeScreen(
                        onGoImport = { currentTab = MainTab.Import },
                        onGoPractice = { currentTab = MainTab.Practice },
                        onGoExam = { currentTab = MainTab.Exam },
                        onOpenBankList = { currentTab = MainTab.BankList },
                        onOpenBankDetail = { bankId ->
                            detailBankId = bankId
                            currentTab = MainTab.BankDetail
                        },
                        onOpenWrongBook = { currentTab = MainTab.WrongBook },
                        onOpenRecords = { currentTab = MainTab.Records }
                    )

                    MainTab.Practice -> PracticeScreen(
                        onGoExam = { currentTab = MainTab.Exam },
                        onOpenRecords = { currentTab = MainTab.Records }
                    )
                    MainTab.Import -> ImportScreen(onImportSaved = { currentTab = MainTab.Home })
                    MainTab.Me -> MeScreen(
                        onOpenWrongBook = { currentTab = MainTab.WrongBook },
                        onOpenRecords = { currentTab = MainTab.Records },
                        onOpenStandardFormat = { currentTab = MainTab.StandardFormat },
                        onOpenAbout = { currentTab = MainTab.About }
                    )
                    MainTab.Exam -> ExamScreen(
                        onBackHome = { currentTab = MainTab.Home },
                        onGoPractice = { currentTab = MainTab.Practice }
                    )
                    MainTab.BankList -> BankListScreen(
                        onBack = { currentTab = MainTab.Home },
                        onOpenBankDetail = { bankId ->
                            detailBankId = bankId
                            currentTab = MainTab.BankDetail
                        }
                    )
                    MainTab.BankDetail -> BankDetailScreen(
                        bankId = detailBankId,
                        onBack = { currentTab = MainTab.Home },
                        onGoPractice = { currentTab = MainTab.Practice },
                        onGoExam = { currentTab = MainTab.Exam },
                        onOpenReview = { currentTab = MainTab.BankReview }
                    )
                    MainTab.BankReview -> BankReviewScreen(
                        bankId = detailBankId,
                        onBack = { currentTab = MainTab.BankDetail }
                    )
                    MainTab.WrongBook -> WrongBookScreen(
                        onBack = { currentTab = MainTab.Home },
                        onGoPractice = { currentTab = MainTab.Practice }
                    )
                    MainTab.Records -> RecordsScreen(
                        onBack = { currentTab = MainTab.Home },
                        onOpenRecord = { recordId ->
                            detailRecordId = recordId
                            currentTab = MainTab.RecordDetail
                        }
                    )
                    MainTab.RecordDetail -> RecordDetailScreen(
                        recordId = detailRecordId,
                        onBack = { currentTab = MainTab.Records }
                    )
                    MainTab.StandardFormat -> StandardImportFormatScreen(
                        onBack = { currentTab = MainTab.Me }
                    )
                    MainTab.About -> AboutScreen(
                        onBack = { currentTab = MainTab.Me }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShirohaBottomNavItem(
    tab: MainTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(durationMillis = ShirohaMotion.BottomNavMillis),
        label = "bottom_nav_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else ShirohaColors.TextSecondary,
        animationSpec = tween(durationMillis = ShirohaMotion.BottomNavMillis),
        label = "bottom_nav_color"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) ShirohaDimens.BottomNavIconSelectedScale else 1f,
        animationSpec = tween(durationMillis = ShirohaMotion.BottomNavMillis),
        label = "bottom_nav_icon_scale"
    )
    val shape = RoundedCornerShape(ShirohaRadius.Md)

    Surface(
        modifier = modifier
            .height(ShirohaDimens.BottomNavItemHeight)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ShirohaDimens.BottomNavItemHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = contentColor,
                modifier = Modifier
                    .size(ShirohaDimens.BottomNavIconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Text(
                text = tab.title,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
