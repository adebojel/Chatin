CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabel Pengguna Utama Cloud
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    identifier VARCHAR(100) UNIQUE NOT NULL, -- Nomor Telepon atau Email asli
    password_hash VARCHAR(255) NOT NULL,      -- Hasil Hashing Bcrypt
    status VARCHAR(255) DEFAULT 'Available',
    webauthn_credential_id TEXT UNIQUE,
    webauthn_public_key TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_identifier ON users(identifier);

-- Tabel Verifikasi Perubahan Nomor Telepon Nyata
CREATE TABLE IF NOT EXISTS phone_verifications (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    new_phone VARCHAR(100) NOT NULL,
    verification_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabel Penyimpanan Bundel Kunci Publik (Signal-style E2EE)
CREATE TABLE IF NOT EXISTS pre_keys (
    user_id INT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    identity_key TEXT NOT NULL,         
    signed_pre_key TEXT NOT NULL,       
    signed_pre_key_signature TEXT NOT NULL,
    one_time_pre_key TEXT,              
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabel Pesan Terenkripsi Sinkron & Asinkron
CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    pengirim_id INT REFERENCES users(id) ON DELETE SET NULL,
    penerima_id INT REFERENCES users(id) ON DELETE SET NULL,
    msg_type VARCHAR(20) DEFAULT 'text',
    ciphertext TEXT NOT NULL,                
    cloud_media_url TEXT,                    
    media_thumbnail TEXT,                    
    counter INT NOT NULL DEFAULT 0,
    status_pesan VARCHAR(20) DEFAULT 'sent', -- 'sent', 'delivered', 'read'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_messages_chat_flow ON messages(pengirim_id, penerima_id);
