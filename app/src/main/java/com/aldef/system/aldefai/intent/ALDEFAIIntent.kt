package com.aldef.system.aldefai.intent

/** Aplikasi yang bisa dibuka lewat suara. */
enum class AppTarget(val label: String) {
    CAMERA("Kamera"),
    GALLERY("Galeri"),
    BROWSER("Browser")
}

/** Tujuan navigasi dalam sistem. */
enum class NavTarget(val label: String) {
    HOME("Beranda"),
    BACK("Kembali"),
    SETTINGS("Pengaturan")
}

/** Fitur internal Aldef System yang bisa dibuka via suara. */
enum class FeatureTarget(val label: String, val route: String) {
    QRIS("QRIS", "qris"),
    COMPASS("Kompas", "compass"),
    CALCULATOR("Kalkulator", "calculator"),
    SPEEDOMETER("Speedometer", "speedometer"),
    CALENDAR("Kalender", "calendar"),
    ALDEF_AI("Pengaturan ALDEF AI", "aldef_ai")
}

/** Aksi perangkat. */
enum class DeviceAction(val label: String) {
    BLUETOOTH_ON("Aktifkan Bluetooth"),
    BLUETOOTH_OFF("Matikan Bluetooth"),
    VOLUME_UP("Naikkan Volume"),
    VOLUME_DOWN("Turunkan Volume")
}

/**
 * Maksud terstruktur hasil pemahaman perintah suara.
 *
 * [confidence] 0..1 menandai keyakinan; [requiresConfirmation] menandai aksi
 * yang perlu dikonfirmasi sebelum dijalankan (dipakai Phase 8).
 */
sealed interface ALDEFAIIntent {
    val confidence: Float
    val label: String
    val requiresConfirmation: Boolean get() = false

    data class OpenApp(val app: AppTarget, override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Buka ${app.label}"
    }

    data class Navigate(val target: NavTarget, override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = target.label
    }

    data class Device(val action: DeviceAction, override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = action.label
    }

    data class Weather(override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Cuaca hari ini"
    }

    data class TellTime(override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Jam sekarang"
    }

    data class TellDate(override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Tanggal hari ini"
    }

    data class TellDay(override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Hari ini"
    }

    data class TellLocation(override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Lokasi saat ini"
    }

    /** Menyetel alarm. [hour] null = buka penyetelan alarm tanpa waktu spesifik. */
    data class SetAlarm(val hour: Int?, val minute: Int, override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() =
            if (hour != null) "Alarm %02d:%02d".format(hour, minute) else "Setel alarm"
    }

    /** Membuka fitur di dalam Aldef System. */
    data class OpenFeature(val feature: FeatureTarget, override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Buka ${feature.label}"
    }

    /** Membuka aplikasi terpasang mana pun berdasarkan nama ucapan. */
    data class OpenAppByName(val query: String, override val confidence: Float) : ALDEFAIIntent {
        override val label: String get() = "Buka \"$query\""
    }

    data class Unknown(val text: String) : ALDEFAIIntent {
        override val confidence: Float get() = 0f
        override val label: String get() = "Perintah belum dikenali"
    }
}

/**
 * Abstraksi pengklasifikasi maksud. Saat ini diisi [IntentEngine] berbasis
 * aturan; kelak bisa diganti pengklasifikasi AI on-device tanpa mengubah
 * pemanggilnya (lihat rencana Phase 10).
 */
interface ALDEFIntentClassifier {
    fun classify(text: String): ALDEFAIIntent
}
