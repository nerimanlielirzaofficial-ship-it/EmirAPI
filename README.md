# Emir AI Studio

Android için Kotlin + Jetpack Compose uygulaması. GitHub Actions ile APK derler.

## Kurulum
1. ZIP'i açıp tüm içeriği yeni GitHub deposuna yükle.
2. Actions > Build Emir AI Studio APK > Run workflow.
3. İşlem bitince Artifacts bölümünden `Emir-AI-Studio-debug` dosyasını indir.
4. ZIP'i aç, `app-debug.apk` dosyasını Android'e kur.

## Kullanım
Uygulamayı aç, API AYARLARI > Düzenle bölümüne Groq ve Gemini API anahtarlarını girip kaydet.
- Groq anahtarı: https://console.groq.com/keys
- Gemini anahtarı: https://aistudio.google.com/app/apikey

Anahtarlarını GitHub'a veya başkasına gönderme. Uygulama anahtarları Android'in şifreli SharedPreferences alanında saklar.

## Video üretimi
Groq `llama-3.3-70b-versatile` ile Türkçe 2D çizgi film promptu hazırlar. Gemini `gemini-omni-1.1-flash` Interactions API ile 9:16, 720p MP4 üretir ve Galeri/Movies/EmirAIStudio içine kaydeder.
Gemini video üretim erişimi, bölge, kota ve ücretlendirme hesabına bağlıdır. Gemini Omni kısa klipler (yaklaşık 3–10 saniye) üretir; 60 saniyelik bölüm için birden fazla sahne ve montaj gerekir.
