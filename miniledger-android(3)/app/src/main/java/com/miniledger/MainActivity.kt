package com.miniledger

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.textSize = 24f
        tv.setPadding(48, 48, 48, 48)
        tv.text = "✅ 启动成功\n基础环境正常"
        setContentView(tv)
    }
}
