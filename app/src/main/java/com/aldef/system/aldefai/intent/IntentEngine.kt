package com.aldef.system.aldefai.intent

/**
 * Mesin pemahaman perintah lokal berbasis aturan (tanpa server, tanpa AI
 * eksternal).
 *
 * Alur: teks dinormalkan (huruf kecil, tanda baca dibuang, kata pengisi seperti
 * "tolong"/"saya ingin"/"aldef" dihilangkan) lalu dicocokkan dengan kumpulan
 * kata kunci. Cocok kata-kerja + sasaran memberi keyakinan tinggi; sasaran saja
 * lebih rendah. Ini menangani bahasa alami tanpa string persis.
 */
class IntentEngine : ALDEFIntentClassifier {

    override fun classify(text: String): ALDEFAIIntent {
        val ordered = orderedWords(text)
        val tokens = ordered.filterNot { it in FILLERS }.toSet()
        if (tokens.isEmpty()) return ALDEFAIIntent.Unknown(text)

        val hasVerb = tokens.any { it in OPEN_VERBS }
        fun has(vararg keys: String): Boolean = keys.any { it in tokens }
        fun conf(base: Float) = (if (hasVerb) base + 0.1f else base).coerceAtMost(0.98f)

        // 1. Navigasi kembali.
        if (has("kembali", "balik", "mundur", "back")) {
            return ALDEFAIIntent.Navigate(NavTarget.BACK, 0.9f)
        }
        // 2. Beranda.
        if (has("beranda", "home", "rumah", "utama", "depan", "dashboard")) {
            return ALDEFAIIntent.Navigate(NavTarget.HOME, conf(0.85f))
        }
        // 3. Pengaturan sistem.
        if (has("pengaturan", "setelan", "setting", "settingan", "konfigurasi")) {
            return ALDEFAIIntent.Navigate(NavTarget.SETTINGS, conf(0.85f))
        }
        // 3b. Fitur internal Aldef System.
        if (has("qris")) return ALDEFAIIntent.OpenFeature(FeatureTarget.QRIS, conf(0.88f))
        if (has("kompas", "kiblat")) return ALDEFAIIntent.OpenFeature(FeatureTarget.COMPASS, conf(0.88f))
        if (has("kalkulator", "kalkulasi", "hitung")) return ALDEFAIIntent.OpenFeature(FeatureTarget.CALCULATOR, conf(0.88f))
        if (has("speedometer", "speedo", "kecepatan")) return ALDEFAIIntent.OpenFeature(FeatureTarget.SPEEDOMETER, conf(0.88f))
        if (has("kalender", "penanggalan", "libur")) return ALDEFAIIntent.OpenFeature(FeatureTarget.CALENDAR, conf(0.88f))
        // 4. Bluetooth (cek sebelum aplikasi agar "buka bluetooth" tak salah arah).
        if (has("bluetooth", "blutut")) {
            val off = has("mati", "matikan", "nonaktif", "nonaktifkan", "off", "putus")
            val on = has("nyala", "nyalakan", "hidup", "hidupkan", "aktif", "aktifkan", "on", "sambung")
            val action = when {
                off -> DeviceAction.BLUETOOTH_OFF
                on -> DeviceAction.BLUETOOTH_ON
                else -> DeviceAction.BLUETOOTH_ON
            }
            return ALDEFAIIntent.Device(action, if (off || on) 0.92f else 0.7f)
        }
        // 5. Cuaca (dibuka lewat pencarian web resmi, tanpa API/berbayar).
        if (has("cuaca", "weather", "ramalan", "perkiraan", "hujan", "mendung")) {
            return ALDEFAIIntent.Weather(conf(0.82f))
        }
        // 5a. Alarm (cek sebelum "jam" agar "set alarm jam 6" tak jadi TellTime).
        if (has("alarm", "bangunkan", "bangunin", "weker")) {
            val (h, m) = parseTime(text)
            return ALDEFAIIntent.SetAlarm(h, m, if (h != null) 0.9f else 0.75f)
        }
        // 5b. Info yang cukup disebutkan (tak membuka aplikasi).
        if (has("jam", "pukul", "waktu")) return ALDEFAIIntent.TellTime(conf(0.85f))
        if (has("lokasi", "posisi", "gps", "alamat", "berada", "lokasiku")) {
            return ALDEFAIIntent.TellLocation(conf(0.85f))
        }
        if (has("tanggal")) return ALDEFAIIntent.TellDate(conf(0.85f))
        if (has("hari")) return ALDEFAIIntent.TellDay(conf(0.85f))
        // 6. Volume.
        if (has("volume", "suara", "audio")) {
            val up = has("naik", "naikkan", "keras", "besar", "tambah", "tinggi", "gede")
            val down = has("turun", "turunkan", "kecil", "pelan", "kurang", "kurangi", "rendah")
            if (up) return ALDEFAIIntent.Device(DeviceAction.VOLUME_UP, 0.9f)
            if (down) return ALDEFAIIntent.Device(DeviceAction.VOLUME_DOWN, 0.9f)
        }
        // 7. Aplikasi.
        if (has("kamera", "foto", "gambar", "selfie", "memotret", "motret", "jepret")) {
            return ALDEFAIIntent.OpenApp(AppTarget.CAMERA, conf(0.82f))
        }
        if (has("galeri", "galery", "gallery", "album")) {
            return ALDEFAIIntent.OpenApp(AppTarget.GALLERY, conf(0.82f))
        }
        if (has("browser", "peramban", "chrome", "internet", "jelajah", "web", "google", "gugel")) {
            return ALDEFAIIntent.OpenApp(AppTarget.BROWSER, conf(0.8f))
        }

        // 8. Cadangan: "buka <nama aplikasi>" apa pun yang terpasang.
        val nameWords = ordered.filterNot { it in OPEN_VERBS || it in FILLERS }
        if (hasVerb && nameWords.isNotEmpty()) {
            return ALDEFAIIntent.OpenAppByName(nameWords.joinToString(" "), 0.55f)
        }

        return ALDEFAIIntent.Unknown(text)
    }

    /** Mengurai jam & menit dari ucapan Indonesia (mis. "jam 6 pagi", "6:30"). */
    private fun parseTime(text: String): Pair<Int?, Int> {
        val t = text.lowercase()
        var hour: Int? = null
        var minute = 0
        Regex("""(\d{1,2})[:.](\d{2})""").find(t)?.let {
            hour = it.groupValues[1].toIntOrNull()
            minute = it.groupValues[2].toIntOrNull() ?: 0
        }
        if (hour == null) Regex("""setengah\s*(\d{1,2})""").find(t)?.let {
            val n = it.groupValues[1].toInt()
            hour = (n - 1 + 24) % 24
            minute = 30
        }
        if (hour == null) Regex("""(?:jam|pukul)\s*(\d{1,2})""").find(t)?.let {
            hour = it.groupValues[1].toIntOrNull()
        }
        if (hour == null) Regex("""(\d{1,2})""").find(t)?.let {
            hour = it.groupValues[1].toIntOrNull()
        }
        hour = hour?.let { h ->
            var hh = h
            val pagi = t.contains("pagi")
            val siangSore = t.contains("siang") || t.contains("sore")
            val malam = t.contains("malam")
            if ((siangSore || malam) && hh in 1..11) hh += 12
            if ((pagi || malam) && hh == 12) hh = 0
            hh.coerceIn(0, 23)
        }
        return hour to minute.coerceIn(0, 59)
    }

    private fun orderedWords(text: String): List<String> =
        text.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotBlank() }

    private companion object {
        val OPEN_VERBS = setOf(
            "buka", "bukakan", "jalankan", "mulai", "tampilkan", "aktifkan",
            "aktifin", "start", "launch", "pergi", "masuk", "cek", "lihat", "periksa"
        )
        val FILLERS = setOf(
            "tolong", "coba", "saya", "aku", "ingin", "mau", "pengen", "minta",
            "dong", "ya", "yah", "aldef", "ai", "hey", "hai", "halo", "bisa",
            "tolongin", "the", "ke", "yang", "itu", "ini", "aplikasi", "app"
        )
    }
}
