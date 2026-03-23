package com.gudaoweb.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    // 自定义颜色变量，格式如 "#66CCFF"
    private val statusBarColor = "#66CCFF"      // 状态栏颜色
    private val navigationBarColor = "#66CCFF"  // 导航栏颜色（可单独设置）

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置系统栏颜色
        window.statusBarColor = Color.parseColor(statusBarColor)
        window.navigationBarColor = Color.parseColor(navigationBarColor)

        // 确保系统栏独立显示，内容不覆盖它们（默认就是 true，此处显式声明）
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // 可选：让状态栏图标和文字颜色自适应（浅色背景时变暗）
        // 如果背景色较深，可以省略下面两行，让图标保持白色
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false // false=浅色图标，true=深色图标
        windowInsetsController.isAppearanceLightNavigationBars = false

        setContent {
            MainActivityContent()
        }
    }
}
