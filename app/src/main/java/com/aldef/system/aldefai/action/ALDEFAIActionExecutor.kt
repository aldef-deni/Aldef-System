package com.aldef.system.aldefai.action

import com.aldef.system.aldefai.intent.ALDEFAIIntent

/**
 * Hasil eksekusi sebuah [ALDEFAIIntent].
 *
 * @param speak kalimat balasan yang diucapkan ALDEF AI.
 * @param closePanel true jika panel sebaiknya ditutup setelah aksi (mis. saat
 *   membuka aplikasi lain agar overlay tidak menutupinya).
 */
data class ALDEFAIActionResult(
    val success: Boolean,
    val speak: String,
    val closePanel: Boolean = true
)

/**
 * Menjalankan maksud terstruktur. Implementasi memakai daftar-putih aksi —
 * tidak pernah menjalankan perintah bebas dari input suara.
 */
interface ALDEFAIActionExecutor {
    suspend fun execute(intent: ALDEFAIIntent): ALDEFAIActionResult
}
