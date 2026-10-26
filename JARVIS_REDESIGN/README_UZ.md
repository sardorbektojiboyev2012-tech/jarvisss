# JARVIS 2.1 — Premium Android Assistant

Android loyiha Kotlin + Jetpack Compose bilan yozilgan. Interfeys grafit-qora fon, binafsha aksentlar va animatsiyali markaziy orb asosida tuzilgan.

## Hozir mavjud funksiyalar

- Ovozli buyruqni Android Speech Recognizer orqali olish.
- Javoblarni Android Text-to-Speech orqali o‘qish (telefonning ovoz/til imkoniyatiga bog‘liq).
- Offline buyruqlar: salomlashish, vaqt/sana, eslatmani saqlash, yordam.
- Telefonning Wi-Fi, Bluetooth, umumiy sozlamalari, budilnik va terish oynasini ochish.
- Mahalliy xotira va suhbat tarixini saqlash.
- Google bo‘limi: web qidiruv, rasm qidiruvi, Google Translate, Google Maps, YouTube va Google Gemini’ni ochish.
- Rasm tanlash va Android ulashish oynasini ochish; mavjud ilovalar ro‘yxatidan Lens mos keladigan qurilmada tanlanishi mumkin.
- O‘zbekcha, inglizcha va ruscha ovozli tanish sozlamasi.
- GitHub Actions orqali debug APK qurish va artifact sifatida yuklash.

## AI haqida muhim ma’lumot

Google Gemini tugmasi Gemini xizmatini tashqi ilova/brauzerda ochadi. Bu versiyada Gemini modeli ilova ichiga API orqali integratsiya qilinmagan. Haqiqiy ichki AI chat uchun xavfsiz server/backend va server tomonda saqlanadigan API kaliti kerak. Ilovada API kaliti hardcode qilinmagan.

## Build qilish

1. Loyihaning ichidagi fayllarni GitHub repository root’iga joylashtiring (`settings.gradle.kts` root’ta turishi kerak).
2. GitHub → **Actions** → **Build JARVIS APK** → **Run workflow** ni bosing.
3. Build muvaffaqiyatli tugasa, run sahifasidagi **Artifacts** bo‘limidan `JARVIS-2.1-debug-APK` ni yuklab oling.
4. ZIP artifact’ni oching va APK’ni Android telefoningizga ko‘chiring.

## Cheklovlar

- Bu chat muhitida Android SDK/Gradle build bajarilmagan; APK kompilyatsiyasi tasdiqlanmagan. GitHub Actions natijasini tekshirish kerak.
- Google xizmatlari internet, hisob va qurilmadagi mavjud ilovalarga bog‘liq.
- Doimiy “Hey JARVIS” wake-word, keng qamrovli telefon boshqaruvi va ichki AI backend bu versiyada to‘liq amalga oshirilmagan.
