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


## Kandidat tambahan yang diperiksa (16 November 2026)

| Repository | Temuan yang dapat diverifikasi | Lisensi / keputusan |
| --- | --- | --- |
| [firebase/quickstart-android](https://github.com/firebase/quickstart-android) | Contoh resmi Firebase Android termasuk FCM service, token registration, permission notifikasi Android 13+, dan handler pesan. | Apache-2.0 (metadata GitHub). Referensi paling aman untuk pola FCM. Belum disalin: Chatin belum memiliki endpoint backend tepercaya untuk mengikat token instalasi ke Firebase UID dan mengirim notifikasi tanpa membocorkan isi pesan. |
| [rvirayii-est/offline-first-kotlin-demo](https://github.com/rvirayii-est/offline-first-kotlin-demo) | Contoh Room transactional outbox, WorkManager retry/backoff, operation ID idempotent, serta dokumentasi pemisahan transport. Backend demo bersifat mock dan adapter Supabase/Spring Boot belum dikonfigurasi. | Lisensi belum diverifikasi pada audit ini. Tidak disalin. Pola arsitektur dapat menjadi bahan desain, tetapi implementasi mock tidak boleh dipakai untuk mengklaim sinkronisasi Chatin nyata. |
| [Alidon256/Hi](https://github.com/Alidon256/Hi) | Aplikasi chat Android Kotlin/Firebase; README menyatakan Apache 2.0 dan menyebut Auth, Firestore, FCM, Storage. Metadata API GitHub tidak mengembalikan lisensi terverifikasi dalam pemeriksaan ini. | Tahan integrasi sampai file LICENSE, dependency, aturan Firestore, dan kualitas implementasi diperiksa langsung. Tidak disalin. |
| [Nikhil-z/Kotlin-Android-sample](https://github.com/Nikhil-z/Kotlin-Android-sample) | Contoh Android Kotlin/Room/Retrofit/offline-first, tetapi bukan sistem messenger atau backend chat. | MIT (metadata GitHub). Tidak disalin karena transport contoh bukan layanan pesan dan tidak menyelesaikan kebutuhan otorisasi chat. |
| [CodeWithBaji/OfflineTodoApp](https://github.com/CodeWithBaji/OfflineTodoApp) | Compose/Room/WorkManager sample yang secara eksplisit memakai FakeRemoteDataSource dan fake backend. | Tidak layak untuk integrasi produk; README menyebut backend palsu. Tidak disalin. |

### Keputusan integrasi

- Belum ada kode pihak ketiga baru yang disalin ke aplikasi. Repo yang memakai mock transport, tidak memiliki lisensi yang terverifikasi, atau tidak cocok dengan domain messenger tidak boleh digabung hanya demi menambah jumlah fitur.
- Langkah FCM berikutnya harus mencakup Android `FirebaseMessagingService`, token lifecycle, izin notifikasi, server-side token registration yang memverifikasi Firebase ID token, pengiriman FCM melalui trusted server, serta penghapusan token saat logout. Sampai backend tersebut tersedia, push notification tetap belum tersedia.
- Langkah offline berikutnya harus memakai transactional outbox, operation ID stabil, WorkManager unique work, retry/backoff, dan ack server yang mengikat UID + chat membership. Jangan menambahkan worker yang hanya mengirim ke mock endpoint.
- Kandidat WebRTC harus diperiksa pada level SDK/API, lisensi transitive/native dependency, signaling, ICE/STUN/TURN, dan keamanan token. UI tombol panggilan saja bukan implementasi media call.


## Audit repository lanjutan: SDK dan fondasi aplikasi (16 November 2026)

Pemeriksaan lanjutan dilakukan pada metadata GitHub dan dokumentasi repository. Angka bintang dan tanggal push bersifat snapshot saat audit, bukan ukuran kualitas atau jaminan keamanan.

| Repository | Snapshot / lisensi | Komponen relevan | Keputusan teknis |
| --- | --- | --- | --- |
| [livekit/client-sdk-android](https://github.com/livekit/client-sdk-android) | Kotlin; Apache-2.0; 371 stars; aktif diperbarui 16 Nov 2026 | SDK Android untuk audio/video/data realtime dengan LiveKit Cloud atau server self-hosted | Kandidat utama POC panggilan. Tetap perlu token endpoint tepercaya, room authorization, TURN/media topology, foreground service, permission, audio routing, dan pengujian dua perangkat. Belum ditambahkan ke dependency aplikasi. |
| [GetStream/webrtc-android](https://github.com/GetStream/webrtc-android) | Kotlin; Apache-2.0; 890 stars; push 5 Nov 2026 | Distribusi WebRTC Android terkompilasi, Kotlin extensions dan komponen Compose | Kandidat alternatif bila Chatin memilih membangun signaling dan media plane sendiri. Perlu evaluasi ukuran native ABI, benturan dependency WebRTC, lifecycle, ICE/TURN dan pemeliharaan. Jangan memasukkan bersama LiveKit sebelum pilihan arsitektur ditetapkan. |
| [uditbhaskar/Whizzz-The-ChatApp](https://github.com/uditbhaskar/Whizzz-The-ChatApp) | Kotlin; MIT; 74 stars; push 27 Apr 2026 | Multi-module Clean Architecture/MVI, Firebase Auth, Realtime Database, FCM, Compose | Referensi pola pemisahan app/core/data/domain/feature/firebase/functions. Repository kecil dan riwayat commit terbatas; audit rules, Functions, dan dependency sebelum mengadopsi pola. Tidak menyalin seluruh aplikasi. |
| [element-hq/element-x-android](https://github.com/element-hq/element-x-android) | Kotlin; AGPL-3.0; 2.5k stars; aktif diperbarui 16 Nov 2026 | Arsitektur messenger Compose modern, Matrix Rust SDK, navigasi, test UI, multi-device, room dan sync | Referensi arsitektur/UX dan studi protokol saja. Tidak menyalin kode tanpa keputusan lisensi AGPL atau lisensi komersial yang sesuai. Matrix Rust SDK bukan drop-in replacement untuk model Firebase Chatin. |
| [zohrevand/dialogue](https://github.com/zohrevand/dialogue) | Kotlin; Apache-2.0; terakhir push 3 Sep 2022 | XMPP/Jabber client dengan Smack dan Jetpack Compose; pemisahan service XMPP dan feature modules | Contoh arsitektur protokol alternatif, tetapi sudah lama tidak aktif. Tidak dipakai untuk implementasi produksi tanpa pembaruan dependency, audit keamanan dan interop. |
| [firebase/quickstart-android](https://github.com/firebase/quickstart-android) | Kotlin; Apache-2.0; 9.3k stars; aktif diperbarui 15 Nov 2026 | Sample resmi Firebase Auth, FCM, Firestore dan integrasi Android | Rujukan resmi implementasi Firebase. Pilih pola dan dependency yang relevan; jangan vendoring seluruh sample. FCM masih membutuhkan trusted server sender dan token ownership. |
| [signalapp/Signal-Android](https://github.com/signalapp/Signal-Android) | Kotlin; AGPL-3.0 | Referensi desain messenger, attachment pipeline, database, protokol keamanan | Referensi threat model dan arsitektur saja; AGPL dan kompleksitas protokol menghalangi penyalinan langsung tanpa persetujuan. |
| [element-hq/element-android](https://github.com/element-hq/element-android) | Kotlin; AGPL-3.0 | Matrix client generasi sebelumnya, masih mendapat security maintenance | Bukan basis kode baru; Element X adalah generasi berikutnya. Referensi historis saja. |

### Prioritas implementasi yang disarankan

1. **P0 — keamanan identitas dan chat**: satukan Firebase UID sebagai identitas otoritatif, buat chat/participant melalui trusted backend atau Cloud Functions, terapkan Firestore Rules berbasis membership, uji negatif dengan Firebase Emulator. Ini prasyarat sebelum pesan lintas akun dinyatakan aman.
2. **P1 — reliability pesan**: Room transactional outbox dengan UUID operation ID, unique WorkManager jobs, exponential backoff, server idempotency, acknowledgement dan deduplication. Jangan menghapus pesan lokal saat upload gagal.
3. **P2 — FCM**: registrasi token per instalasi yang diikat ke Firebase ID token, rotasi token, revoke saat logout, server-side FCM sender, Android 13+ notification permission, deep links, dan test delivery.
4. **P3 — panggilan**: lakukan spike LiveKit terlebih dahulu karena SDK Android aktif dan Apache-2.0. Bangun endpoint token di server yang memverifikasi Firebase ID token serta otorisasi room. Uji audio-only, video, permission denial, background/foreground, Bluetooth, network switching, TURN, dan dua perangkat fisik. Jangan menggabungkan LiveKit dengan stack WebRTC lain pada saat yang sama.
5. **P4 — media**: upload privat melalui signed URL berumur pendek, MIME sniffing, batas ukuran, checksum, antivirus bila diperlukan, metadata ownership dan deletion.
6. **P5 — E2EE**: pilih protokol standar dan implementasi yang diaudit. Lakukan threat model, key lifecycle, multi-device, backup/recovery dan external review. Jangan mengiklankan E2EE sebelum interoperabilitas dan pengujian selesai.
7. **P6 — UI/UX**: gunakan Material 3, Compose adaptive layouts, aksesibilitas TalkBack, state/loading/error/empty yang konsisten, animasi ringan dan screenshot dari build Chatin yang benar-benar berjalan.

### Batasan pengambilan kode

- Audit ini tidak berarti seluruh repository kandidat sudah diunduh atau semua file telah ditinjau baris demi baris.
- Metadata lisensi repository tidak otomatis membuktikan lisensi semua aset, submodule, model, font, atau dependency transitif.
- Tidak ada source code pihak ketiga yang disalin pada tahap ini. Integrasi dependency SDK akan dilakukan setelah pilihan arsitektur, versi, lisensi, minSdk, ukuran APK, dan pengujian kompatibilitas disetujui.
- Hindari mengambil screenshot, logo, atau aset UI dari aplikasi lain lalu menampilkannya sebagai screenshot Chatin. Dokumentasi Chatin harus memakai screenshot aplikasi sendiri dari build yang tervalidasi.
