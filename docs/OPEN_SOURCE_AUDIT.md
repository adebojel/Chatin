# Audit integrasi open-source Chatin

Tanggal audit: 16 November 2026  
Repository target: https://github.com/adebojel/Chatin  
Branch yang diaudit: `feat/chatin-core-reliability`

Dokumen ini mencatat proyek publik yang diperiksa, lisensi yang terlihat pada metadata GitHub, relevansi teknis, dan keputusan integrasi. Pemeriksaan metadata bukan pengganti audit lisensi/legal penuh terhadap setiap file dan dependency.

## Prinsip integrasi

- Tidak mengimpor source code, aset, model, atau protokol dari proyek lain tanpa memeriksa lisensi, atribusi, NOTICE, dan kewajiban distribusi.
- Tidak mengadopsi library hanya karena populer. Periksa ukuran APK, minSdk, Kotlin/AGP, native ABI, pemeliharaan, CVE, dan kompatibilitas Firebase.
- Tidak menampilkan keberhasilan operasi yang belum menerima acknowledgement dari server/perangkat/provider.
- Tidak menambahkan kredensial, service-account, signing key, atau data pengguna ke Git.
- Proyek AGPL tidak dicampur ke aplikasi ini tanpa keputusan lisensi eksplisit karena kewajiban copyleft jaringan/distribusi dapat berdampak pada keseluruhan produk.

## Repository yang diperiksa

| Repository | Fokus yang relevan | Lisensi pada metadata GitHub | Keputusan |
| --- | --- | --- | --- |
| [Signal-Android](https://github.com/signalapp/Signal-Android) | Arsitektur messenger, database pesan, attachment, protokol Signal/libsignal, keamanan | AGPL-3.0 | Pelajari pola arsitektur dan threat model saja. Jangan salin kode atau integrasikan modul AGPL tanpa keputusan lisensi dan review legal. |
| [Element Android](https://github.com/element-hq/element-android) | Matrix client, room, sync, media, encryption, multi-device | AGPL-3.0 | Referensi arsitektur saja; bukan sumber kode yang langsung digabungkan ke aplikasi dengan lisensi yang belum ditentukan. |
| [Jitsi Meet](https://github.com/jitsi/jitsi-meet) | Video conference, signaling, media routing, UI panggilan | Apache-2.0 | Kandidat untuk proof-of-concept panggilan, tetapi integrasi Android perlu menilai Jitsi SDK/meet SDK, ukuran, signaling deployment, TURN, dan kompatibilitas versi. Belum ditambahkan ke APK. |
| [Nextcloud Talk Android](https://github.com/nextcloud/talk-android) | Implementasi WebRTC Android, PeerConnection, ICE, signaling, screen capture | Metadata lisensi: NOASSERTION | Tidak menyalin kode sampai lisensi file dan dependency diverifikasi secara manual. Referensi teknis untuk kebutuhan WebRTC. |
| [Firebase Android Quickstarts](https://github.com/firebase/quickstart-android) | Contoh resmi Firebase Phone Auth, callback OTP, sign-in credential, Firebase APIs | Apache-2.0 | Referensi paling dekat dengan stack Chatin. Alur Phone Auth yang sudah ada perlu dibandingkan dengan sample resmi dan diuji pada perangkat nyata. Tidak perlu menyalin seluruh sample. |
| [Firebase Android SDK](https://github.com/firebase/firebase-android-sdk) | Implementasi SDK Firebase Android | Apache-2.0 | Dependency resmi dipakai melalui Firebase BoM; jangan vendoring source SDK ke aplikasi. |

## Temuan repository Chatin saat audit

### Android
- Kotlin + Jetpack Compose.
- Firebase Authentication Phone Auth dan Firestore telah dipakai pada kode Android.
- Room menyimpan antrean lokal; pesan pending belum memiliki rekonsiliasi server otomatis yang selesai.
- Profil Firebase kini diarahkan ke dokumen `users/{FirebaseAuth UID}` dan presence dibatasi pada UID sesi saat ini.
- Pengiriman pesan Firestore masih membutuhkan desain pembuatan chat dan daftar anggota terverifikasi.
- Panggilan belum memiliki transport media/signaling yang dapat dibuktikan. UI/konfigurasi kamera dan timer tidak membuktikan panggilan aktif.
- Pairing perangkat belum memiliki challenge QR, verifikasi sesi web, dan endpoint pencabutan yang nyata.
- Backup Google Drive dan cloud sync belum terhubung ke proses upload/restore terverifikasi.

### Backend
- `server.js` menggunakan Express, Socket.IO, PostgreSQL, JWT, bcrypt, Gemini SDK, dan S3-compatible SDK.
- Skema PostgreSQL mendefinisikan tabel pengguna, verifikasi nomor, pre-keys, dan pesan.
- Backend JWT/identifier/password adalah domain identitas terpisah dari Firebase Auth UID. Jangan menganggap token JWT tersebut otomatis membuktikan identitas Firebase.
- Header server menyebut E2EE, tetapi label/kolom ciphertext atau tabel pre-keys tidak membuktikan bahwa protokol E2EE sudah diimplementasikan dan diuji end-to-end.
- Endpoint WebAuthn yang hanya membuat challenge tidak cukup untuk menyatakan login passkey selesai; perlu challenge acak kriptografis, origin/RP ID validation, signature verification, anti-replay, dan credential lifecycle.
- Upload S3 harus memverifikasi otorisasi, tipe/ukuran file, private bucket, signed URL expiry, dan scanning bila diperlukan.
- `JWT_SECRET` diwajibkan saat startup; jangan gunakan fallback lemah atau default produksi.

## Rencana integrasi berurutan

1. **Build integrity** — debug/release compile, unit tests, dependency verification, APK signing report.
2. **Identity boundary** — satu identitas kanonis (Firebase UID atau backend identity yang ditautkan secara eksplisit), jangan mencampur phone number, local integer ID, dan JWT subject.
3. **Chat authorization** — buat chat lewat trusted server/Cloud Function; simpan participant UID; rules memeriksa membership pada setiap read/write; uji dengan Firebase Emulator.
4. **Message lifecycle** — outbox idempotent, server acknowledgement, retry/backoff, deduplication, delivered/read receipts hanya dari perangkat penerima.
5. **Media** — upload privat dengan URL berumur pendek, validasi MIME/size, checksum, retry, dan penghapusan.
6. **Push** — FCM token lifecycle, server-side send, notification permission, deep link, dan test multi-device.
7. **Calls** — pilih satu stack WebRTC/Jitsi, signaling, STUN/TURN, foreground service, audio focus, Bluetooth route, network handoff, incoming call wake-up, permission dan abuse controls.
8. **E2EE** — pilih protokol yang diaudit (misalnya Signal Protocol) dengan dependency dan lisensi yang disetujui; jangan gunakan helper kriptografi buatan sendiri sebagai klaim E2EE.
9. **Multi-device** — QR challenge sekali pakai, expiry, server nonce, authenticated approval, key registration, session revocation.
10. **Release** — README, screenshots perangkat nyata, changelog, signed APK/AAB, SHA-256, GitHub Release assets, dan smoke test.

## Status penggabungan kode open-source

Belum ada source code pihak ketiga yang disalin ke Chatin melalui audit ini. Itu disengaja: kandidat AGPL/NOASSERTION tidak boleh digabung tanpa persetujuan lisensi; Jitsi dan Firebase akan dievaluasi sebagai dependency resmi atau pola implementasi yang kompatibel, bukan menyalin seluruh aplikasi.

## Referensi primer

- Signal Android: https://github.com/signalapp/Signal-Android
- Element Android: https://github.com/element-hq/element-android
- Jitsi Meet: https://github.com/jitsi/jitsi-meet
- Nextcloud Talk Android: https://github.com/nextcloud/talk-android
- Firebase Quickstarts Android: https://github.com/firebase/quickstart-android
- Firebase Android SDK: https://github.com/firebase/firebase-android-sdk
