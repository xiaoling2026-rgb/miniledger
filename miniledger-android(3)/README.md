# 消费记账本 · 安卓采集端

> 📦 **这是一个「可直接 fork 的 GitHub 模板仓库」**。把它 fork 到你自己的账号后，
> 只要 `git push` 到 `main`/`master`，GitHub Actions 就会**自动编译出可安装的 APK**，
> 全程无需安装 Android Studio。详见下方「自动构建 APK」章节。

把 H5 记账页封装成安卓 App：通过 `NotificationListenerService` 监听手机支付通知，
自动解析出 **金额 / 商户 / 支付方式（花呗、美团月付、微信支付、支付宝、银行卡）/ 品类**，
**静默写入本机文件**，无需你手动点记录。明细可在 App 内查看、删除、导出。

## 装到手机桌面：两种方式（先看这个）

| 方式 | 能不能上桌面 | 能不能「自动记账」 | 难度 |
|---|---|---|---|
| **A. 安卓 App（本工程）** | ✅ 真图标，装完就在桌面 | ✅ 通知监听自动抓 | 需 Android Studio 构建安装 |
| **B. H5 加「添加到主屏幕」** | ✅ 浏览器生成图标 | ❌ 仅手动记账/导入 | 需先放到一个网址再添加 |

**方式 A（推荐，功能最全）**：用 Android Studio 打开本工程 → 构建安装 APK 到手机 → 桌面出现「消费记账本」图标，打开即用，支付通知自动入账。详见下方「构建与运行」。

**方式 B（快速体验网页版）**：
1. 把 `index.html` + `manifest.webmanifest` + `sw.js` + `icons/` 整个放到任意可访问的网址
   （免费方案：GitHub Pages / Netlify / Vercel；或电脑开 `python3 -m http.server` 让手机连同一 WiFi 访问电脑 IP）。
2. 手机浏览器打开该网址 → 安卓 Chrome 点「⋮ → 添加到主屏幕」；iPhone Safari 点「分享 → 添加到主屏幕」。
3. 桌面出现图标，点开即是全屏 App 形态（已配图标+manifest+离线缓存）。
   ⚠️ 此方式**没有通知监听**，只能手动记账和导入账单；数据存在该浏览器本地。

## 工作原理

```
手机弹出支付通知
   └─ PaymentListenerService 捕获（标题+文本+来源App）
        └─ NotificationParser 解析出 金额/商户/方式/品类
             ├─ LedgerStore 写入本机 ledger.json（App 关着也记）
             └─ 广播给 MainActivity
                  └─ WebView 内 H5 页面实时新增一条「🤖自动」记录
```

> 说明：通知里通常只有 **金额 + 商户 + 时间**，没有具体买了什么商品。
> 所以"买了哪些东西"在通知方案下到**商户**级别；要做到"商品级"需再叠加账单导入/小票OCR。

## 目录结构

```
android/
├─ .github/workflows/build-apk.yml       # GitHub Actions：推送后自动编译 APK
├─ gradle/wrapper/                       # Gradle Wrapper（CI 自动编译依赖）
│  ├─ gradle-wrapper.jar
│  └─ gradle-wrapper.properties
├─ gradlew / gradlew.bat                 # 构建脚本（本地也可 ./gradlew assembleDebug）
├─ app/build.gradle.kts
├─ app/src/main/AndroidManifest.xml
├─ app/src/main/assets/index.html        # 复用前面的 H5 记账页
├─ app/src/main/java/com/miniledger/
│  ├─ MainActivity.kt                    # WebView 容器 + JS 桥
│  ├─ PaymentListenerService.kt          # 通知监听服务
│  ├─ NotificationParser.kt              # 支付通知解析规则
│  ├─ LedgerStore.kt                     # 本机 JSON 账本读写
│  └─ MiniLedgerBridge.kt                # H5↔安卓 数据同步桥
├─ build.gradle.kts / settings.gradle.kts / gradle.properties
└─ README.md
```

## 自动构建 APK（GitHub Actions，免 Android Studio）

如果你**不想装 Android Studio**，只要把代码推到 GitHub，云端会自动编译出可安装的 APK，下载装手机即可。本工程已内置：

- `gradle/wrapper/`（Gradle 8.5 包装器，CI 用它自动下载工具链）
- `.github/workflows/build-apk.yml`（推送到 `main`/`master` 或手动触发即构建）

> **直接 fork 本仓库的用户请注意**：从他人仓库 fork 而来的仓库，GitHub Actions
> **默认处于关闭状态**。请先进入仓库 **Actions** 页面，按提示点
> 「I understand my workflows, go ahead and enable them」开启；开启后每次 push 即可自动构建。

### 使用步骤

1. **建仓库**：在 GitHub 新建一个仓库（公开/私有均可），把这个 `android/` 目录作为工程根目录推上去。
   要求根目录直接包含 `gradlew`、`.github/` 等（即 `android/` 内容就是仓库根）。
2. **开启 Actions**：仓库默认已开启「GitHub Actions」。私有仓库同样免费。
3. **触发构建**：
   - 方式一：把代码 `git push` 到 `main` 或 `master` 分支，自动开始；
   - 方式二：进仓库 **Actions → Build APK → Run workflow** 手动触发。
4. **等构建**：首次约 5–15 分钟（需联网下载 Gradle 8.5、Android SDK 组件、依赖库）。可在 Actions 页看实时日志。
5. **下载 APK**：构建成功后，在对应运行记录的右侧 **Artifacts** 区下载 `miniledger-apk`（里面是 `app-debug.apk`）。Artifacts 默认保留 30 天。
6. **装到手机**：把 `app-debug.apk` 传到手机（微信文件传输/数据线/网盘），点击安装；首次需允许「安装未知来源应用」。
7. **授权与验证**：打开 App → 系统跳转「通知使用权」给本 App 打勾 → 用微信/支付宝/银行卡/美团付一笔 → 回到「明细」应自动出现 `🤖自动` 记录，「统计」自动汇总。

> 说明：产出的是 **debug 签名 APK**，可正常安装使用。若将来要上架应用商店，再做 release 签名即可，不影响功能。

## 构建与运行（在带 Android SDK 的电脑上）

1. **用 Android Studio 打开 `android/` 目录**，等待 Gradle 同步完成。
2. 连上 **Android 真机**（API 24+，模拟器拿不到真实支付通知），或先跑起来看 UI。
3. 点 ▶ Run 安装到手机。
4. 首次打开 App 会跳到系统「通知使用权」设置页，**给本 App 打勾授权**。
5. 用微信/支付宝/银行卡/美团正常付一笔，回到 App 的「明细」即可看到
   自动出现一条带 `🤖自动` 标记的记账；「统计」里按支付/品类/时间自动汇总。

## 支持的支付渠道解析

| 渠道 | 触发来源 | 识别方式 |
|---|---|---|
| 微信支付 | 微信通知 | 包名 tencent.mm / 文本含"微信支付" |
| 支付宝 / 花呗 | 支付宝通知 | 包名 alipay / 文本含"支付宝"，再按"花呗"细分 |
| 银行短信 / 银行卡 | 各银行App及短信 | 文本含"银行/储蓄卡/信用卡/尾号"或已知银行包名 |
| 美团月付 | 美团通知 | 包名 meituan / 文本含"美团月付" |

解析规则在 `NotificationParser.kt`，各家的通知文案若有差异，按上面的正则/关键词微调即可。

## 账单导入（支付宝/微信 CSV）+ 商品级识别

通知监听只能拿到「金额+商户」，而**账单导出里的「商品/商品名称」列才是具体买了什么**。
用「明细」页的 **📥 导入支付宝/微信账单(CSV)** 可一次性把历史账单补进来：

1. 支付宝 / 微信 → 账单 → 导出账单（CSV）。微信导出的文件常是 `.xls`，实为 CSV，可直接选。
2. 点导入，选文件。代码会自动：
   - 定位表头（含"交易时间"的行），按列名映射；
   - 仅保留「支出」行，跳过 收入 / 退款 / 关闭 / 失败；
   - 把"商品/商品名称"存为**商品级 `item` 字段**，明细里以 🛒 前缀展示；
   - 支付方式归一（花呗/美团月付/微信支付/支付宝/银行卡…），品类优先取账单自带分类，否则按关键词推断；
   - 按订单号去重，导入结果弹窗报告「新增 N / 跳过 M」。

> 编码兼容：先用 UTF-8 解码，若出现乱码（微信导出多为 GBK）自动回退 GBK。

## 隐私

- 全部数据仅存于手机本机私有目录（`ledger.json`），**不上传任何服务器**。
- 换手机前请用 H5 内「导出备份(JSON)」导出，再到新机导入。

## 后续可加

- 小票/截图 OCR（进一步补全商品级与无通知场景）
- 云同步（如需多设备）

## 把它变成你自己的「模板仓库」

把这个仓库推到 GitHub 后，如果你想让别人（或未来的你）一键 fork 出独立副本：

1. 仓库 **Settings → 勾选 "Template repository"**（主页即出现绿色的「Use this template」按钮）。
2. **建议仓库描述**：`📱 安卓自动记账 App：监听支付通知静默入账，GitHub Actions 自动出 APK`
3. **建议 Topics**：`android` `kotlin` `expense-tracker` `github-actions` `apk` `notification-listener` `budget`

> 其他人 fork 后只需：开启 Actions → `git push` → 下载 Artifacts 里的 `app-debug.apk` 装手机即可，无需读源码、无需装 Android Studio。
