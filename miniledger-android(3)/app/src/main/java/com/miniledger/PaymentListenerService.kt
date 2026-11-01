package com.miniledger

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject

/**
 * 通知监听服务：系统每弹出一条通知都会回调 onNotificationPosted。
 * 我们只在「支付类」通知出现时解析并静默写入本地账本，同时广播给界面刷新。
 */
class PaymentListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return
            val extras = sbn.notification.extras
            val title = extras?.getCharSequence("android.title")?.toString()
            val text = extras?.getCharSequence("android.text")?.toString()

            val rec = NotificationParser.parse(title, text, pkg) ?: return   // 非支付通知直接忽略

            // 静默写入本地文件（App 未打开也照常记录）
            LedgerStore.append(this, rec)

            // 通知前台页面刷新
            val json = JSONObject().apply {
                put("id", rec.id); put("amt", rec.amt); put("mer", rec.mer); put("item", rec.item)
                put("cat", rec.cat); put("pay", rec.pay); put("dt", rec.dt)
                put("note", rec.note); put("auto", rec.auto)
            }.toString()
            sendBroadcast(Intent("com.miniledger.NEW_RECORD").apply { putExtra("record", json) })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
