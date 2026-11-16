# WA Contact Collector

Aplikasi Android native Kotlin + Jetpack Compose yang berjalan sepenuhnya lokal/offline untuk memantau event terbatas antarmuka WhatsApp atas kendali pengguna.

## Privasi dan batasan

- Tidak memakai endpoint, bearer token, API CRM, server, login, internet, atau WorkManager.
- `AccessibilityService` hanya menerima event dari `com.whatsapp` dan `com.whatsapp.w4b`.
- Service hanya membaca node yang sedang terlihat untuk mencari pola nomor, tidak melakukan scrolling otomatis, dan tidak mengambil daftar chat massal.
- Hanya pola nomor yang ditemukan pada event UI yang diproses; isi pesan, password, media, dan teks lain tidak disimpan.
- Kandidat nomor ditampilkan di dashboard. Nomor baru masuk Room hanya setelah pengguna menekan `Simpan nomor`.
- Monitoring bersifat opt-in dan berhenti ketika tombol `Hentikan` ditekan. Untuk menonaktifkan service sepenuhnya, gunakan Pengaturan Android.
- `READ_CONTACTS` hanya dipakai untuk mengecek kecocokan nomor yang sudah disetujui pengguna.

## Fitur

- Tombol Mulai Pantau, Hentikan Pantauan, dan buka WhatsApp/WhatsApp Business.
- Normalisasi `+62`, `62`, `08`, dan `8` menjadi `628xxxxxxxxxx`.
- Deduplicasi otomatis menggunakan nomor ternormalisasi sebagai primary key Room.
- Dashboard total kontak, kontak baru, kandidat nomor, status service, dan riwayat pencatatan.
- Informasi scanning lokal: package yang aktif, jumlah event scroll manual yang diproses, dan waktu pemantauan terakhir.
- Ekspor CSV dan hapus data lokal.

## Build tanpa Android Studio

Prasyarat: JDK 17, Android SDK API 35, dan Gradle 8.10.

```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```

APK debug berada di `app/build/outputs/apk/debug/app-debug.apk`. Workflow `.github/workflows/android.yml` tetap membangun unit test dan APK debug menggunakan GitHub Actions.

## Struktur teknis

- `MainActivity.kt`: dashboard Compose, kontrol monitoring, izin kontak, intent WhatsApp, CSV.
- `data/WhatsAppAccessibilityService.kt`: observer `TYPE_VIEW_SCROLLED` WhatsApp pada node yang sedang terlihat.
- `data/CaptureSessionStore.kt`: status opt-in dan kandidat sementara di memori.
- `data/ContactRepository.kt`: normalisasi, deduplikasi, pengecekan kontak, dan Room.
- `data/AppDatabase.kt`: database lokal pelanggan dan riwayat pencatatan.
- `app/src/main/res/xml/whatsapp_accessibility_service.xml`: batas package dan event service.
