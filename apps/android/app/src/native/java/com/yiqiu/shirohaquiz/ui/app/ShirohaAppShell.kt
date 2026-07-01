package com.yiqiu.shirohaquiz.ui.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.screens.AboutScreen
import com.yiqiu.shirohaquiz.ui.screens.AiSettingsScreen
import com.yiqiu.shirohaquiz.ui.screens.BankDetailScreen
import com.yiqiu.shirohaquiz.ui.screens.BankListScreen
import com.yiqiu.shirohaquiz.ui.screens.BankReviewScreen
import com.yiqiu.shirohaquiz.ui.screens.DataManagementScreen
import com.yiqiu.shirohaquiz.ui.screens.ExamScreen
import com.yiqiu.shirohaquiz.ui.screens.FavoriteScreen
import com.yiqiu.shirohaquiz.ui.screens.HomeScreen
import com.yiqiu.shirohaquiz.ui.screens.ImportScreen
import com.yiqiu.shirohaquiz.ui.screens.MeScreen
import com.yiqiu.shirohaquiz.ui.screens.AppearancePreferenceScreen
import com.yiqiu.shirohaquiz.ui.screens.PracticePreferenceScreen
import com.yiqiu.shirohaquiz.ui.screens.PracticeQuickEditScreen
import com.yiqiu.shirohaquiz.ui.screens.QuestionSearchScreen
import com.yiqiu.shirohaquiz.ui.screens.PracticeScreen
import com.yiqiu.shirohaquiz.ui.screens.RecordDetailScreen
import com.yiqiu.shirohaquiz.ui.screens.RecordsScreen
import com.yiqiu.shirohaquiz.ui.screens.StandardImportFormatScreen
import com.yiqiu.shirohaquiz.ui.screens.WrongBookPreferenceScreen
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
    QuestionSearch("题目搜索", Icons.Rounded.Search, showInBottomBar = false),
    BankDetail("题库详情", Icons.Rounded.Dashboard, showInBottomBar = false),
    BankReview("题库核对", Icons.Rounded.Dashboard, showInBottomBar = false),
    WrongBook("错题本", Icons.Rounded.School, showInBottomBar = false),
    Favorites("收藏夹", Icons.Rounded.School, showInBottomBar = false),
    Records("记录", Icons.Rounded.Dashboard, showInBottomBar = false),
    RecordDetail("记录详情", Icons.Rounded.Dashboard, showInBottomBar = false),
    AppearancePreference("外观偏好", Icons.Rounded.Settings, showInBottomBar = false),
    PracticePreference("刷题偏好", Icons.Rounded.School, showInBottomBar = false),
    PracticeQuickEdit("快速编辑", Icons.Rounded.School, showInBottomBar = false),
    WrongBookPreference("错题本设置", Icons.Rounded.School, showInBottomBar = false),
    AiSettings("AI 设置", Icons.Rounded.Settings, showInBottomBar = false),
    DataManagement("数据管理", Icons.Rounded.Settings, showInBottomBar = false),
    StandardFormat("标准格式", Icons.Rounded.ImportExport, showInBottomBar = false),
    About("关于", Icons.Rounded.Settings, showInBottomBar = false)
}

private fun MainTab.fallbackBackTarget(): MainTab? = when (this) {
    MainTab.Home,
    MainTab.Practice,
    MainTab.Import,
    MainTab.Me -> null

    MainTab.Exam,
    MainTab.BankList,
    MainTab.WrongBook,
    MainTab.Favorites,
    MainTab.Records -> MainTab.Home

    MainTab.QuestionSearch,
    MainTab.BankDetail -> MainTab.BankList
    MainTab.BankReview -> MainTab.BankDetail
    MainTab.RecordDetail -> MainTab.Records
    MainTab.PracticeQuickEdit -> MainTab.Practice
    MainTab.AppearancePreference,
    MainTab.PracticePreference,
    MainTab.WrongBookPreference,
    MainTab.AiSettings,
    MainTab.DataManagement,
    MainTab.StandardFormat,
    MainTab.About -> MainTab.Me
}

private data class AppRouteSnapshot(
    val tab: MainTab,
    val bankId: String? = null,
    val recordId: String? = null
)

private const val ROUTE_PART_SEPARATOR = "\u001F"

private fun AppRouteSnapshot.encode(): String = listOf(
    tab.name,
    bankId.orEmpty(),
    recordId.orEmpty()
).joinToString(ROUTE_PART_SEPARATOR)

private fun decodeRouteSnapshot(value: String): AppRouteSnapshot? {
    val parts = value.split(ROUTE_PART_SEPARATOR, limit = 3)
    val tab = parts.firstOrNull()?.let { name ->
        MainTab.entries.firstOrNull { it.name == name }
    } ?: return null
    return AppRouteSnapshot(
        tab = tab,
        bankId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() },
        recordId = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }
    )
}

@Composable
fun ShirohaAppShell() {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var detailBankId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeBackStack by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedRootTab by rememberSaveable { mutableStateOf(MainTab.Home) }

    fun currentRouteSnapshot() = AppRouteSnapshot(
        tab = currentTab,
        bankId = detailBankId,
        recordId = detailRecordId
    )

    fun navigateTo(
        target: MainTab,
        bankId: String? = null,
        recordId: String? = null
    ) {
        val targetBankId = bankId ?: detailBankId
        val targetRecordId = recordId ?: detailRecordId
        if (
            currentTab == target &&
            detailBankId == targetBankId &&
            detailRecordId == targetRecordId
        ) {
            return
        }
        routeBackStack = routeBackStack + currentRouteSnapshot().encode()
        detailBankId = targetBankId
        detailRecordId = targetRecordId
        currentTab = target
    }

    fun navigateRoot(target: MainTab) {
        routeBackStack = emptyList()
        currentTab = target
        if (target.showInBottomBar) {
            selectedRootTab = target
        }
    }

    fun navigateBack() {
        val previous = routeBackStack.lastOrNull()?.let(::decodeRouteSnapshot)
        if (previous != null) {
            routeBackStack = routeBackStack.dropLast(1)
            if (previous.tab == MainTab.BankDetail || previous.tab == MainTab.BankReview) {
                detailBankId = previous.bankId
            }
            if (previous.tab == MainTab.RecordDetail) {
                detailRecordId = previous.recordId
            }
            currentTab = previous.tab
            return
        }
        currentTab.fallbackBackTarget()?.let { fallback ->
            currentTab = fallback
        }
    }

    val hasBackTarget = routeBackStack.isNotEmpty() || currentTab.fallbackBackTarget() != null
    BackHandler(enabled = hasBackTarget) {
        navigateBack()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSideNavigation = QuizRepository.tabletSideNavigationEnabled &&
            maxWidth >= 600.dp &&
            maxHeight >= 480.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useSideNavigation) {
                    BottomAppBar(
                        containerColor = ShirohaColors.BottomBar,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = ShirohaDimens.BottomBarHorizontalPadding,
                                    vertical = ShirohaDimens.BottomBarVerticalPadding
                                ),
                            horizontalArrangement = Arrangement.spacedBy(ShirohaDimens.BottomNavItemGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MainTab.entries.filter { it.showInBottomBar }.forEach { tab ->
                                ShirohaBottomNavItem(
                                    tab = tab,
                                    selected = selectedRootTab == tab,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (currentTab != tab) navigateRoot(tab)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useSideNavigation) {
                    ShirohaSideNavigation(
                        selectedTab = selectedRootTab,
                        onTabClick = { tab ->
                            if (currentTab != tab) navigateRoot(tab)
                        }
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ShirohaColors.BgGradientTop,
                                    ShirohaColors.BgGradientMiddle,
                                    ShirohaColors.BgGradientBottom
                                )
                            )
                        )
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
                            onGoImport = { navigateRoot(MainTab.Import) },
                            onGoPractice = { navigateRoot(MainTab.Practice) },
                            onGoExam = { navigateTo(MainTab.Exam) },
                            onOpenBankList = { navigateTo(MainTab.BankList) },
                            onOpenBankDetail = { bankId ->
                                navigateTo(MainTab.BankDetail, bankId = bankId)
                            },
                            onOpenWrongBook = { navigateTo(MainTab.WrongBook) },
                            onOpenFavorites = { navigateTo(MainTab.Favorites) },
                            onOpenRecords = { navigateTo(MainTab.Records) }
                        )
    
                        MainTab.Practice -> PracticeScreen(
                            onGoExam = { navigateTo(MainTab.Exam) },
                            onOpenRecords = { navigateTo(MainTab.Records) },
                            onOpenQuickEdit = { navigateTo(MainTab.PracticeQuickEdit) }
                        )
                        MainTab.PracticeQuickEdit -> PracticeQuickEditScreen(
                            onBack = { navigateBack() }
                        )
                        MainTab.Import -> ImportScreen(
                            onImportSaved = { navigateRoot(MainTab.Home) },
                            onOpenPreference = { navigateTo(MainTab.AiSettings) }
                        )
                        MainTab.Me -> MeScreen(
                            onOpenRecords = { navigateTo(MainTab.Records) },
                            onOpenAppearancePreference = { navigateTo(MainTab.AppearancePreference) },
                            onOpenPracticePreference = { navigateTo(MainTab.PracticePreference) },
                            onOpenWrongBookPreference = { navigateTo(MainTab.WrongBookPreference) },
                            onOpenAiSettings = { navigateTo(MainTab.AiSettings) },
                            onOpenDataManagement = { navigateTo(MainTab.DataManagement) },
                            onOpenStandardFormat = { navigateTo(MainTab.StandardFormat) },
                            onOpenAbout = { navigateTo(MainTab.About) }
                        )
                        MainTab.Exam -> ExamScreen(
                            onBackHome = { navigateRoot(MainTab.Home) },
                            onGoPractice = { navigateRoot(MainTab.Practice) },
                            onOpenRecord = { recordId ->
                                navigateTo(MainTab.RecordDetail, recordId = recordId)
                            }
                        )
                        MainTab.BankList -> BankListScreen(
                            onBack = { navigateBack() },
                            onOpenQuestionSearch = { navigateTo(MainTab.QuestionSearch) },
                            onOpenBankDetail = { bankId ->
                                navigateTo(MainTab.BankDetail, bankId = bankId)
                            }
                        )
                        MainTab.QuestionSearch -> QuestionSearchScreen(
                            onBack = { navigateBack() },
                            onOpenBankDetail = { bankId ->
                                navigateTo(MainTab.BankDetail, bankId = bankId)
                            }
                        )
                        MainTab.BankDetail -> BankDetailScreen(
                            bankId = detailBankId,
                            onBack = { navigateBack() },
                            onGoPractice = { navigateRoot(MainTab.Practice) },
                            onGoExam = { navigateTo(MainTab.Exam) },
                            onOpenReview = { navigateTo(MainTab.BankReview) }
                        )
                        MainTab.BankReview -> BankReviewScreen(
                            bankId = detailBankId,
                            onBack = { navigateBack() }
                        )
                        MainTab.WrongBook -> WrongBookScreen(
                            onBack = { navigateBack() },
                            onGoPractice = { navigateRoot(MainTab.Practice) }
                        )
                        MainTab.Favorites -> FavoriteScreen(
                            onBack = { navigateBack() },
                            onGoPractice = { navigateRoot(MainTab.Practice) }
                        )
                        MainTab.Records -> RecordsScreen(
                            onBack = { navigateBack() },
                            onOpenRecord = { recordId ->
                                navigateTo(MainTab.RecordDetail, recordId = recordId)
                            }
                        )
                        MainTab.RecordDetail -> RecordDetailScreen(
                            recordId = detailRecordId,
                            onBack = { navigateBack() }
                        )
                        MainTab.AppearancePreference -> AppearancePreferenceScreen(
                            onBack = { navigateBack() }
                        )
                        MainTab.PracticePreference -> PracticePreferenceScreen(
                            onBack = { navigateBack() }
                        )
                        MainTab.WrongBookPreference -> WrongBookPreferenceScreen(
                            onBack = { navigateBack() }
                        )
                        MainTab.AiSettings -> AiSettingsScreen(
                            onBack = { navigateBack() }
                        )
                        MainTab.DataManagement -> DataManagementScreen(
                            onBack = { navigateBack() }
                        )
                        MainTab.StandardFormat -> StandardImportFormatScreen(
                            onBack = { navigateBack() }
                        )
                        MainTab.About -> AboutScreen(
                            onBack = { navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ShirohaSideNavigation(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .width(88.dp)
            .fillMaxHeight(),
        containerColor = ShirohaColors.BottomBar
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        MainTab.entries.filter { it.showInBottomBar }.forEach { tab ->
            val selected = selectedTab == tab
            NavigationRailItem(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                selected = selected,
                onClick = { onTabClick(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(ShirohaDimens.BottomNavIconSize)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    unselectedIconColor = ShirohaColors.TextSecondary,
                    unselectedTextColor = ShirohaColors.TextSecondary
                )
            )
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
                contentDescription = null,
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
