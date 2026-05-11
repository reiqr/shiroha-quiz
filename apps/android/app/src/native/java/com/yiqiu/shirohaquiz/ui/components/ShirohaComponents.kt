package com.yiqiu.shirohaquiz.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yiqiu.shirohaquiz.R
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaRadius
import com.yiqiu.shirohaquiz.ui.theme.ShirohaSpacing

@Composable
fun ShirohaHeader(
    kicker: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ShirohaSpacing.Sm)
    ) {
        Text(
            text = kicker,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShirohaRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = ShirohaColors.CardGlass),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(ShirohaSpacing.Xl),
            content = content
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    selected: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (selected) ShirohaColors.BrandPrimarySoft else Color.White.copy(alpha = 0.74f),
        border = BorderStroke(
            1.dp,
            if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun ActionPillButton(
    icon: ImageVector,
    text: String,
    primary: Boolean = true,
    modifier: Modifier = Modifier,
    fillWidthContent: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Pill),
        color = if (primary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.84f),
        border = if (primary) null else BorderStroke(1.dp, ShirohaColors.LineStrong)
    ) {
        Row(
            modifier = (if (fillWidthContent) Modifier.fillMaxSize() else Modifier)
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (fillWidthContent) Arrangement.Center else Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(18.dp),
                tint = if (primary) Color.White else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MetricGlassCard(
    label: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(ShirohaSpacing.Xs))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ShortcutGlassCard(
    title: String,
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuizOptionCard(
    label: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(ShirohaRadius.Lg),
        color = if (selected) ShirohaColors.BrandPrimarySoft else Color.White.copy(alpha = 0.84f),
        border = BorderStroke(
            1.dp,
            if (selected) ShirohaColors.LineSelected else ShirohaColors.LineSoft
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFF3F5FA),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) {
                Spacer(Modifier.weight(1f))
                StatusChip("已选", selected = true)
            }
        }
    }
}

@Composable
fun UploadPanel(
    title: String,
    desc: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, ShirohaColors.LineSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ShirohaSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoticeCard(
    text: String,
    warning: Boolean = true
) {
    val background = if (warning) ShirohaColors.StateWarningSoft else Color.White.copy(alpha = 0.78f)
    val foreground = if (warning) Color(0xFF9A6700) else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = background
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = foreground
        )
    }
}

@Composable
fun IllustrationHeroCard(
    title: String,
    subtitle: String,
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    imageSize: Dp = 96.dp,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    GlassCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                content()
            }
            Surface(
                shape = RoundedCornerShape(ShirohaRadius.Lg),
                color = Color.White.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, ShirohaColors.LineSoft)
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(imageSize),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun EmptyStateIllustration(
    title: String,
    message: String,
    @DrawableRes imageRes: Int = R.drawable.illus_empty_state,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(140.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action?.invoke()
        }
    }
}

@Composable
fun LoadingIllustration(
    text: String,
    @DrawableRes imageRes: Int = R.drawable.illus_loading_state
) {
    val transition = rememberInfiniteTransition(label = "loading_illus")
    val scale = transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_scale"
    )

    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(112.dp)
                    .scale(scale.value),
                contentScale = ContentScale.Fit
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
