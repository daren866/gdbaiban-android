package com.gudaoweb.app

import android.content.pm.ActivityInfo
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

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
                    javaScriptEnabled = true          // 启用 JavaScript
                    domStorageEnabled = true          // 启用 DOM 存储（可选）
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // 注入全局 JavaScript 函数 dialog，该函数调用原生 Android 接口
                        view?.evaluateJavascript(
                            """
                            window.dialog = function(message, title) {
                                Android.showDialog(title, message);
                            };
                            """.trimIndent(),
                            null
                        )
                    }
                }

                // 添加 JavaScript 接口，对象名为 Android
                addJavascriptInterface(JavaScriptInterface(context), "Android")

                // 加载 HTML 内容（包含按钮）
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
                            }
                        </style>
                    </head>
                    <body>
                        <h1>你好web世界！</h1>
                        <p>古道web框架哦。</p>
                        <button type="button" onclick="dialog('test dialog', '测试函数哦')">函数测试</button>
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
        // 确保对话框在主线程显示
        (context as? android.app.Activity)?.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
