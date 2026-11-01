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
    private var receiverRegistered = false

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

        val tv = TextView(this)
        tv.setPadding(48, 48, 48, 48)
        tv.textSize = 20f
        tv.text = "① 程序已启动\n（正在准备页面…）"
        setContentView(tv)

        Handler(Looper.getMainLooper()).postDelayed({ startWebView() }, 1500)
    }

    private fun startWebView() {
        try {
            webView = WebView(this)
            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
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
                    if (url != null && url.contains("index.html")) {
                        view?.evaluateJavascript(
                            "(function(){var s=document.createElement('style');s.textContent='*{backdrop-filter:none!important;-webkit-backdrop-filter:none!important;}';document.head.appendChild(s);})()"
                        ) {}
                        val existing = LedgerStore.toJson(LedgerStore.load(this@MainActivity))
                        view?.evaluateJavascript("window.__miniLedgerLoad($existing)") {}
                        registerBroadcastOnce()
                    }
                }
            }

            setContentView(webView)
            webView.loadData(
                "<html><body style='font-size:28px;padding:40px;font-family:sans-serif'>② WebView 正常</body></html>",
                "text/html",
                "utf-8"
            )

            Handler(Looper.getMainLooper()).postDelayed({
                webView.loadUrl("file:///android_asset/index.html")
            }, 2500)
        } catch (e: Throwable) {
            showError(e)
        }
    }

    private fun registerBroadcastOnce() {
        if (receiverRegistered) return
        receiverRegistered = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, IntentFilter("com.miniledger.NEW_RECORD"), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(receiver, IntentFilter("com.miniledger.NEW_RECORD"))
            }
        } catch (_: Throwable) {
        }
    }

    private fun showError(e: Throwable) {
        val tv = TextView(this)
        tv.setPadding(48, 48, 48, 48)
        tv.textSize = 14f
        tv.text = "启动失败：\n${e.javaClass.name}\n${e.message}\n\n${e.stackTraceToString().take(1500)}"
        setContentView(tv)
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
