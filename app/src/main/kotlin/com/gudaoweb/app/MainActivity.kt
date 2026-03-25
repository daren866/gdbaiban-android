package com.gudaoweb.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    // 默认状态栏/导航栏颜色
    private val defaultStatusBarColor = "#66CCFF"
    private val defaultNavBarColor = "#66CCFF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarColorCompat(
            Color.parseColor(defaultStatusBarColor),
            Color.parseColor(defaultNavBarColor)
        )

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            MainActivityContent()
        }
    }

    fun setStatusBarColorFromWeb(statusColor: Int, navColor: Int? = null) {
        setStatusBarColorCompat(statusColor, navColor ?: statusColor)
    }

    // 版本兼容核心方法
    private fun setStatusBarColorCompat(statusColor: Int, navColor: Int) {
        if (Build.VERSION.SDK_INT >= 36) { // Android 16 = API 36
            // 使用自定义View方案
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            // 移除旧覆盖层
            removeOverlay("status_bar_overlay")
            removeOverlay("nav_bar_overlay")
            // 添加新覆盖层
            addStatusBarOverlay(statusColor)
            addNavigationBarOverlay(navColor)
        } else {
            // 使用系统API
            window.statusBarColor = statusColor
            window.navigationBarColor = navColor
            // 移除可能遗留的自定义View
            removeOverlay("status_bar_overlay")
            removeOverlay("nav_bar_overlay")
            // 确保内容不延伸
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    // 获取状态栏高度
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

    // 获取导航栏高度
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

    // 添加导航栏覆盖层（修正版：使用 FrameLayout.LayoutParams 并设置 Gravity.BOTTOM）
    private fun addNavigationBarOverlay(color: Int) {
        val navBarHeight = getNavigationBarHeight()
        if (navBarHeight <= 0) return

        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val navBarOverlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                navBarHeight,
                Gravity.BOTTOM
            )
            setBackgroundColor(color)
            tag = "nav_bar_overlay"
        }
        rootView.addView(navBarOverlay)
    }

    // 移除覆盖层
    private fun removeOverlay(tag: String) {
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        rootView.findViewWithTag<View>(tag)?.let { rootView.removeView(it) }
    }
}
