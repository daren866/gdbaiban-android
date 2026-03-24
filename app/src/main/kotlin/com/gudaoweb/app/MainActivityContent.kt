package com.gudaoweb.app

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat

@Composable
fun MainActivityContent() {
    val context = LocalContext.current

    // 强制竖屏
    (context as? android.app.Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // 注入全局函数
                        view?.evaluateJavascript(
                            """
                            window.dialog = function(message, title) {
                                Android.showDialog(title, message);
                            };
                            window.statusbar = window.statusbar || {};
                            window.statusbar.color = function(colorCode) {
                                Android.setStatusBarColor(colorCode);
                            };
                            """.trimIndent(),
                            null
                        )
                    }
                }

                addJavascriptInterface(JavaScriptInterface(context), "Android")

                loadDataWithBaseURL(
                    null,
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                margin: 0;
                                padding: 16px;
                                font-family: sans-serif;
                            }
                            button {
                                margin-top: 12px;
                                padding: 8px 16px;
                                font-size: 16px;
                                margin-right: 8px;
                            }
                            .button-group {
                                margin-top: 12px;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>你好web世界！</h1>
                        <p>古道web框架哦。</p>
                        <button type="button" onclick="dialog('test dialog', '测试函数哦')">函数测试</button>
                        <div class="button-group">
                            <button type="button" onclick="statusbar.color('#FFFFFF')">设置白色状态栏</button>
                            <button type="button" onclick="statusbar.color('#66CCFF')">设置蓝色状态栏</button>
                        </div>
                    </body>
                    </html>
                    """.trimIndent(),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

/**
 * JavaScript 接口类，用于与原生 Android 交互
 */
private class JavaScriptInterface(private val context: android.content.Context) {
    @JavascriptInterface
    fun showDialog(title: String, message: String) {
        (context as? android.app.Activity)?.runOnUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    @JavascriptInterface
    fun setStatusBarColor(colorCode: String) {
        val activity = context as? android.app.Activity ?: return
        activity.runOnUiThread {
            try {
                val color = Color.parseColor(colorCode)
                activity.window?.apply {
                    statusBarColor = color
                    navigationBarColor = color
                    val windowInsetsController = WindowCompat.getInsetsController(this, decorView)
                    val isLight = Color.luminance(color) > 0.5
                    windowInsetsController.isAppearanceLightStatusBars = isLight
                    windowInsetsController.isAppearanceLightNavigationBars = isLight
                }
            } catch (e: Exception) {
                // 颜色格式错误时忽略
            }
        }
    }
}
