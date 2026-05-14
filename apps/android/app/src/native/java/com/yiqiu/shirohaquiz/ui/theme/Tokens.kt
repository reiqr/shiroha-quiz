package com.yiqiu.shirohaquiz.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ShirohaColors {
    val BgApp = Color(0xFFF4F6FB)
    val BgElevated = Color(0xFFFBFCFE)
    val BgDeepFocus = Color(0xFF0E1627)

    val BgGradientTop = Color(0xFFF7F9FF)
    val BgGradientMiddle = Color(0xFFF1F4FB)
    val BgGradientBottom = Color(0xFFF6F7FB)

    val CardGlass = Color(0xC7FFFFFF)
    val CardSoft = Color(0xE6FFFFFF)
    val CardMuted = Color(0xFFF7F9FD)
    val CardWhite84 = Color.White.copy(alpha = 0.84f)
    val CardWhite86 = Color.White.copy(alpha = 0.86f)
    val CardWhite78 = Color.White.copy(alpha = 0.78f)
    val CardWhite72 = Color.White.copy(alpha = 0.72f)
    val CardWhite68 = Color.White.copy(alpha = 0.68f)
    val CardWhite62 = Color.White.copy(alpha = 0.62f)
    val BottomBar = Color.White.copy(alpha = 0.82f)

    val LineSoft = Color(0x38A0AEC0)
    val LineStrong = Color(0xFFD8E0EF)
    val LineSelected = Color(0xFF85A7FF)

    val TextPrimary = Color(0xFF101828)
    val TextSecondary = Color(0xFF667085)
    val TextTertiary = Color(0xFF94A3B8)
    val TextOnBrand = Color.White
    val TextWarning = Color(0xFF9A6700)
    val IconWarning = Color(0xFFE29A00)

    val BrandPrimary = Color(0xFF4F7CFF)
    val BrandPrimarySoft = Color(0xFFEAF0FF)
    val BrandSecondary = Color(0xFF6C8EEA)

    val OptionLabelIdle = Color(0xFFF3F5FA)

    val StateSuccess = Color(0xFF17B26A)
    val StateSuccessSoft = Color(0xFFDDF7EA)
    val StateWarning = Color(0xFFF79009)
    val StateWarningSoft = Color(0xFFFFF7E8)
    val StateDanger = Color(0xFFF04438)
    val StateDangerSoft = Color(0xFFFEECEC)
}

object ShirohaRadius {
    val Sm = 14.dp
    val Md = 18.dp
    val Lg = 24.dp
    val Xl = 30.dp
    val Pill = 999.dp
}

object ShirohaSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp
}

object ShirohaDimens {
    val Hairline = 1.dp
    val PageHorizontalPadding = ShirohaSpacing.Xl
    val PageVerticalPadding = ShirohaSpacing.Sm

    val HeroCardHeight = 132.dp
    val HeroImageSize = 92.dp
    val HeroImageFrameSize = 100.dp
    val HeroImageFrameExtra = 8.dp
    val HeroImageAlpha = 0.92f

    val StepPillWidth = 136.dp
    val StepPillMinHeight = 28.dp
    val StepPillHorizontalPadding = 10.dp
    val StepPillVerticalPadding = 5.dp

    val StatusChipMinHeight = 32.dp
    val StatusChipHorizontalPadding = 12.dp
    val StatusChipVerticalPadding = 7.dp

    val ActionButtonMinHeight = 44.dp
    val ActionButtonIconSize = 17.dp
    val ActionButtonHorizontalPadding = 14.dp
    val ActionButtonEqualHorizontalPadding = 10.dp
    val ActionButtonIconTextGap = 7.dp
    val ActionButtonEqualIconTextGap = 6.dp
    val DisabledAlpha = 0.52f

    val OptionCardHorizontalPadding = 14.dp
    val OptionCardVerticalPadding = 14.dp
    val OptionLabelSize = 36.dp
    val OptionLabelTextGap = 12.dp

    val BottomBarHorizontalPadding = 12.dp
    val BottomBarVerticalPadding = 4.dp
    val BottomNavItemGap = 6.dp
    val BottomNavItemHeight = 56.dp
    val BottomNavItemHorizontalPadding = 4.dp
    val BottomNavIconSize = 21.dp
    val BottomNavIconSelectedScale = 1.05f

    val EmptyStateImageSize = 140.dp
    val LoadingImageSize = 112.dp
}

object ShirohaMotion {
    const val PageTransitionMillis = 180
    const val PageFadeOutMillis = 90
    const val BottomNavMillis = 140
    const val HeroFloatMillis = 1700
    const val LoadingScaleMillis = 900
    const val SplashFadeMillis = 420
    const val SplashHoldMillis = 1450L

    const val PageTransitionOffsetPx = 6
    val HeroFloatDistance = 2.6.dp
    const val LoadingScaleMin = 0.98f
    const val LoadingScaleMax = 1.02f
}
