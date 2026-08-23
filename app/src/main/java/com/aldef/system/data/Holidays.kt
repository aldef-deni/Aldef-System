package com.aldef.system.data

import java.time.LocalDate

/**
 * Hari libur nasional Indonesia + penanda "tanggal merah".
 *
 * Tiga sumber tanggal digabung:
 *  1. **Tetap** — tanggalnya sama tiap tahun (Tahun Baru, Buruh, Pancasila,
 *     Kemerdekaan, Natal). Selalu akurat.
 *  2. **Kristen bergerak** — Jumat Agung & Kenaikan Isa Almasih dihitung dari
 *     tanggal Paskah (algoritma Computus). Akurat untuk tahun berapa pun.
 *  3. **Lunar/candra** — Imlek, Nyepi, Idul Fitri/Adha, Waisak, Tahun Baru
 *     Islam, Maulid, Isra Miraj. Tanggalnya ditetapkan pemerintah lewat SKB 3
 *     Menteri dan bergeser tiap tahun, jadi diisi tabel [variable] per tahun.
 *     **Perbarui tabel ini bila SKB resmi berubah.**
 */
object Holidays {

    data class Holiday(val date: LocalDate, val name: String)

    // Hari Minggu selalu tanggal merah di kalender Indonesia.
    fun isSunday(date: LocalDate): Boolean = date.dayOfWeek == java.time.DayOfWeek.SUNDAY

    /** Tanggal merah = hari Minggu atau ada libur nasional. */
    fun isRedDate(date: LocalDate): Boolean = isSunday(date) || holidaysOn(date).isNotEmpty()

    fun holidaysOn(date: LocalDate): List<Holiday> =
        holidaysForYear(date.year).filter { it.date == date }

    /** Semua libur nasional pada [year], terurut tanggal. */
    fun holidaysForYear(year: Int): List<Holiday> {
        val list = mutableListOf<Holiday>()

        // 1. Tetap.
        list += Holiday(LocalDate.of(year, 1, 1), "Tahun Baru Masehi")
        list += Holiday(LocalDate.of(year, 5, 1), "Hari Buruh Internasional")
        list += Holiday(LocalDate.of(year, 6, 1), "Hari Lahir Pancasila")
        list += Holiday(LocalDate.of(year, 8, 17), "Hari Kemerdekaan RI")
        list += Holiday(LocalDate.of(year, 12, 25), "Hari Raya Natal")

        // 2. Kristen bergerak.
        val easter = easterSunday(year)
        list += Holiday(easter.minusDays(2), "Wafat Isa Almasih (Jumat Agung)")
        list += Holiday(easter.plusDays(39), "Kenaikan Isa Almasih")

        // 3. Lunar per tahun.
        variable[year]?.forEach { (iso, name) ->
            list += Holiday(LocalDate.parse(iso), name)
        }

        return list.sortedBy { it.date }
    }

    /**
     * Algoritma Meeus/Jones/Butcher untuk Minggu Paskah (kalender Gregorian).
     * Deterministik untuk tahun mana pun, tanpa perlu tabel.
     */
    private fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    /**
     * Tanggal libur berbasis penanggalan lunar per tahun (format ISO
     * `YYYY-MM-DD`). Tanggal 2026–2027 adalah perkiraan terbaik menurut kalender
     * yang beredar; sinkronkan dengan SKB 3 Menteri resmi saat terbit.
     */
    private val variable: Map<Int, List<Pair<String, String>>> = mapOf(
        2025 to listOf(
            "2025-01-27" to "Isra Mikraj Nabi Muhammad SAW",
            "2025-01-29" to "Tahun Baru Imlek 2576",
            "2025-03-29" to "Hari Suci Nyepi (Tahun Baru Saka 1947)",
            "2025-03-31" to "Hari Raya Idulfitri 1446 H",
            "2025-04-01" to "Hari Raya Idulfitri 1446 H",
            "2025-05-12" to "Hari Raya Waisak 2569 BE",
            "2025-06-06" to "Hari Raya Iduladha 1446 H",
            "2025-06-27" to "Tahun Baru Islam 1447 H",
            "2025-09-05" to "Maulid Nabi Muhammad SAW",
        ),
        2026 to listOf(
            "2026-01-16" to "Isra Mikraj Nabi Muhammad SAW",
            "2026-02-17" to "Tahun Baru Imlek 2577",
            "2026-03-19" to "Hari Suci Nyepi (Tahun Baru Saka 1948)",
            "2026-03-20" to "Hari Raya Idulfitri 1447 H",
            "2026-03-21" to "Hari Raya Idulfitri 1447 H",
            "2026-05-27" to "Hari Raya Iduladha 1447 H",
            "2026-06-01" to "Hari Raya Waisak 2570 BE",
            "2026-06-16" to "Tahun Baru Islam 1448 H",
            "2026-08-25" to "Maulid Nabi Muhammad SAW",
        ),
        2027 to listOf(
            "2027-01-06" to "Isra Mikraj Nabi Muhammad SAW",
            "2027-02-06" to "Tahun Baru Imlek 2578",
            "2027-03-09" to "Hari Suci Nyepi (Tahun Baru Saka 1949)",
            "2027-03-10" to "Hari Raya Idulfitri 1448 H",
            "2027-03-11" to "Hari Raya Idulfitri 1448 H",
            "2027-05-17" to "Hari Raya Iduladha 1448 H",
            "2027-05-21" to "Hari Raya Waisak 2571 BE",
            "2027-06-06" to "Tahun Baru Islam 1449 H",
            "2027-08-15" to "Maulid Nabi Muhammad SAW",
        ),
    )
}
