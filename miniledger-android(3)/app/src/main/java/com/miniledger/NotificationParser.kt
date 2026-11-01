package com.miniledger

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * 一条解析出来的消费记录
 */
data class PaymentRecord(
    val id: String,
    val amt: Double,
    val mer: String,
    val item: String = "",
    val cat: String,
    val pay: String,
    val dt: String,
    val note: String,
    val auto: Boolean = true
)

/**
 * 支付通知解析器：从通知标题/文本/来源包名中提取 金额、商户、支付方式、品类。
 * 覆盖：微信支付、支付宝/花呗、银行短信、美团月付。
 */
object NotificationParser {

    private val AMT_RE = Pattern.compile("[¥￥]?\\s*(\\d+(?:\\.\\d{1,2})?)")
    private val MER_RE = Pattern.compile("(?:向|在|商户[:：]?|订单[:：]?|付款给|转给)\\s*([^，,。.¥￥\\s]{2,12})")
    private val BANK_PKGS = listOf("bank", "icbc", "ccb", "abchina", "cmbchina", "cmbc", "unionpay", "boc", "ceb")

    private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    /**
     * @return 解析成功返回记录；不是支付类通知或无法提取金额返回 null
     */
    fun parse(title: String?, text: String?, pkg: String?): PaymentRecord? {
        val t = "${title ?: ""} ${text ?: ""}".trim()
        if (t.isEmpty()) return null
        val p = pkg ?: ""

        // 1) 判定支付渠道
        val pay = when {
            p.contains("alipay", true) || t.contains("支付宝") ->
                if (t.contains("花呗")) "花呗" else "支付宝"
            p.contains("meituan", true) || t.contains("美团月付") -> "美团月付"
            p.contains("tencent.mm", true) || t.contains("微信支付") -> "微信支付"
            BANK_PKGS.any { p.contains(it, true) } ||
                t.contains("银行") || t.contains("储蓄卡") ||
                t.contains("信用卡") || t.contains("尾号") -> "银行卡"
            else -> return null   // 非支付类通知，忽略
        }

        // 2) 提取金额
        val am = AMT_RE.matcher(t)
        if (!am.find()) return null
        val amt = am.group(1).toDoubleOrNull() ?: return null
        if (amt <= 0) return null

        // 3) 提取商户
        val mm = MER_RE.matcher(t)
        val mer = if (mm.find()) mm.group(1).replace(Regex("[消费付款支付订单]"), "").trim() else "未命名"

        // 4) 推断品类
        val cat = guessCategory(t, mer)

        return PaymentRecord(
            id = System.currentTimeMillis().toString(36) + (1000..9999).random().toString(),
            amt = amt,
            mer = mer,
            cat = cat,
            pay = pay,
            dt = ISO.format(Date()),
            note = "",
            auto = true
        )
    }

    /** 根据关键词粗略归类，缺省「其他」 */
    private fun guessCategory(t: String, mer: String): String {
        val s = "$t $mer"
        return when {
            s.contains(Regex("餐|吃|咖啡|奶茶|外卖|肯德基|麦当劳|星巴克|食堂|饭店|餐厅")) -> "餐饮"
            s.contains(Regex("超市|盒马|购物|京东|淘宝|拼多多|商场|便利|服饰")) -> "购物"
            s.contains(Regex("滴滴|地铁|公交|加油|高铁|火车|机票|打车|出行|停车|加油")) -> "出行"
            s.contains(Regex("电影|游戏|视频|会员|娱乐|KTV|游")) -> "娱乐"
            s.contains(Regex("药|医院|诊所|医疗|体检|养生")) -> "医疗"
            s.contains(Regex("书|课|培训|教育|学")) -> "教育"
            else -> "其他"
        }
    }
}
