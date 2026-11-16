# Audit keamanan backend Chatin

Status audit: 16 November 2026  
Target: `server.js`, `schema.sql`, `package.json` pada branch `feat/chatin-core-reliability`.

## Keputusan

**Jangan deploy backend ini ke internet atau gunakan untuk data pengguna nyata.** File ini adalah prototipe dengan beberapa endpoint yang tampak siap pakai, tetapi kontrol autentikasi, otorisasi, verifikasi kriptografi, privasi media, dan integrasi identitas belum memenuhi syarat produksi. Nama route, tabel `pre_keys`, kolom `ciphertext`, Socket.IO, atau dependency kriptografi tidak membuktikan bahwa E2EE telah berjalan.

## Temuan yang diverifikasi dari source

### Kritis — WebAuthn belum melakukan verifikasi autentikasi

- `GET /api/auth/webauthn-challenge` membentuk challenge dari `Date.now()` dan `Math.random()`; nilai tersebut bukan challenge kriptografis acak yang disimpan, kedaluwarsa, dan diikat ke sesi.
- `POST /api/auth/webauthn-verify` hanya menyimpan `credentialId` dan `publicKey` dari body setelah JWT. Tidak ada pemeriksaan attestation/assertion signature, origin, RP ID, challenge binding, counter, atau anti-replay.
- Endpoint tidak boleh disebut registrasi/verifikasi passkey atau biometrik yang valid.

### Kritis — Socket.IO mengandalkan JWT backend terpisah dari Firebase

- Socket middleware memverifikasi JWT yang diterbitkan endpoint login backend sendiri. Token ini tidak otomatis membuktikan Firebase Auth UID.
- Event `send_encrypted_message` menerima `penerimaId` dan payload dari klien, lalu menulis pesan tanpa memeriksa hubungan pertemanan, block list, izin percakapan, atau keanggotaan chat.
- Event `mark_message_read` mengubah status berdasarkan penerima yang login, tetapi mengirim notifikasi ke `pengirimId` dari payload klien, bukan mengambil sender dari row pesan yang berhasil diperbarui.
- Pemetaan `activeSocketUsers` hanya menyimpan satu socket per user dan menghapus entry tanpa memastikan socket yang disconnect adalah socket terakhir. Multi-device/reconnect dapat membuat status salah.
- Tidak ada limit payload/event, rate limit, idempotency key, atau kebijakan abuse yang memadai.

### Tinggi — Pre-key tidak diverifikasi dan tidak dibatasi pemiliknya

- `POST /api/pre-keys` menerima identity key, signed pre-key, signature, dan one-time pre-key dari request, lalu menyimpannya tanpa validasi format atau signature.
- `GET /api/pre-keys/:userId` membolehkan setiap pemegang JWT backend membaca pre-key user ID mana pun. Public key memang dapat dipublikasikan, tetapi endpoint tetap membutuhkan kontrol anti-enumerasi, validasi identitas dan mekanisme konsumsi one-time pre-key yang atomik.
- Tidak ada implementasi Signal Protocol session, ratchet, key rotation, key verification, atau test interoperability yang membuktikan E2EE.

### Tinggi — Upload media belum memiliki kontrol privasi yang cukup

- `POST /api/media/upload` menerima Base64 dan metadata dari klien tanpa batas ukuran khusus per file, validasi magic bytes, allowlist MIME, pemeriksaan nama file, atau scanning.
- Object key memasukkan nama file dari pengguna.
- URL hasil dibentuk sebagai URL S3 publik konvensional. Ketersediaan URL tersebut tidak membuktikan object private atau akses terotorisasi.
- Belum ada signed URL privat berumur pendek, aturan akses per chat, kuota, dan lifecycle penghapusan.

### Tinggi — TLS fallback dan konfigurasi PostgreSQL

- Server dapat berjalan dengan HTTP bila file sertifikat tidak ditemukan. Produksi harus memakai HTTPS yang dipaksakan pada reverse proxy tepercaya dan konfigurasi proxy yang benar.
- PostgreSQL SSL memakai `rejectUnauthorized: false`; ini menonaktifkan verifikasi sertifikat server dan membuka risiko koneksi ke endpoint palsu.
- CORS bukan kontrol autentikasi. Permintaan tanpa Origin tetap diizinkan, yang normal untuk sebagian klien native tetapi tidak boleh dianggap sebagai bukti identitas.

### Tinggi — Registrasi dan login backend merupakan identitas terpisah

- `/api/auth/register` dan `/api/auth/login` membuat akun identifier/password terpisah dari Firebase Phone Auth.
- Tidak ada tautan tepercaya antara backend JWT subject dan Firebase UID.
- Belum terlihat rate limit, perlindungan brute-force, kebijakan password, verifikasi identifier, refresh/revoke token, dan audit sesi.
- Jangan mencampur integer `users.id`, nomor telepon, dan Firebase UID sebagai identitas yang saling menggantikan.

### Sedang — AI proxy dan error handling

- Endpoint AI memerlukan JWT backend, tetapi belum ada pembatasan ukuran prompt, rate limit per akun, budget/kuota, atau kontrol penyalahgunaan.
- Error dari beberapa endpoint mengembalikan `err.message` ke klien; detail internal database/provider tidak boleh dibocorkan ke pengguna.

## Keputusan implementasi

1. Backend tidak dihubungkan ke aplikasi Android selama identitas backend dan Firebase belum disatukan secara eksplisit.
2. Jangan mengiklankan E2EE, WebAuthn/passkey, cloud backup, media privat, atau panggilan real-time berdasarkan source ini.
3. Sebelum mengaktifkan Socket.IO: gunakan Firebase UID atau identitas backend yang ditautkan dengan verifikasi server; validasi membership setiap pesan; ambil sender dari row DB; batasi event/payload; tambahkan rate limit, idempotency, dan pengujian otorisasi.
4. Sebelum mengaktifkan E2EE: integrasikan protokol yang diaudit dan kompatibel lisensi, jangan mengandalkan label `ciphertext` atau helper ECDH/AES mandiri.
5. Sebelum mengaktifkan WebAuthn: gunakan implementasi WebAuthn server yang matang dengan challenge CSPRNG, storage one-time, expiry, origin/RP ID binding, signature/attestation verification, counter dan anti-replay.
6. Sebelum mengaktifkan media: validasi isi file, private bucket, object authorization, signed URL terbatas, batas ukuran dan lifecycle.
7. Sebelum deployment: enforce TLS, verifikasi sertifikat PostgreSQL, secret manager, rate limiting, observability tanpa log data pribadi, backup/restore drill, dependency scanning, dan penetration test.

## Bukti verifikasi yang masih dibutuhkan

- Unit/integration tests untuk semua route dengan user A/B, token invalid/expired, payload malformed, replay, dan akses lintas akun.
- WebAuthn conformance tests dan pengujian browser/perangkat nyata.
- E2EE interoperability tests antara dua instalasi terpisah.
- Media access tests untuk user yang bukan anggota chat.
- Load, abuse, rate-limit, dan recovery tests.
- Bukti deployment TLS dan konfigurasi database yang tervalidasi.

**Kesimpulan:** backend saat ini adalah fondasi eksperimen, bukan backend messenger aman yang siap produksi. Pertahankan fitur terkait dalam status nonaktif sampai kontrol di atas diimplementasikan dan diuji.
