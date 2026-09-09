# VLink — اسکلت اولیه اپ

این یک پروژه‌ی Android Studio (Kotlin + Jetpack Compose) با ساختار مشابه v2rayNG است.
همین الان قابل باز شدن در Android Studio است و کامپایل می‌شود، اما **تانل واقعی VPN
هنوز فعال نیست** — چون آن به هسته‌ی Xray/V2Ray نیاز دارد که باید اضافه کنید (پایین توضیح داده شده).

## چیزهایی که همین الان کار می‌کنند

| بخش | وضعیت |
|---|---|
| ساختار پروژه (Gradle, Manifest, Compose) | ✅ آماده |
| مدل داده (`ConfigProfile`, `Subscription`) | ✅ آماده |
| دیتابیس محلی (Room) برای ذخیره‌ی چند ساب + کانفیگ | ✅ آماده |
| پارس لینک‌های vmess/vless/trojan/ss (فیلدهای کامل) | ✅ آماده (`FullConfigParser`) |
| ساخت JSON واقعی کانفیگ Xray/V2Ray از روی لینک | ✅ آماده (`ConfigBuilder`) — tcp/ws + tls |
| دانلود و پارس کردن یک URL ساب | ✅ آماده |
| افزودن/حذف/رفرش چند ساب همزمان | ✅ آماده (`SubscriptionScreen`) |
| افزودن کانفیگ با پیست لینک | ✅ آماده (`AddConfigScreen`) |
| افزودن کانفیگ با اسکن QR | ✅ آماده (zxing + `ScanContract`) |
| افزودن کانفیگ با پیست از کلیپ‌بورد | ✅ آماده |
| UI اصلی: لیست کانفیگ‌ها + دکمه پینگ هر کدوم | ✅ آماده (`HomeScreen`) |
| فلوی درخواست مجوز VPN از سیستم (`VpnService.prepare`) | ✅ آماده |
| تست پینگ TCP (fallback) | ✅ آماده |
| تست پینگ واقعی از طریق تانل (مثل v2rayNG) | ✅ کد آماده، منتظر هسته‌ی واقعی (زیر توضیح داده شده) |
| اتصال واقعی VPN (تانل ترافیک) | ⛔ منتظر هسته‌ی واقعی |

## قدم بعدی مهم: وصل کردن هسته‌ی V2Ray/Xray

همه‌ی لایه‌های اطراف هسته آماده و از یک اینترفیس واحد به اسم
`TunnelCore` (در `core/vpn/TunnelCore.kt`) استفاده می‌کنن — یعنی وقتی
هسته‌ی واقعی رو اضافه کردی، فقط کافیه یک کلاس جدید پیاده‌سازی کنی
(`XrayNativeCore : TunnelCore`) و در `TunnelCore.create()` جایگزینش کنی؛
لازم نیست به UI، دیتابیس، یا پارسرها دست بزنی.

جزئیات کامل (شکل API معمول AndroidLibXrayLite، نکته‌ی مهم `protect(fd)`
برای جلوگیری از حلقه شدن ترافیک، و نحوه‌ی وصل کردن fd تانل) توی کامنت
بالای `TunnelCore.kt` نوشته شده.

اپ‌هایی مثل v2rayNG از `libv2ray` یا `Xray-core` که با `gomobile` به AAR
کامپایل شده استفاده می‌کنند. دو راه داری:

1. **ساده‌تر:** از یه AAR از پیش کامپایل‌شده استفاده کن (مثلاً از پروژه‌ی
   `AndroidLibXrayLite` که خودش رو به صورت prebuilt AAR منتشر می‌کنه).
   فایل `.aar` رو بذار توی `app/libs/` و توی `app/build.gradle.kts` این خط
   رو از کامنت در بیار:
   ```kotlin
   implementation(files("libs/libv2ray.aar"))
   ```

2. **کامل‌تر:** خودت `Xray-core` رو با Go + gomobile برای اندروید کامپایل
   کن (نیاز به نصب Go و gomobile داره، خارج از Android Studio).

بعد از اضافه کردن AAR:
- توی `V2RayVpnService.kt` جایی که `TODO` نوشته شده، fd تانل رو به هسته پاس بده.
- توی `PingTester.kt`، نسخه‌ی "واقعی" پینگ (از طریق تانل) رو طبق TODO پیاده کن.

## اجرا

1. پروژه رو توی Android Studio (Hedgehog یا جدیدتر) باز کن.
2. بذار Gradle sync بشه (نیاز به اینترنت برای دانلود dependency‌ها داره).
3. Run روی یک دستگاه/امولاتور با API 24+.

## ساختار پوشه‌ها

```
app/src/main/java/com/vlink/app/
├── MainActivity.kt              # navigation + ViewModel wiring
├── ui/
│   ├── theme/Theme.kt           # رنگ و تم اپ
│   └── screens/
│       ├── HomeScreen.kt        # لیست کانفیگ‌ها + پینگ + اتصال
│       └── SubscriptionScreen.kt# مدیریت چند ساب
├── data/
│   ├── model/                   # ConfigProfile, Subscription
│   ├── db/                      # Room entities + DAO + Database
│   └── repository/              # منطق ترکیب پارسر + دیتابیس + پینگ
└── core/
    ├── parser/
    │   ├── ConfigParser.kt      # پارس سبک (برای لیست: نام/آدرس/پورت) + ساب
    │   └── FullConfigParser.kt  # پارس کامل هر پروتکل (uuid, tls, ws path, ...)
    ├── ping/PingTester.kt       # پینگ واقعی (از طریق core) + fallback به TCP
    └── vpn/
        ├── ConfigBuilder.kt     # ساخت JSON واقعی core از روی FullConfigParser
        ├── TunnelCore.kt        # اینترفیس انتزاعی + PLUG-IN POINT هسته‌ی واقعی
        └── V2RayVpnService.kt   # VpnService واقعی؛ TUN رو می‌سازه و به core می‌ده
```
