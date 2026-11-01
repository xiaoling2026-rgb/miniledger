package com.miniledger

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地账本存储：把记录以 JSON 数组形式写入 App 私有目录 ledger.json。
 * 单数据源，H5 页面与通知监听服务都读写这里。
 */
object LedgerStore {

    private const val FILE = "ledger.json"

    fun load(context: Context): List<PaymentRecord> {
        return try {
            val path = context.getFileStreamPath(FILE)
            if (!path.exists()) return emptyList()
            val arr = JSONArray(path.readText())
            (0 until arr.length()).map { jsonToRecord(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun append(context: Context, rec: PaymentRecord) {
        val list = load(context).toMutableList()
        list.add(rec)
        save(context, list)
    }

    fun delete(context: Context, id: String) = save(context, load(context).filter { it.id != id })

    fun clear(context: Context) = save(context, emptyList())

    fun saveAll(context: Context, list: List<PaymentRecord>) = save(context, list)

    fun toJson(list: List<PaymentRecord>): String {
        val arr = JSONArray()
        list.forEach { arr.put(recordToJson(it)) }
        return arr.toString()
    }

    private fun save(context: Context, list: List<PaymentRecord>) {
        val json = JSONArray().apply { list.forEach { put(recordToJson(it)) } }.toString()
        context.openFileOutput(FILE, Context.MODE_PRIVATE).use { it.write(json.toByteArray()) }
    }

    private fun recordToJson(r: PaymentRecord) = JSONObject().apply {
        put("id", r.id); put("amt", r.amt); put("mer", r.mer); put("item", r.item)
        put("cat", r.cat); put("pay", r.pay); put("dt", r.dt); put("note", r.note); put("auto", r.auto)
    }

    private fun jsonToRecord(o: JSONObject) = PaymentRecord(
        id = o.optString("id"),
        amt = o.optDouble("amt"),
        mer = o.optString("mer", "未命名"),
        item = o.optString("item", ""),
        cat = o.optString("cat", "其他"),
        pay = o.optString("pay", "其他"),
        dt = o.optString("dt"),
        note = o.optString("note"),
        auto = o.optBoolean("auto", true)
    )
}
