# HitungUang 🪙

**HitungUang** adalah aplikasi asisten pencatatan keuangan pribadi modern berbasis Android yang dirancang dengan pendekatan **Offline-First**, mengutamakan privasi penuh, kecepatan performa, serta keandalan integritas data.

Aplikasi ini dibangun menggunakan arsitektur **Feature-First + Clean Architecture** dan didukung oleh teknologi Android modern terkini (Jetpack Compose, Room, Dagger Hilt, WorkManager, dan ML Kit).

---

## ✨ Fitur Utama

Aplikasi HitungUang dikembangkan dalam 26 Milestone dengan cakupan fitur lengkap:

### 1. Keuangan & Akun (Core Ledger)
* **Multi-Account/Wallet Support**: Kelola banyak dompet atau rekening bank secara fleksibel.
* **Transaksi Pemasukan & Pengeluaran**: Pencatatan cepat dengan kategori, catatan khusus, dan pemilih tanggal Material 3.
* **Transfer Antar Akun**: Dukungan biaya admin dan validasi saldo secara transaksional.
* **Balance Engine**: Sentralisasi kalkulasi saldo serta modul validasi & perbaikan otomatis guna menjamin konsistensi saldo secara ketat.
* **Sistem Draf Otomatis (Auto-Save)**: Menyimpan draf transaksi yang sedang diisi secara real-time agar tidak hilang saat dialog tertutup tidak sengaja.

### 2. Fitur Pintar & OCR (Smart Features)
* **Pindai Struk (OCR Scanner)**: Ekstraksi teks otomatis dari struk belanja menggunakan Google ML Kit secara lokal (offline) untuk mendeteksi toko, tanggal, item barang, pajak, dan total pengeluaran.
* **Review Struk & Arsip**: Antarmuka review hasil scan sebelum disimpan ke arsip beserta pembersihan otomatis (auto-cleanup) struk lama.
* **Lampiran Transaksi (Attachment)**: Unggah hingga 5 foto (kamera/galeri) per transaksi dengan kompresi gambar JPEG otomatis untuk menghemat ruang penyimpanan.

### 3. Anggaran & Analisis (Budgeting & Dashboard)
* **Dashboard Finansial**: Ringkasan kondisi keuangan terpadu, filter periode fleksibel, shortcut transaksi cepat (Quick Add), dan tips insight pintar.
* **Manajemen Anggaran (Budgeting)**: Batasan budget Global atau per Kategori dengan kalkulasi threshold dinamis (warna indikator berubah saat pengeluaran mendekati limit).
* **Rollover Otomatis**: Anggaran yang kedaluwarsa akan secara otomatis di-reset atau dialihkan (rollover) ke periode berikutnya.
* **Grafik Analisis**: Visualisasi donat chart distribusi pengeluaran yang dibangun responsif menggunakan Canvas Compose.

### 4. Keamanan & Utilitas
* **Sistem Kunci PIN & Biometrik**: Proteksi penuh saat membuka aplikasi dengan dukungan sidik jari/wajah serta kode pemulihan (recovery code).
* **Pencarian Cepat**: Mesin pencari global berbasis **Room FTS4** (Full-Text Search) dengan query sanitization untuk pencarian parsial di bawah 100ms.
* **Tempat Sampah (Recycle Bin)**: Sistem soft-delete transaksi yang terintegrasi dengan pemulihan (restore) saldo dan auto-cleanup background worker.
* **Backup & Restore ZIP**: Ekspor dan impor seluruh basis data beserta berkas lampiran ke file ZIP terenkripsi lokal melalui System Access Framework (SAF).
* **Notifikasi Lokal**: Pengingat mencatat harian kustom, peringatan anggaran kritis, serta laporan ringkasan mingguan/bulanan offline via WorkManager.

---

## 🛠️ Stack Teknologi

Aplikasi ini menggunakan teknologi Android modern untuk memastikan modularitas, skalabilitas, dan stabilitas:

* **Bahasa**: Kotlin 2.x (dengan compiler Kotlin terbaru)
* **UI Framework**: Jetpack Compose (Material 3)
* **Dependency Injection**: Dagger Hilt
* **Database**: Room Persistence Library 2.8.4 (KSP2 Compiler)
* **Local Preferences**: AndroidX DataStore Preferences
* **Background Jobs**: WorkManager
* **Image Loader**: Coil 3 (Asynchronous Image Loading)
* **Charts**: Vico Charts (dan custom Canvas drawing)
* **OCR / Vision**: Google ML Kit Text Recognition
* **Testing**: JUnit4, Robolectric (untuk Compose UI Testing lokal di JVM), dan Mockito/Coroutines Test

---

## 📐 Arsitektur Proyek

HitungUang mengadopsi prinsip **Clean Architecture** dengan pemisahan folder berbasis **Feature-First**:

```text
app/src/main/java/com/hitunguang/
├── core/
│   ├── database/     # Room DB, Entity, DAO, dan Migrasi database
│   ├── datastore/    # DataStore Preferences (App & User Settings)
│   └── theme/        # Skema Warna, Tipografi, dan Material Theme
│
└── feature/          # Modul Fitur (onboarding, dashboard, transaction, dll)
    ├── [feature_name]/
    │   ├── data/       # Implementasi Repository dan Mapper
    │   ├── domain/     # Usecase dan Model Bisnis murni
    │   └── presentation/# Compose UI Screens, Dialogs, dan ViewModels
```

Setiap fitur bersifat independen dan berkomunikasi melalui lapisan UseCase untuk mempermudah pemeliharaan kode dan penulisan test.

---

## 🚀 Cara Menjalankan & Membangun Proyek

### Prasyarat
* **Android Studio Ladybug** (atau versi lebih baru)
* **JDK 17** (direkomendasikan menggunakan JetBrains Runtime bawaan Android Studio)
* Perangkat Android dengan **SDK Min 26 (Android 8.0)**

### Build Skenario

1. **Clone repositori**:
   ```bash
   git clone https://github.com/Medskiyyy/HitungUang.git
   cd HitungUang/app
   ```

2. **Menjalankan Pengujian Unit & UI (Robolectric)**:
   ```powershell
   # Windows PowerShell
   $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew test
   ```

3. **Membangun APK Rilis Teroptimasi**:
   ```powershell
   # Windows PowerShell
   $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew :app:assembleRelease
   ```
   APK rilis yang telah dioptimasi dengan Proguard/R8 dan ditandatangani akan berada di folder:
   `app/app/build/outputs/apk/release/app-release.apk`

---

## 📝 Lisensi
Proyek ini dilisensikan di bawah ketentuan lisensi internal pribadi. Semua hak cipta dilindungi undang-undang.
