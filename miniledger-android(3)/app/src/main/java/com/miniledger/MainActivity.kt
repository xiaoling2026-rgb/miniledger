package com.miniledger

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

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
            if (::webView.isInitialized) {
                webView.post { webView.evaluateJavascript("window.__miniLedgerIngest($json)") {} }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val boot = TextView(this)
        boot.setPadding(48, 48, 48, 48)
        boot.text = "消费记账本启动中…"
        setContentView(boot)

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                setup()
            } catch (e: Throwable) {
                boot.text = "启动失败：\n${e.javaClass.name}: ${e.message}\n\n${e.stackTraceToString().take(2000)}"
                setContentView(boot)
            }
        }, 600)
    }

    private fun setup() {
        webView = WebView(this)
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, IntentFilter("com.miniledger.NEW_RECORD"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, IntentFilter("com.miniledger.NEW_RECORD"))
        }
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                if (enabled.isNullOrEmpty() || !enabled.contains(packageName)) {
                    Toast.makeText(this, "请开启「通知使用权」以自动记账", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            } catch (_: Throwable) {
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(receiver) }
    }
}
