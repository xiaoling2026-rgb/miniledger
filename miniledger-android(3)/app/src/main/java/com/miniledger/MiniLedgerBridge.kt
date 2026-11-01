package com.miniledger

import android.webkit.JavascriptInterface
import org.json.JSONArray

/**
 * JS 桥：让 WebView 内的 H5 页面把「手动新增 / 删除 / 清空」同步回安卓本地文件，
 * 保证 H5(localStorage) 与服务端文件是同一份数据。
 */
class MiniLedgerBridge(
    private val context: android.content.Context,
    private val onChanged: () -> Unit
) {

    @JavascriptInterface
    fun saveRecords(json: String) {
        try {
            val arr = JSONArray(json)
            val records = (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                PaymentRecord(
                    id = o.optString("id"),
                    amt = o.optDouble("amt"),
                    mer = o.optString("mer", "未命名"),
                    item = o.optString("item", ""),
                    cat = o.optString("cat", "其他"),
                    pay = o.optString("pay", "其他"),
                    dt = o.optString("dt"),
                    note = o.optString("note"),
                    auto = o.optBoolean("auto", false)
                )
            }
            LedgerStore.saveAll(context, records)
            onChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun deleteRecord(id: String) {
        LedgerStore.delete(context, id)
        onChanged()
    }

    @JavascriptInterface
    fun clearAll() {
        LedgerStore.clear(context)
        onChanged()
    }
}
