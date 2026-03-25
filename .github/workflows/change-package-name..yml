package com.gudaoweb.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val defaultStatusBarColor = "#66CCFF"
    private val defaultNavBarColor = "#66CCFF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarColorCompat(
            Color.parseColor(defaultStatusBarColor),
            Color.parseColor(defaultNavBarColor)
        )

        setContent {
            MainActivityContent()
        }
    }

    /**
     * 供网页调用的公开方法，设置状态栏和导航栏颜色（两者相同）
     */
    fun setStatusBarColorFromWeb(statusColor: Int, navColor: Int? = null) {
        setStatusBarColorCompat(statusColor, navColor ?: statusColor)
    }

    // ========== 版本兼容核心方法 ==========
    private fun setStatusBarColorCompat(statusColor: Int, navColor: Int) {
        if (Build.VERSION.SDK_INT >= 36) { // Android 16 = API 36
            // 使用自定义 View 方案
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false) // 让内容延伸到系统栏下，以便覆盖

            removeOverlay("status_bar_overlay")
            removeOverlay("nav_bar_overlay")
            addStatusBarOverlay(statusColor)
            addNavigationBarOverlay(navColor)

            // 根据颜色亮度自动调整状态栏图标颜色
            setSystemBarIconColor(statusColor, navColor)
        } else {
            // 使用系统 API
            window.statusBarColor = statusColor
            window.navigationBarColor = navColor
            removeOverlay("status_bar_overlay")
            removeOverlay("nav_bar_overlay")
            WindowCompat.setDecorFitsSystemWindows(window, true) // 恢复内容不延伸

            // 系统 API 自动处理图标颜色，但为了统一，也调用一次
            setSystemBarIconColor(statusColor, navColor)
        }
    }

    /**
     * 设置状态栏/导航栏图标颜色（浅色背景 -> 深色图标，深色背景 -> 浅色图标）
     */
    private fun setSystemBarIconColor(statusColor: Int, navColor: Int) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isLight = Color.luminance(statusColor) > 0.5
        windowInsetsController.isAppearanceLightStatusBars = isLight
        // 导航栏图标颜色通常跟随状态栏，但也可以单独判断
        val isNavLight = Color.luminance(navColor) > 0.5
        windowInsetsController.isAppearanceLightNavigationBars = isNavLight
    }

    // ========== 辅助方法（获取高度、添加/移除覆盖层） ==========
    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                resources.displayMetrics
            ).toInt()
        }
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                resources.displayMetrics
            ).toInt()
        }
    }

    private fun addStatusBarOverlay(color: Int) {
        val statusBarHeight = getStatusBarHeight()
        if (statusBarHeight <= 0) return

        // 使用 decorView 作为父容器，确保覆盖层在最上层
        val decorView = window.decorView as ViewGroup
        val statusBarOverlay = View(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                resources.displayMetrics.widthPixels,
                statusBarHeight
            )
            setBackgroundColor(color)
            tag = "status_bar_overlay"
        }
        decorView.addView(statusBarOverlay)
    }

    private fun addNavigationBarOverlay(color: Int) {
        val navBarHeight = getNavigationBarHeight()
        if (navBarHeight <= 0) return

        val decorView = window.decorView as ViewGroup
        val navBarOverlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                navBarHeight,
                android.view.Gravity.BOTTOM
            )
            setBackgroundColor(color)
            tag = "nav_bar_overlay"
        }
        decorView.addView(navBarOverlay)
    }

    private fun removeOverlay(tag: String) {
        val decorView = window.decorView as? ViewGroup ?: return
        decorView.findViewWithTag<View>(tag)?.let { decorView.removeView(it) }
    }
}
