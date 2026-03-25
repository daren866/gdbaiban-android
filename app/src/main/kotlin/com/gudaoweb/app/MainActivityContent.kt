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
        val activity = context as? MainActivity ?: return
        activity.runOnUiThread {
            try {
                val color = Color.parseColor(colorCode)
                // 状态栏和导航栏都设置为同一颜色
                activity.setStatusBarColorFromWeb(color, color)
            } catch (e: Exception) {
                // 忽略颜色格式错误
            }
        }
    }
}
