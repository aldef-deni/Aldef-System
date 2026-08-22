package com.aldef.system.data

/**
 * Pembaca muatan QRIS (standar EMVCo Merchant-Presented QR).
 *
 * Muatannya berupa rangkaian TLV: dua digit tag, dua digit panjang, lalu nilai.
 * Beberapa tag isinya TLV lagi (informasi akun merchant dan data tambahan).
 */
object QrisParser {

    data class Field(val tag: String, val value: String)

    data class Account(
        val tag: String,
        val globalId: String?,
        val merchantPan: String?,
        val merchantId: String?,
        val criteria: String?
    )

    data class Qris(
        val raw: String,
        val version: String?,
        val isDynamic: Boolean,
        val merchantName: String?,
        val merchantCity: String?,
        val postalCode: String?,
        val countryCode: String?,
        val currencyCode: String?,
        val amount: String?,
        val tipIndicator: String?,
        val tipValue: String?,
        val merchantCategoryCode: String?,
        val accounts: List<Account>,
        val terminalLabel: String?,
        val referenceLabel: String?,
        val billNumber: String?,
        val crcValid: Boolean
    ) {
        val acquirer: String?
            get() = accounts.firstNotNullOfOrNull { it.globalId }

        val currencyLabel: String?
            get() = when (currencyCode) {
                "360" -> "IDR"
                "840" -> "USD"
                "702" -> "SGD"
                "458" -> "MYR"
                null -> null
                else -> currencyCode
            }

        /** Kategori merchant menurut kriteria QRIS (UMI/UKE/UME/UBE/URE). */
        val merchantCriteria: String?
            get() = accounts.firstNotNullOfOrNull { it.criteria }?.let {
                when (it.uppercase()) {
                    "UMI" -> "Usaha Mikro (UMI)"
                    "UKE" -> "Usaha Kecil (UKE)"
                    "UME" -> "Usaha Menengah (UME)"
                    "UBE" -> "Usaha Besar (UBE)"
                    "URE" -> "Usaha Regular (URE)"
                    else -> it
                }
            }
    }

    /** true kalau muatannya berbentuk QR EMVCo (diawali tag 00 versi "01"). */
    fun looksLikeQris(payload: String): Boolean =
        payload.length > 8 && payload.startsWith("0002") && payload.contains("5802")

    fun parse(payload: String): Qris? {
        val fields = parseTlv(payload) ?: return null
        fun value(tag: String) = fields.firstOrNull { it.tag == tag }?.value

        val accounts = fields
            .filter { it.tag.toIntOrNull() in 26..51 }
            .mapNotNull { field ->
                val inner = parseTlv(field.value) ?: return@mapNotNull null
                Account(
                    tag = field.tag,
                    globalId = inner.firstOrNull { it.tag == "00" }?.value,
                    merchantPan = inner.firstOrNull { it.tag == "01" }?.value,
                    merchantId = inner.firstOrNull { it.tag == "02" }?.value,
                    criteria = inner.firstOrNull { it.tag == "03" }?.value
                )
            }

        val additional = value("62")?.let { parseTlv(it) }.orEmpty()
        fun extra(tag: String) = additional.firstOrNull { it.tag == tag }?.value

        return Qris(
            raw = payload,
            version = value("00"),
            isDynamic = value("01") == "12",
            merchantName = value("59"),
            merchantCity = value("60"),
            postalCode = value("61"),
            countryCode = value("58"),
            currencyCode = value("53"),
            amount = value("54"),
            tipIndicator = value("55"),
            tipValue = value("56") ?: value("57"),
            merchantCategoryCode = value("52"),
            accounts = accounts,
            terminalLabel = extra("07"),
            referenceLabel = extra("05"),
            billNumber = extra("01"),
            crcValid = verifyCrc(payload)
        )
    }

    private fun parseTlv(data: String): List<Field>? {
        val out = mutableListOf<Field>()
        var i = 0
        while (i + 4 <= data.length) {
            val tag = data.substring(i, i + 2)
            val length = data.substring(i + 2, i + 4).toIntOrNull() ?: return null
            val start = i + 4
            val end = start + length
            if (end > data.length) return null
            out += Field(tag, data.substring(start, end))
            i = end
        }
        // Sisa byte yang tidak membentuk TLV utuh menandakan muatan bukan EMVCo.
        return if (i == data.length) out else null
    }

    /**
     * CRC-16/CCITT-FALSE atas seluruh muatan kecuali empat digit nilai CRC-nya
     * sendiri. Kalau tidak cocok, QR-nya rusak atau bukan QRIS.
     */
    private fun verifyCrc(payload: String): Boolean {
        val marker = payload.lastIndexOf("6304")
        if (marker < 0 || marker + 8 != payload.length) return false
        val expected = payload.substring(marker + 4)
        val computed = crc16(payload.substring(0, marker + 4))
        return expected.equals(computed, ignoreCase = true)
    }

    private fun crc16(input: String): String {
        var crc = 0xFFFF
        for (byte in input.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return "%04X".format(crc)
    }

    /** Memformat nilai rupiah "12345.67" menjadi "Rp 12.345,67". */
    fun formatAmount(amount: String?, currency: String?): String? {
        if (amount.isNullOrBlank()) return null
        val number = amount.toDoubleOrNull() ?: return amount
        val symbol = when (currency) {
            "IDR", "360", null -> "Rp "
            else -> "$currency "
        }
        val hasCents = number % 1.0 != 0.0
        val pattern = if (hasCents) "#,##0.00" else "#,##0"
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale("in", "ID"))
        return symbol + java.text.DecimalFormat(pattern, symbols).format(number)
    }
}
