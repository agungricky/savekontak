# WA Contact Collector

Aplikasi Android native Kotlin + Jetpack Compose untuk mengumpulkan nomor pelanggan dari sumber yang diizinkan, menormalisasi nomor Indonesia, menghapus duplikat, dan menyimpan hasilnya secara lokal.

## Batasan privasi dan kepatuhan

- Aplikasi hanya mengambil data dari endpoint HTTPS resmi yang dikonfigurasi pengguna, misalnya CRM atau WhatsApp Business Platform API.
- Aplikasi tidak melakukan scraping daftar chat, membaca database WhatsApp, atau mengumpulkan nomor secara diam-diam.
- Izin `READ_CONTACTS` hanya digunakan untuk mengecek apakah nomor yang sudah diterima dari sumber resmi ada di kontak perangkat.
- Token API disimpan di Android DataStore privat aplikasi. Gunakan HTTPS, minimalkan scope token, dan pastikan dasar hukum/persetujuan pelanggan sesuai kebijakan organisasi.

## Fitur

- Membuka WhatsApp biasa atau WhatsApp Business memakai Android Intent.
- Normalisasi `+62`, `62`, `08`, dan `8` menjadi format `628xxxxxxxxxx`.
- Deduplicasi otomatis memakai nomor ternormalisasi sebagai primary key Room.
- Cek kontak perangkat dengan `READ_CONTACTS` secara opsional.
- Sinkronisasi manual dan berkala memakai WorkManager dengan network constraint.
- Dashboard total nomor, nomor baru pada sinkronisasi terakhir, nomor terbaru, dan riwayat sinkronisasi.
- Ekspor CSV dan penghapusan data lokal.

## Format endpoint resmi

Atur endpoint dan Bearer token pada menu Pengaturan. Respons JSON yang didukung:

```json
[
  { "phone": "081234567890", "name": "Pelanggan A" }
]
```

atau:

```json
{
  "customers": [
    { "phone": "+62 812-3456-7890", "name": "Pelanggan A" }
  ]
}
```

Provider harus mengizinkan aplikasi memanggil endpoint tersebut dan menyediakan data sesuai kebijakan/izin yang berlaku.

## Build tanpa Android Studio

Prasyarat: JDK 17, Android SDK dengan platform/build-tools API 35, dan Gradle 8.10. Dari terminal VS Code:

```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```

APK debug berada di `app/build/outputs/apk/debug/app-debug.apk`. Workflow GitHub Actions di `.github/workflows/android.yml` memasang JDK, Android SDK, dan Gradle lalu mengunggah APK sebagai artifact.

Untuk perangkat/emulator yang terhubung:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Struktur teknis

- `MainActivity.kt`: UI Compose, dashboard, pengaturan, ekspor, dan intent WhatsApp.
- `data/AppDatabase.kt`: Room database untuk pelanggan dan riwayat sinkronisasi.
- `data/OfficialApiClient.kt`: klien JSON endpoint resmi; bukan scraper WhatsApp.
- `data/ContactRepository.kt`: normalisasi, deduplikasi, cek kontak, dan penjadwalan.
- `data/SyncWorker.kt`: sinkronisasi berkala di background sesuai batasan Android.
