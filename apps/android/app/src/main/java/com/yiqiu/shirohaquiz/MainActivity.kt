package com.yiqiu.shirohaquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yiqiu.shirohaquiz.ui.app.ShirohaAppShell
import com.yiqiu.shirohaquiz.ui.theme.ShirohaQuizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            ShirohaQuizTheme {
                ShirohaAppShell()
            }
        }
    }
}
