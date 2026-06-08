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

## 📥 Cara Install

1. Unduh berkas **`HitungUang.apk`** versi terbaru dari halaman **Releases** di repositori GitHub ini.
2. Buka berkas APK yang telah diunduh di perangkat Android Anda.
3. Jika muncul peringatan keamanan (keamanan Play Protect atau instalasi dari sumber tidak dikenal), berikan izin untuk melanjutkan instalasi.
4. Ketuk **Install** (Pasang) dan tunggu hingga proses selesai.
5. Buka aplikasi **HitungUang 🪙** dari laci aplikasi Anda.

---

## 📖 Panduan Penggunaan & Tutorial

### 1. Setup Awal (Onboarding)
Saat aplikasi dibuka pertama kali, Anda akan dipandu melalui 8 langkah setup awal:
* **Profil**: Masukkan nama panggilan dan pekerjaan Anda.
* **Dompet**: Buat akun dompet utama Anda beserta saldo awalnya.
* **Anggaran**: Tetapkan batas pengeluaran bulanan global pertama Anda.
* **Keamanan**: Aktifkan kunci PIN (4 digit) serta Biometrik (Sidik Jari/Wajah) untuk mengamankan data keuangan Anda.
* **Cadangan**: Pilih folder penyimpanan aman di perangkat Anda untuk auto-backup data.

### 2. Mencatat Transaksi Manual
* Ketuk chip kategori pada bagian **Quick Add** di Dashboard, atau buka tab **Transaksi** lalu ketuk tombol **(+)**.
* Pilih tipe transaksi (**Pemasukan** atau **Pengeluaran**).
* Masukkan nominal, pilih akun dompet, pilih kategori, dan tambahkan catatan/tanggal jika diperlukan.
* Anda juga dapat melampirkan hingga 5 foto bukti transaksi langsung dari Kamera atau Galeri.
* Ketuk **Simpan**.

### 3. Pindai Struk Belanja Otomatis (OCR)
* Masuk ke tab **Scan** pada navigasi bawah.
* Ambil foto struk belanja atau unggah foto dari Galeri.
* Engine OCR lokal (Google ML Kit) akan memindai struk dan mem-parsing nama toko, tanggal, daftar barang belanjaan, pajak, hingga total pengeluaran.
* Tinjau hasil pemindaian di layar review, sesuaikan data jika diperlukan, lalu ketuk **Simpan Transaksi**.

### 4. Transfer Dana Antar Rekening
* Masuk ke tab **Akun** (Kelola Dompet).
* Ketuk tombol **Transfer** (ikon panah bolak-balik) di pojok kanan atas.
* Pilih rekening asal, rekening tujuan, nominal transfer, serta biaya admin jika ada.
* Ketuk **Kirim**. Saldo kedua rekening akan disesuaikan secara real-time dan aman.

### 5. Cadangkan dan Pulihkan Data (Backup & Restore)
* Buka menu pengaturan (ketuk ikon gerigi di pojok kanan atas Dashboard atau Akun).
* Pilih opsi **Backup & Restore**.
* Ketuk **Ekspor Data** untuk mengompresi basis data Room beserta seluruh gambar lampiran ke dalam satu berkas file ZIP di folder yang telah Anda pilih.
* Anda juga dapat mengimpor berkas ZIP cadangan tersebut di kemudian hari untuk memulihkan data secara utuh.

---

## 📝 Lisensi
Proyek ini dilisensikan di bawah ketentuan lisensi internal pribadi. Semua hak cipta dilindungi undang-undang.
