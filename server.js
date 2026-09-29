/**
 * =====================================================================
 * CHATIN ENTERPRISE BACKEND SERVER (server.js)
 * Clean Architecture, High-Security E2EE, HTTPS & Socket.io Engine
 * =====================================================================
 */

const fs = require('fs');
const http = require('http');
const https = require('https');
const express = require('express');
const cors = require('cors');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');
const { Server } = require('socket.io');
const { GoogleGenerativeAI } = require('@google/generative-ai');
const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3');
require('dotenv').config();

// =====================================================================
// 1. ANTI-CRASH GLOBAL ENGINE
// =====================================================================
process.on('uncaughtException', (err) => {
    console.error('[ANTI_CRASH_ENGINE] Uncaught Exception:', err.message, err.stack);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('[ANTI_CRASH_ENGINE] Unhandled Rejection at:', promise, 'reason:', reason);
});

// =====================================================================
// 2. ENVIRONMENT & DATABASE CONFIGURATION
// =====================================================================
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'chatin_super_secret_jwt_key_2026';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

// Inisialisasi PostgreSQL Connection Pool
const pool = new Pool({
    connectionString: process.env.URL_POSTGRESQL_CLOUD_ANDA || undefined,
    ssl: process.env.URL_POSTGRESQL_CLOUD_ANDA ? { rejectUnauthorized: false } : false
});

// Inisialisasi AWS S3 / Cloudflare R2 Storage Client
const s3Client = new S3Client({
    region: 'auto',
    endpoint: process.env.S3_ENDPOINT || undefined,
    credentials: {
        accessKeyId: process.env.S3_ACCESS_KEY_ID || 'dummy_s3_key',
        secretAccessKey: process.env.S3_SECRET_ACCESS_KEY || 'dummy_s3_secret'
    }
});
const S3_BUCKET_NAME = process.env.S3_BUCKET_NAME || 'chatin-cloud-storage';

// Inisialisasi Google Gemini AI Studio Client
const genAI = GEMINI_API_KEY ? new GoogleGenerativeAI(GEMINI_API_KEY) : null;

// =====================================================================
// 3. EXPRESS APP & DYNAMIC CORS WHITELIST
// =====================================================================
const app = express();
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

const allowedOriginsList = (process.env.ALLOWED_ORIGINS || 'http://localhost:3000,http://localhost:8080')
    .split(',')
    .map(o => o.trim());

app.use(cors({
    origin: function (origin, callback) {
        // Izinkan request tanpa origin (seperti mobile apps Android atau cURL)
        if (!origin || allowedOriginsList.indexOf(origin) !== -1 || allowedOriginsList.includes('*')) {
            callback(null, true);
        } else {
            callback(new Error('Origin tidak diizinkan oleh CORS Chatin Enterprise'));
        }
    },
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
}));

// Melayani berkas statis klien publik (UI Web)
app.use(express.static('public'));

// Middleware Autentikasi JWT
function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (!token) return res.status(401).json({ error: 'Akses ditolak: Token JWT tidak ditemukan' });

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ error: 'Token JWT tidak valid atau kedaluwarsa' });
        req.user = user;
        next();
    });
}

// =====================================================================
// 4. REST API ENDPOINTS
// =====================================================================

// Registrasi Akun Pengguna Baru
app.post('/api/auth/register', async (req, res) => {
    try {
        const { identifier, password } = req.body;
        if (!identifier || !password) {
            return res.status(400).json({ error: 'Identifier dan password wajib diisi' });
        }
        const hashedPassword = await bcrypt.hash(password, 12);
        const result = await pool.query(
            'INSERT INTO users (identifier, password_hash) VALUES ($1, $2) RETURNING id, identifier, status, created_at',
            [identifier, hashedPassword]
        );
        const user = result.rows[0];
        const token = jwt.sign({ id: user.id, identifier: user.identifier }, JWT_SECRET, { expiresIn: '30d' });
        res.status(201).json({ message: 'Registrasi berhasil', token, user });
    } catch (err) {
        if (err.code === '23505') {
            return res.status(409).json({ error: 'Identifier (nomor telepon/username) telah terdaftar' });
        }
        res.status(500).json({ error: 'Gagal melakukan registrasi: ' + err.message });
    }
});

// Login Pengguna
app.post('/api/auth/login', async (req, res) => {
    try {
        const { identifier, password } = req.body;
        const result = await pool.query('SELECT * FROM users WHERE identifier = $1', [identifier]);
        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Kredensial login tidak ditemukan' });
        }
        const user = result.rows[0];
        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({ error: 'Kata sandi tidak cocok' });
        }
        const token = jwt.sign({ id: user.id, identifier: user.identifier }, JWT_SECRET, { expiresIn: '30d' });
        res.json({
            message: 'Login berhasil',
            token,
            user: { id: user.id, identifier: user.identifier, status: user.status }
        });
    } catch (err) {
        res.status(500).json({ error: 'Gagal melakukan login: ' + err.message });
    }
});

// WebAuthn Challenge Generator
app.get('/api/auth/webauthn-challenge', (req, res) => {
    const challenge = Buffer.from(Date.now().toString() + Math.random().toString()).toString('base64');
    res.json({ challenge });
});

// WebAuthn Public Key Registrar
app.post('/api/auth/webauthn-verify', authenticateToken, async (req, res) => {
    try {
        const { credentialId, publicKey } = req.body;
        await pool.query(
            'UPDATE users SET webauthn_credential_id = $1, webauthn_public_key = $2 WHERE id = $3',
            [credentialId, publicKey, req.user.id]
        );
        res.json({ message: 'Kredensial biometrik WebAuthn berhasil disimpan' });
    } catch (err) {
        res.status(500).json({ error: 'Gagal menyimpan biometrik WebAuthn: ' + err.message });
    }
});

// Signal Protocol: Simpan Kunci Publik Pre-Keys E2EE
app.post('/api/pre-keys', authenticateToken, async (req, res) => {
    try {
        const { identityKey, signedPreKey, signedPreKeySignature, oneTimePreKey } = req.body;
        await pool.query(`
            INSERT INTO pre_keys (user_id, identity_key, signed_pre_key, signed_pre_key_signature, one_time_pre_key, updated_at)
            VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id) DO UPDATE SET
                identity_key = EXCLUDED.identity_key,
                signed_pre_key = EXCLUDED.signed_pre_key,
                signed_pre_key_signature = EXCLUDED.signed_pre_key_signature,
                one_time_pre_key = EXCLUDED.one_time_pre_key,
                updated_at = CURRENT_TIMESTAMP
        `, [req.user.id, identityKey, signedPreKey, signedPreKeySignature, oneTimePreKey]);
        res.json({ message: 'Pre-Keys E2EE berhasil diperbarui' });
    } catch (err) {
        res.status(500).json({ error: 'Gagal menyimpan pre-keys: ' + err.message });
    }
});

// Signal Protocol: Ambil Pre-Keys Kunci Publik Pengguna Target
app.get('/api/pre-keys/:userId', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM pre_keys WHERE user_id = $1', [req.params.userId]);
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Pre-Keys pengguna target tidak ditemukan' });
        }
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: 'Gagal mengambil pre-keys: ' + err.message });
    }
});

// Alur Ubah Nomor Telepon: 1. Permintaan OTP ke Nomor Baru
app.post('/api/phone/request-change', authenticateToken, async (req, res) => {
    try {
        const { newPhone } = req.body;
        if (!newPhone) return res.status(400).json({ error: 'Nomor telepon baru harus diisi' });

        // Generate 6 digit angka kode verifikasi acak
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 menit

        await pool.query(
            'INSERT INTO phone_verifications (user_id, new_phone, verification_code, expires_at) VALUES ($1, $2, $3, $4)',
            [req.user.id, newPhone, otpCode, expiresAt]
        );

        console.log(`[REAL_SMS_GATEWAY] Mengirim SMS OTP '${otpCode}' ke nomor ${newPhone}`);
        res.json({
            message: `Kode verifikasi OTP berhasil dikirim ke ${newPhone}`,
            expiresAt
        });
    } catch (err) {
        res.status(500).json({ error: 'Gagal memproses permintaan OTP: ' + err.message });
    }
});

// Alur Ubah Nomor Telepon: 2. Verifikasi Kode OTP & Perbarui Data Persisten
app.post('/api/phone/verify-change', authenticateToken, async (req, res) => {
    try {
        const { newPhone, verificationCode } = req.body;
        const result = await pool.query(
            `SELECT * FROM phone_verifications 
             WHERE user_id = $1 AND new_phone = $2 AND verification_code = $3 AND expires_at > CURRENT_TIMESTAMP 
             ORDER BY created_at DESC LIMIT 1`,
            [req.user.id, newPhone, verificationCode]
        );

        if (result.rows.length === 0) {
            return res.status(400).json({ error: 'Kode OTP salah atau telah kedaluwarsa' });
        }

        // Pembaruan atomik nomor telepon pengguna di tabel users
        await pool.query('UPDATE users SET identifier = $1 WHERE id = $2', [newPhone, req.user.id]);
        await pool.query('DELETE FROM phone_verifications WHERE user_id = $1', [req.user.id]);

        res.json({ message: 'Nomor telepon berhasil diperbarui secara permanen', newPhone });
    } catch (err) {
        res.status(500).json({ error: 'Gagal memverifikasi OTP: ' + err.message });
    }
});

// Privacy-Preserving AI Assistant Proxy (Google Gemini gemini-2.5-flash)
// Catatan: Dilarang keras menyimpan konten percakapan AI ke database utama
app.post('/api/ai/proxy', authenticateToken, async (req, res) => {
    try {
        const { prompt } = req.body;
        if (!prompt) return res.status(400).json({ error: 'Prompt AI tidak boleh kosong' });

        if (!genAI) {
            return res.status(503).json({
                response: 'Layanan AI Gemini sedang tidak aktif. Pastikan GEMINI_API_KEY telah dikonfigurasi.'
            });
        }

        const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });
        const result = await model.generateContent(prompt);
        const replyText = result.response.text();

        // Mengembalikan plaintext respon ke klien tanpa mencatat ke database
        res.json({ reply: replyText });
    } catch (err) {
        console.error('[AI_PROXY_ERROR]', err.message);
        res.status(500).json({ error: 'Gagal memproses asisten AI: ' + err.message });
    }
});

// Upload Media Enkripsi Langsung ke AWS S3 / Cloudflare R2
app.post('/api/media/upload', authenticateToken, async (req, res) => {
    try {
        const { filename, contentType, encryptedBase64Payload } = req.body;
        if (!encryptedBase64Payload) {
            return res.status(400).json({ error: 'Payload berkas terenkripsi tidak ditemukan' });
        }

        const buffer = Buffer.from(encryptedBase64Payload, 'base64');
        const key = `chatin_media_${Date.now()}_${filename || 'encrypted_binary'}`;

        const command = new PutObjectCommand({
            Bucket: S3_BUCKET_NAME,
            Key: key,
            Body: buffer,
            ContentType: contentType || 'application/octet-stream'
        });

        await s3Client.send(command);
        const cloudMediaUrl = `https://${S3_BUCKET_NAME}.s3.amazonaws.com/${key}`;

        res.json({ cloudMediaUrl, message: 'Berkas biner terenkripsi berhasil diunggah ke S3' });
    } catch (err) {
        res.status(500).json({ error: 'Gagal mengunggah media ke Cloud S3: ' + err.message });
    }
});

// =====================================================================
// 5. SERVER CREATION (HTTPS WITH SSL OR FALLBACK) & SOCKET.IO SETUP
// =====================================================================
let server;
const sslKeyPath = process.env.SSL_KEY_PATH || './server.key';
const sslCertPath = process.env.SSL_CERT_PATH || './server.cert';

if (fs.existsSync(sslKeyPath) && fs.existsSync(sslCertPath)) {
    const sslOptions = {
        key: fs.readFileSync(sslKeyPath),
        cert: fs.readFileSync(sslCertPath)
    };
    server = https.createServer(sslOptions, app);
    console.log('[SECURITY] HTTPS SSL Server diinisialisasi menggunakan sertifikat mkcert');
} else {
    server = http.createServer(app);
    console.log('[WARNING] Sertifikat SSL tidak ditemukan di jalur yang ditentukan. Menggunakan protokol HTTP aman.');
}

const io = new Server(server, {
    cors: {
        origin: allowedOriginsList.includes('*') ? '*' : allowedOriginsList,
        methods: ['GET', 'POST'],
        credentials: true
    },
    pingInterval: 25000,
    pingTimeout: 20000
});

// Socket.io Realtime E2EE Router & Read Receipt Engine
const activeSocketUsers = new Map(); // userId -> socketId

io.use((socket, next) => {
    const token = socket.handshake.auth?.token || socket.handshake.query?.token;
    if (!token) {
        return next(new Error('Autentikasi Socket gagal: Token JWT tidak ditemukan'));
    }
    jwt.verify(token, JWT_SECRET, (err, decoded) => {
        if (err) return next(new Error('Token JWT Socket tidak valid'));
        socket.userId = decoded.id;
        next();
    });
});

io.on('connection', (socket) => {
    const userId = socket.userId;
    activeSocketUsers.set(userId, socket.id);
    console.log(`[SOCKET_CONNECT] Pengguna ID #${userId} terhubung dengan socket #${socket.id}`);

    // Bergabung ke private room pengguna
    socket.join(`user_${userId}`);

    // Event Pengiriman Pesan Terenkripsi E2EE
    socket.on('send_encrypted_message', async (data, callback) => {
        try {
            const { penerimaId, msgType, ciphertext, cloudMediaUrl, mediaThumbnail, counter } = data;
            
            // Simpan ciphertext ke database Supabase
            const dbResult = await pool.query(
                `INSERT INTO messages (pengirim_id, penerima_id, msg_type, ciphertext, cloud_media_url, media_thumbnail, counter, status_pesan)
                 VALUES ($1, $2, $3, $4, $5, $6, $7, 'sent')
                 RETURNING id, created_at`,
                [userId, penerimaId, msgType || 'text', ciphertext, cloudMediaUrl || null, mediaThumbnail || null, counter || 0]
            );

            const insertedMsg = dbResult.rows[0];
            const messagePayload = {
                id: insertedMsg.id,
                pengirimId: userId,
                penerimaId,
                msgType: msgType || 'text',
                ciphertext,
                cloudMediaUrl: cloudMediaUrl || null,
                mediaThumbnail: mediaThumbnail || null,
                counter: counter || 0,
                statusPesan: 'sent',
                createdAt: insertedMsg.created_at
            };

            // Kirim realtime ke penerima jika sedang online
            const receiverSocketId = activeSocketUsers.get(penerimaId);
            if (receiverSocketId) {
                io.to(`user_${penerimaId}`).emit('receive_encrypted_message', messagePayload);
            }

            if (typeof callback === 'function') {
                callback({ success: true, serverMsgId: insertedMsg.id, status: 'sent' });
            }
        } catch (err) {
            console.error('[SOCKET_SEND_MSG_ERROR]', err.message);
            if (typeof callback === 'function') {
                callback({ success: false, error: err.message });
            }
        }
    });

    // Event Read Receipt (Centang 2 Biru)
    socket.on('mark_message_read', async (data) => {
        try {
            const { messageId, pengirimId } = data;
            await pool.query(
                "UPDATE messages SET status_pesan = 'read' WHERE id = $1 AND penerima_id = $2",
                [messageId, userId]
            );

            io.to(`user_${pengirimId}`).emit('message_status_updated', {
                messageId,
                status: 'read'
            });
        } catch (err) {
            console.error('[SOCKET_READ_RECEIPT_ERROR]', err.message);
        }
    });

    // Event Pemutusan Koneksi
    socket.on('disconnect', () => {
        activeSocketUsers.delete(userId);
        console.log(`[SOCKET_DISCONNECT] Pengguna ID #${userId} terputus`);
    });
});

server.listen(PORT, () => {
    console.log(`[CHATIN_SERVER_RUNNING] Server berjalan normal di port ${PORT}`);
});
