package com.yiqiu.shirohaquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yiqiu.shirohaquiz.state.QuizRepository
import com.yiqiu.shirohaquiz.ui.app.ShirohaAppShell
import com.yiqiu.shirohaquiz.ui.theme.ShirohaColors
import com.yiqiu.shirohaquiz.ui.theme.ShirohaMotion
import com.yiqiu.shirohaquiz.ui.theme.ShirohaQuizTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        QuizRepository.init(applicationContext)
        setContent {
            ShirohaQuizTheme {
                ShirohaStartupGate {
                    ShirohaAppShell()
                }
            }
        }
    }
}

@Composable
private fun ShirohaStartupGate(
    content: @Composable () -> Unit
) {
    val shouldShowStartupSplash = remember { QuizRepository.startupSplashEnabled }
    var showSplash by remember { mutableStateOf(shouldShowStartupSplash) }

    LaunchedEffect(shouldShowStartupSplash) {
        if (shouldShowStartupSplash) {
            delay(ShirohaMotion.SplashHoldMillis)
            showSplash = false
        } else {
            showSplash = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(durationMillis = ShirohaMotion.SplashFadeMillis))
        ) {
            ShirohaSplashArtwork()
        }
    }
}

@Composable
private fun ShirohaSplashArtwork() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShirohaColors.BgApp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_screen_study),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        ShirohaSplashNativeOverlay()
    }
}

@Composable
private fun ShirohaSplashNativeOverlay() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = maxHeight * 0.714f,
                    start = 40.dp,
                    end = 40.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Shiroha",
                    color = Color(0xFF1F5EA8),
                    fontSize = 42.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Quiz",
                    color = Color(0xFFFF78AD),
                    fontSize = 42.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "让自律成为习惯，让成长看得见",
                color = Color(0xFF7C9ED6),
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(30.dp))
            ShirohaSplashProgressBar(
                modifier = Modifier
                    .width(maxWidth * 0.54f)
                    .height(18.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "正在加载中…",
                color = Color(0xFF7C9ED6),
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShirohaSplashProgressBar(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.46f))
            .border(1.dp, Color(0xFFC8D8F4).copy(alpha = 0.62f), shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.48f)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF6EA7),
                            Color(0xFFFFA9CF)
                        )
                    )
                )
        )
        Text(
            text = "✦",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 9.dp)
        )
    }
}
