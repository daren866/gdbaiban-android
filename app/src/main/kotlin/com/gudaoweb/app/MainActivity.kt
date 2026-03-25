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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始设置状态栏颜色
        setStatusBarColorFromWeb(Color.parseColor("#66CCFF"), Color.parseColor("#66CCFF"))

        setContent {
            MainActivityContent()
        }
    }

    // 供 JavaScript 调用的公开方法
    fun setStatusBarColorFromWeb(statusColor: Int, navColor: Int? = null) {
        setStatusBarColorCompat(statusColor, navColor ?: statusColor)
    }

    // 版本兼容的核心方法
    private fun setStatusBarColorCompat(statusColor: Int, navColor: Int) {
        if (Build.VERSION.SDK_INT >= 36) { // Android 16 = API 36
            // 使用自定义 View 方案
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            // 移除旧覆盖层
            removeOverlay("status_bar_overlay")
            removeOverlay("nav_bar_overlay")
            // 添加新覆盖层
            addStatusBarOverlay(statusColor)
            addNavigationBarOverlay(navColor)
        } else {
            // 使用系统 API
            window.statusBarColor = statusColor
            window.navigationBarColor = navColor
            // 确保移除可能遗留的自定义 View
            removeOverlay("status_bar_overlay")
            removeOverlay("nav_bar_overlay")
            // 确保内容不延伸
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    // 辅助方法：获取状态栏高度
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

    // 辅助方法：获取导航栏高度
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

    // 添加状态栏覆盖层
    private fun addStatusBarOverlay(color: Int) {
        val statusBarHeight = getStatusBarHeight()
        if (statusBarHeight <= 0) return

        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val statusBarOverlay = View(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                resources.displayMetrics.widthPixels,
                statusBarHeight
            )
            setBackgroundColor(color)
            tag = "status_bar_overlay"
        }
        rootView.addView(statusBarOverlay)
    }

    // 添加导航栏覆盖层
    private fun addNavigationBarOverlay(color: Int) {
        val navBarHeight = getNavigationBarHeight()
        if (navBarHeight <= 0) return

        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val navBarOverlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                navBarHeight,
                android.view.Gravity.BOTTOM
            )
            setBackgroundColor(color)
            tag = "nav_bar_overlay"
        }
        rootView.addView(navBarOverlay)
    }

    // 移除指定 tag 的覆盖层
    private fun removeOverlay(tag: String) {
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        rootView.findViewWithTag<View>(tag)?.let { rootView.removeView(it) }
    }
}
