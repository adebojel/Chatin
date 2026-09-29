package com.example.data.models

enum class CallMediaType {
    AUDIO,
    VIDEO
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallNetworkMode {
    VOIP_WIFI,        // High-bitrate 1080p60 WebRTC over Wi-Fi HD
    VOIP_CELLULAR,    // Adaptive-bitrate 720p WebRTC over 4G/5G
    GSM_OPERATOR      // Native Telephony Carrier circuit-switched GSM call
}

data class CallRecord(
    val id: String,
    val contactName: String,
    val phoneNumber: String,
    val avatarColorHex: Long = 0xFF00E5FF,
    val mediaType: CallMediaType = CallMediaType.AUDIO,
    val direction: CallDirection = CallDirection.OUTGOING,
    val networkMode: CallNetworkMode = CallNetworkMode.VOIP_WIFI,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val qualityBitrateKbps: Int = 2400
)

enum class AppThemeMode {
    DARK_NEON,
    CLEAN_WHITE,
    SYSTEM_AUTO
}

enum class BackgroundPreset(
    val title: String,
    val description: String,
    val emojiIcon: String = "✨"
) {
    CYBER_DARK("Futuristik Gelap", "Gaya glassmorphic neon modern", "⚡"),
    CLEAN_MINIMAL("Putih Bersih", "Gaya minimalis WhatsApp modern", "❄️"),
    SKY("Pemandangan Langit", "Awan halus & aurora langit biru", "☁️"),
    NATURE("Pemandangan Alam", "Bukit hijau & cahaya pagi hangat", "🌿"),
    FOREST("Pemandangan Hutan", "Hutan pinus zamrud yang menenangkan", "🌲"),
    OCEAN("Pemandangan Laut", "Ombak laut biru kristal jernih", "🌊"),
    CUSTOM_GALLERY("Galeri Perangkat", "Pilih foto favorit dari penyimpanan", "🖼️"),
    AI_GENERATED("Kreasi Cerdas AI", "Dihasilkan dengan visual generator", "🎨")
}

data class ThemeState(
    val themeMode: AppThemeMode = AppThemeMode.DARK_NEON,
    val backgroundPreset: BackgroundPreset = BackgroundPreset.CYBER_DARK,
    val customGalleryUri: String? = null,
    val aiPrompt: String = "Lembah kosmik futuristik dengan tanaman neon bersinar",
    val primaryAccentHex: Long = 0xFF00E5FF,
    val chatBubbleColorSentHex: Long = 0xFF144272,
    val glassTransparency: Float = 0.85f
)

enum class ColorFilterType(val label: String, val iconEmoji: String) {
    ORIGINAL("Asli", "✨"),
    BEAUTY_GLOW("Beauty Smooth", "🌸"),
    CYBER_NEON("Cyberpunk Neon", "⚡"),
    MONO_NOIR("Monokrom Noir", "🎬"),
    VINTAGE_WARM("Vintage 90s", "📻"),
    GOLDEN_HOUR("Golden Hour", "🌅"),
    EMERALD_MINT("Emerald Clean", "🍃")
}

enum class VirtualBackgroundMode(val label: String, val iconEmoji: String = "") {
    NONE("Normal"),
    BLUR_LIGHT("Blur Ringan"),
    BLUR_INTENSE("Blur Kuat"),
    GREEN_SCREEN_GALLERY("Green Screen (Galeri)", "🖼️"),
    VIRTUAL_OFFICE("Studio Kantor"),
    VIRTUAL_AURORA("Aurora Malam"),
    VIRTUAL_SUNSET("Sunset Pantai"),
    VIRTUAL_ZEN_GARDEN("Taman Hutan")
}

enum class ArEffectCategory(val title: String) {
    KEREN("⚡ Keren"),
    GOKIL_LUCU("🤡 Gokil & Lucu"),
    MARAH("🔥 Marah"),
    HEWAN("🐱 Hewan"),
    BUAH("🍓 Buah")
}

enum class ArFaceEffect(
    val label: String,
    val emoji: String,
    val category: ArEffectCategory
) {
    NONE("Tanpa Efek", "🚫", ArEffectCategory.KEREN),
    CYBER_GLASSES("Kacamata Neon", "👓", ArEffectCategory.KEREN),
    NEON_AURA("Aura Menyala", "✨", ArEffectCategory.KEREN),
    STAR_BURST("Taburan Bintang", "⭐", ArEffectCategory.KEREN),
    FIRE_CROWN("Mahkota Api", "👑", ArEffectCategory.KEREN),
    LASER_EYES("Mata Laser Cyber", "👀", ArEffectCategory.KEREN),
    CLOWN_FACE("Badut Kocak", "🤡", ArEffectCategory.GOKIL_LUCU),
    BIG_MOUTH("Mulut Monster", "👄", ArEffectCategory.GOKIL_LUCU),
    DERP_GLASSES("Kacamata Konyol", "🤪", ArEffectCategory.GOKIL_LUCU),
    PARTY_BLASTER("Pesta Ultah", "🎉", ArEffectCategory.GOKIL_LUCU),
    ANGRY_DEVIL("Tanduk Iblis", "😈", ArEffectCategory.MARAH),
    HEAD_STEAM("Kepala Berasap", "😤", ArEffectCategory.MARAH),
    LIGHTNING_STORM("Petir Murka", "⚡", ArEffectCategory.MARAH),
    CAT_EARS("Telinga Kucing", "🐱", ArEffectCategory.HEWAN),
    DOG_NOSE("Anjing Gemas", "🐶", ArEffectCategory.HEWAN),
    BUNNY_EARS("Kelinci Lucu", "🐰", ArEffectCategory.HEWAN),
    PANDA_HOOD("Panda Imut", "🐼", ArEffectCategory.HEWAN),
    STRAWBERRY_HAT("Topi Stroberi", "🍓", ArEffectCategory.BUAH),
    WATERMELON_GLASSES("Kacamata Semangka", "🍉", ArEffectCategory.BUAH),
    PINEAPPLE_CROWN("Nanas Tropis", "🍍", ArEffectCategory.BUAH),
    BANANA_RAIN("Hujan Pisang", "🍌", ArEffectCategory.BUAH)
}

enum class VoiceChangerEffect(
    val label: String,
    val emoji: String,
    val description: String,
    val pitchMultiplier: Float
) {
    NORMAL("Suara Asli", "🎙️", "Audio natural tanpa manipulasi frekuensi", 1.0f),
    CHIPMUNK("Tupai / Helium", "🐿️", "Pitch tinggi ceria modulasi helium", 1.6f),
    ROBOT_CYBER("Robot Cyberpunk", "🤖", "Efek vocoder metalik futuristik", 0.9f),
    DEEP_TITAN("Monster / Raksasa", "👹", "Pitch bass berat resonansi dalam", 0.65f),
    ANIME_GIRL("Karakter Anime", "🎀", "Frekuensi feminin lembut manis", 1.4f),
    ALIEN_SPACE("Alien Luar Angkasa", "👽", "Modulasi resonansi kosmik flanger", 1.25f),
    RADIO_WALKIE("Walkie-Talkie Radio", "📻", "Filter bandpass telekomunikasi darurat", 1.05f),
    ECHO_CAVE("Gema Gua Megah", "🏔️", "Delay reverb atmosferik lebar", 0.95f)
}

data class VideoCallOptions(
    val selectedFilter: ColorFilterType = ColorFilterType.ORIGINAL,
    val backgroundMode: VirtualBackgroundMode = VirtualBackgroundMode.NONE,
    val greenScreenCustomUri: String? = null,
    val faceEffect: ArFaceEffect = ArFaceEffect.NONE,
    val voiceChanger: VoiceChangerEffect = VoiceChangerEffect.NORMAL,
    val isMicMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isScreenSharing: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val isFloatingPipActive: Boolean = false,
    val isEffectDrawerOpen: Boolean = false,
    val bitrateKbps: Int = 2500,
    val networkMode: CallNetworkMode = CallNetworkMode.VOIP_WIFI
)
