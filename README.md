# Chatin — Android Messenger, Web Client & Realtime Backend

Chatin adalah aplikasi pesan Android dengan fondasi web client dan backend realtime.

**Status: BETA DEVELOPMENT — BELUM SIAP PRODUKSI.** Autentikasi OTP Firebase dan sebagian alur pesan telah diperkuat, tetapi pengujian perangkat nyata, sinkronisasi offline, aturan keamanan Firestore, panggilan WebRTC, notifikasi push, dan enkripsi end-to-end penuh belum selesai. Jangan gunakan untuk percakapan rahasia atau data produksi.

## Pembaruan terbaru

| Informasi | Nilai |
| --- | --- |
| Versi Android | 1.0 (versionCode 1, sesuai konfigurasi Gradle saat ini) |
| Package ID | com.aistudio.chatin.kxqrvz |
| Minimum Android | Android 7.0 / API 24 |
| Target SDK | API 36 |
| Waktu pembaruan README | **15 November 2026, 09:44 WIB (UTC+7)** |
| Branch pengembangan | feat/chatin-core-reliability |
| Commit kode terakhir diperiksa | 3a2d775c0e9ee88794452f5e312ae17efd42c343 |
| Pull request | [PR #3 — Firebase phone verification & message reliability](https://github.com/adebojel/Chatin/pull/3) |

### Perubahan kode terbaru di branch pengembangan

- Login nomor telepon memakai Firebase Phone Authentication dan verifikasi OTP sungguhan; enam digit sembarang tidak lagi dianggap valid.
- Nomor telepon divalidasi dalam format internasional E.164 (contoh Indonesia: +6281234567890).
- Sesi Firebase menjadi sumber autentikasi; status login lokal saja tidak cukup.
- Logout mengakhiri sesi Firebase.
- Profil disimpan setelah autentikasi berhasil.
- Pesan memakai Firebase UID sebagai identitas pengirim.
- Pesan baru berstatus PENDING sampai Firestore mengonfirmasi penulisan.
- Ditambahkan unit test validator nomor telepon dan langkah unit test pada workflow Android.

**Catatan:** PR #3 masih draft dan belum digabung ke main. Perubahan tersebut ada pada branch PR, bukan berarti sudah tersedia pada versi rilis publik.

## Unduh dan instal aplikasi Android

### Build uji coba (APK Debug)

**[Buka hasil build Chatin di GitHub Actions](https://github.com/adebojel/Chatin/actions/runs/43669045895)**

Cara mengambil APK setelah workflow selesai:

1. Buka tautan GitHub Actions di atas.
2. Pastikan status workflow completed dan hasilnya success.
3. Gulir ke bagian Artifacts pada halaman run.
4. Unduh artifact bernama chatin-debug-apk.
5. Ekstrak ZIP, lalu cari app-debug.apk.
6. Pindahkan APK ke ponsel Android dan buka file tersebut.
7. Jika Android meminta izin, izinkan instalasi dari sumber yang digunakan untuk membuka APK. Matikan kembali izin tersebut bila tidak diperlukan.
8. Jalankan Chatin dan ikuti checklist uji coba di bawah.

**Penting:**
- Tautan di atas adalah halaman workflow, bukan tautan APK langsung. APK baru dapat diunduh setelah build sukses dan artifact terunggah.
- Artifact debug CI disimpan selama 7 hari. Jika kedaluwarsa, jalankan ulang workflow dari tab Actions atau minta build baru.
- Build Debug ditujukan untuk pengujian, bukan distribusi publik.
- Belum ada tautan Google Play Store atau rilis stabil.
- Hanya instal APK dari repository resmi Chatin.

### Membuat APK sendiri melalui Android Studio

1. Pasang Android Studio dan Android SDK yang mendukung API 36.
2. Clone repository:
   ```bash
   git clone https://github.com/adebojel/Chatin.git
   cd Chatin
   ```
3. Checkout branch pengembangan bila ingin menguji perubahan terbaru:
   ```bash
   git checkout feat/chatin-core-reliability
   ```
4. Siapkan konfigurasi Firebase.
5. Buka folder proyek di Android Studio dan tunggu Gradle Sync.
6. Jalankan:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:assembleDebug
   ```
7. APK dihasilkan di app/build/outputs/apk/debug/app-debug.apk.

Di Windows gunakan gradlew.bat sebagai pengganti ./gradlew.

## Fitur dan status implementasi

| Area | Status saat ini | Catatan |
| --- | --- | --- |
| Android native | Fondasi tersedia | Kotlin, Jetpack Compose |
| Nomor telepon + OTP | Integrasi Firebase di PR #3 | Perlu konfigurasi project dan uji perangkat nyata |
| Validasi E.164 | Ditambahkan | Unit test tersedia |
| Sesi dan logout Firebase | Diperkuat di PR #3 | Perlu uji siklus hidup aplikasi |
| Profil pengguna | Integrasi Firestore tersedia | Skema ID pengguna masih perlu migrasi |
| Chat dan pesan Firestore | Implementasi dasar tersedia | Rules dan keanggotaan chat belum siap produksi |
| Status pending pesan | Diperbaiki di PR #3 | Tidak mengklaim terkirim sebelum acknowledgement |
| Cache lokal/offline | Fondasi lokal tersedia | Worker upload dan rekonsiliasi outbox belum selesai |
| Web client | Ada di folder public/ | Perlu pengujian integrasi |
| Backend realtime | Express + Socket.IO | Perlu konfigurasi deployment dan pengujian |
| PostgreSQL | Skema tersedia | Perlu konfigurasi dan migrasi server |
| Upload media | Belum siap | Provider dan akses privat perlu diselesaikan |
| Push notification FCM | Belum dikonfirmasi selesai | Jangan diasumsikan aktif |
| Voice/video call WebRTC | Belum selesai | UI/fondasi bukan panggilan produksi |
| Enkripsi end-to-end (E2EE) | Belum tersedia menyeluruh | Pesan saat ini dapat disimpan sebagai teks biasa |
| Keamanan Firestore produksi | Belum selesai | Perlu UID membership, rules ketat, dan emulator tests |

## Arsitektur dan teknologi

- Android: Kotlin, Jetpack Compose, Coroutines/Flow, Room, Retrofit/OkHttp.
- Autentikasi dan database realtime: Firebase Authentication dan Cloud Firestore.
- Backend: Node.js, Express, Socket.IO, PostgreSQL, JWT, bcrypt.
- Web client: HTML, CSS, dan JavaScript pada folder public/.
- Media: rancangan integrasi penyimpanan kompatibel S3, misalnya Cloudflare R2.

### Struktur repository

| Path | Kegunaan |
| --- | --- |
| app/ | Aplikasi Android |
| app/src/main/java/ | Source Kotlin |
| app/src/test/ | Unit tests |
| public/ | Web client |
| server.js | Server Express dan Socket.IO |
| schema.sql | Skema PostgreSQL |
| gradle/libs.versions.toml | Katalog versi dependency Gradle |
| .env.example | Template variabel lingkungan backend |
| docs/FIREBASE_SETUP.md | Panduan Firebase dan catatan keamanan |
| .github/workflows/ | Workflow CI/build APK |

## Persyaratan pengembangan

- Android Studio yang mendukung Android Gradle Plugin dan SDK proyek.
- JDK 17 untuk workflow CI Android.
- Android SDK platform/build tools API 36.
- Node.js 18 atau versi yang didukung platform deployment.
- npm dan PostgreSQL untuk backend.
- Akun Firebase dengan izin mengatur Authentication dan Firestore untuk pengujian autentikasi/chat.

## Konfigurasi Firebase

Package Android yang harus didaftarkan:

```text
com.aistudio.chatin.kxqrvz
```

Langkah konfigurasi:

1. Buka [Firebase Console](https://console.firebase.google.com/).
2. Pilih project Firebase yang memang Anda miliki/kelola. Konfigurasi repository saat ini menunjuk ke project ID chatin-43eed; pastikan project ini benar sebelum digunakan.
3. Daftarkan aplikasi Android dengan package ID di atas.
4. Unduh google-services.json untuk aplikasi yang tepat dan letakkan di app/google-services.json.
5. Aktifkan Authentication → Sign-in method → Phone.
6. Periksa kebijakan negara SMS, kuota SMS, dan nomor uji Firebase bila digunakan.
7. Daftarkan SHA-1 dan SHA-256 untuk sertifikat debug/release sesuai kebutuhan Firebase.
8. Siapkan Cloud Firestore dan gunakan Security Rules yang ketat.
9. Uji autentikasi di perangkat fisik.

Panduan lebih rinci: [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md).

### Android build properties

Android Secrets Gradle Plugin menggunakan file local.properties (file lokal yang diabaikan Git) untuk nilai rahasia dan local.defaults.properties sebagai nilai aman saat CI atau saat file lokal belum tersedia. File local.defaults.properties sudah disediakan di repository dengan string kosong untuk integrasi opsional. Jangan menaruh key produksi di local.defaults.properties atau commit local.properties.

**Jangan pernah mengirim OTP, password, private key, file service-account JSON, atau kredensial rahasia melalui chat/issue.** Jangan mengaktifkan aturan Firestore publik seperti allow read, write: if true.

### Skema Firestore yang digunakan saat ini

- users/{phoneNumber} — profil/presence lama; perlu migrasi ke UID.
- chats/{chatId} — metadata percakapan.
- chats/{chatId}/messages/{messageId} — dokumen pesan.

Skema tersebut belum memenuhi seluruh kebutuhan keamanan produksi. Sebelum rilis, identitas pengguna harus konsisten menggunakan Firebase Auth UID, chat harus memiliki daftar memberUids yang tervalidasi, dan setiap operasi baca/tulis harus memeriksa keanggotaan.

## Menjalankan backend

Dari root repository:

```bash
cp .env.example .env
npm install
```

Atur variabel berikut pada environment deployment. Jangan commit file .env.

| Variabel | Fungsi |
| --- | --- |
| PORT | Port HTTP (default 3000) |
| JWT_SECRET | Secret acak kuat, minimal 32 karakter |
| URL_POSTGRESQL_CLOUD_ANDA | Connection string PostgreSQL |
| ALLOWED_ORIGINS | Daftar origin web tepercaya, dipisahkan koma |
| GEMINI_API_KEY | Opsional, proxy Gemini bila fitur dikonfigurasi |
| S3_ENDPOINT | Endpoint storage S3-compatible |
| S3_ACCESS_KEY_ID | Access key storage |
| S3_SECRET_ACCESS_KEY | Secret key storage |
| S3_BUCKET_NAME | Nama bucket media |

Jalankan skema dan server:

```bash
psql "$URL_POSTGRESQL_CLOUD_ANDA" -f schema.sql
npm start
```

Server memerlukan JWT_SECRET yang kuat. Gunakan HTTPS/TLS sebelum backend dibuka ke internet. Endpoint upload media dan OTP perubahan nomor belum boleh dianggap aktif sebelum provider sesungguhnya dikonfigurasi.

## Menjalankan pemeriksaan dan test

### Android

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Workflow CI debug menjalankan unit test, build APK, pemeriksaan sertifikat APK, lalu upload artifact.

### Backend

```bash
npm install
node --check server.js
npm start
```

Pemeriksaan sintaks atau build sukses tidak menggantikan integration test, Firebase Emulator Rules test, pengujian perangkat nyata, dan audit keamanan.

## Checklist uji coba untuk pengguna

Lakukan pengujian hanya setelah artifact APK berhasil dibuat dan diunduh.

1. Instalasi: APK terpasang, aplikasi terbuka, tidak crash saat mulai.
2. OTP benar: masukkan nomor dengan format internasional, misalnya +62...; pastikan SMS OTP diterima dan kode benar berhasil diverifikasi.
3. OTP salah: kode salah atau kedaluwarsa harus ditolak.
4. Sesi: tutup dan buka ulang aplikasi; periksa apakah sesi Firebase tetap sesuai kondisi autentikasi.
5. Logout: logout, lalu pastikan layar/login state kembali dan sesi Firebase berakhir.
6. Profil: periksa apakah nama/foto/profil tetap ada setelah aplikasi dibuka ulang.
7. Pesan online: kirim pesan dan periksa status setelah Firestore mengonfirmasi.
8. Jaringan terputus: catat apakah pesan gagal atau tetap PENDING. **Sinkronisasi otomatis pesan offline belum selesai**, jadi jangan menganggap pesan pending akan otomatis terkirim saat internet kembali.
9. Stabilitas: coba pindah aplikasi, tutup paksa, dan buka ulang.
10. Laporan: kirim model perangkat, versi Android, waktu kejadian, langkah reproduksi, screenshot/video, dan pesan error. Tutupi nomor telepon serta data pribadi.

Jika OTP gagal, periksa project Firebase, package ID, SHA-1/SHA-256, Phone Authentication, kuota SMS, dan kebijakan negara. Jangan kirim OTP atau kredensial kepada siapa pun.

## Roadmap sebelum rilis publik

- [ ] Selesaikan dan review autentikasi Firebase; uji OTP pada perangkat nyata.
- [ ] Migrasi dokumen pengguna ke Firebase Auth UID.
- [ ] Terapkan memberUids dan Firestore Security Rules least-privilege.
- [ ] Tambahkan Firebase Emulator Rules tests dan integration tests.
- [ ] Selesaikan offline outbox, retry, idempotency, dan sinkronisasi background.
- [ ] Selesaikan lifecycle token dan push notification FCM.
- [ ] Implementasikan panggilan WebRTC sesungguhnya dengan signaling, ICE/STUN/TURN.
- [ ] Rancang dan audit protokol E2EE; jangan membuat klaim E2EE sebelum selesai.
- [ ] Selesaikan upload media privat, kontrol akses, dan kebijakan retensi.
- [ ] Tambahkan pelaporan/blokir, privasi, penghapusan akun, dan kebijakan data.
- [ ] Jalankan pengujian lintas perangkat, security review, dan uji beban backend.
- [ ] Buat signed release, catatan rilis, dan distribusi resmi setelah seluruh gate terpenuhi.

## Keamanan dan privasi

- Jangan commit .env, JWT secret, SMS provider secret, kredensial S3, private signing key, keystore, password keystore, atau service-account private key.
- Batasi Firebase API key ke aplikasi dan API yang diperlukan. Firebase client API key bukan pengganti Security Rules.
- Jangan gunakan database Firestore dengan akses publik.
- Pesan yang ditulis sebagai teks biasa **bukan E2EE**. Hindari data sensitif hingga arsitektur dan aturan keamanan selesai diaudit.
- Sediakan HTTPS/TLS, pembatasan akses, validasi input, rate limiting, dan logging yang tidak membocorkan data pribadi.
- Laporkan kerentanan secara privat kepada pemilik repository.

## Kontribusi dan pelaporan masalah

Gunakan [Issues](https://github.com/adebojel/Chatin/issues) untuk bug atau usulan fitur. Sertakan langkah reproduksi, hasil yang diharapkan, hasil aktual, perangkat/versi Android, dan log yang sudah disensor. Jangan menyertakan OTP, password, token, nomor pribadi, atau data percakapan nyata.

## Lisensi

Periksa file LICENSE di repository. Jika belum tersedia, hak penggunaan ulang dan distribusi belum dinyatakan secara eksplisit.

---

**Repository:** [github.com/adebojel/Chatin](https://github.com/adebojel/Chatin)  
**Build Android:** [GitHub Actions — Android Debug APK](https://github.com/adebojel/Chatin/actions/workflows/android-debug-apk.yml)  
**Pull request pengembangan:** [PR #3](https://github.com/adebojel/Chatin/pull/3)  
**Terakhir diperbarui:** 15 November 2026, 09:44 WIB (UTC+7); dokumentasi setup build diperbarui kembali setelah perbaikan konfigurasi CI.
