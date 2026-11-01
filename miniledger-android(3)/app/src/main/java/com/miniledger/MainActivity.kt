package com.miniledger

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        filePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else emptyArray())
        filePathCallback = null
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val json = intent?.getStringExtra("record") ?: return
            webView.post { webView.evaluateJavascript("window.__miniLedgerIngest($json)") {} }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(MiniLedgerBridge(this) {}, "MiniLedgerBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                wv: WebView?,
                cb: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback = cb
                pickFile.launch("*/*")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val existing = LedgerStore.toJson(LedgerStore.load(this@MainActivity))
                view?.evaluateJavascript("window.__miniLedgerLoad($existing)") {}
            }
        }
        webView.loadUrl("file:///android_asset/index.html")

        // 注意：Android 13+ 注册广播必须显式声明导出标志，否则启动即崩溃
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter("com.miniledger.NEW_RECORD"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            if (enabled.isNullOrEmpty() || !enabled.contains(packageName)) {
                Toast.makeText(this, "请开启「通知使用权」以自动记账", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(receiver) }
    }
}
