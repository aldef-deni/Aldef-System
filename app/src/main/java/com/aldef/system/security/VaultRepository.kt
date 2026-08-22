package com.aldef.system.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.crypto.SecretKey

/** Satu berkas di dalam brankas. */
data class VaultEntry(
    val id: String,
    val name: String,
    val mime: String,
    val size: Long,
    val addedAt: Long
)

/**
 * Brankas berkas terkunci.
 *
 * Berkas disimpan di direktori privat aplikasi ([Context.getFilesDir]) sehingga
 * tidak muncul di galeri atau pengelola berkas, dan isinya dienkripsi AES-GCM
 * dengan kunci turunan PIN. Tiap entri terdiri dari dua berkas:
 *  - `<id>.idx` metadata terenkripsi (nama asli, tipe, ukuran)
 *  - `<id>.bin` isi berkas terenkripsi
 */
class VaultRepository(private val context: Context) {

    private val dir: File = File(context.filesDir, ".vault").apply { mkdirs() }
    private val saltFile = File(dir, "vault.salt")

    /** Salt tetap per-perangkat; dibuat sekali saat brankas pertama dipakai. */
    private fun salt(): ByteArray {
        if (!saltFile.exists()) {
            saltFile.writeBytes(VaultCrypto.randomBytes(VaultCrypto.SALT_BYTES))
        }
        return saltFile.readBytes()
    }

    fun deriveKey(pin: String): SecretKey = VaultCrypto.deriveKey(pin, salt())

    /**
     * Membaca daftar isi brankas. Entri yang gagal didekripsi (PIN salah atau
     * berkas rusak) dilewati, bukan dianggap kosong secara diam-diam —
     * jumlahnya dilaporkan lewat [Listing.unreadable].
     */
    fun list(key: SecretKey): Listing {
        val entries = mutableListOf<VaultEntry>()
        var unreadable = 0
        dir.listFiles { f -> f.name.endsWith(".idx") }?.forEach { idx ->
            runCatching {
                val json = JSONObject(String(VaultCrypto.decrypt(key, idx.readBytes()), Charsets.UTF_8))
                VaultEntry(
                    id = idx.name.removeSuffix(".idx"),
                    name = json.getString("name"),
                    mime = json.optString("mime", "application/octet-stream"),
                    size = json.optLong("size"),
                    addedAt = json.optLong("addedAt")
                )
            }.onSuccess { entries += it }.onFailure { unreadable++ }
        }
        return Listing(entries.sortedByDescending { it.addedAt }, unreadable)
    }

    data class Listing(val entries: List<VaultEntry>, val unreadable: Int)

    /** Menarik berkas dari [uri] ke dalam brankas. */
    fun import(key: SecretKey, uri: Uri): Result<VaultEntry> = runCatching {
        val resolver = context.contentResolver
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "berkas"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                    ?.let { if (!c.isNull(it)) name = c.getString(it) }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }
                    ?.let { if (!c.isNull(it)) size = c.getLong(it) }
            }
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val id = UUID.randomUUID().toString()
        val blob = File(dir, "$id.bin")

        val written = resolver.openInputStream(uri)?.use { input ->
            blob.outputStream().use { output -> VaultCrypto.encryptStream(key, input, output) }
        } ?: run {
            blob.delete()
            error("Tidak bisa membuka $name")
        }

        val entry = VaultEntry(
            id = id,
            name = name,
            mime = mime,
            size = if (size > 0) size else written,
            addedAt = System.currentTimeMillis()
        )
        writeIndex(key, entry)
        entry
    }

    private fun writeIndex(key: SecretKey, entry: VaultEntry) {
        val json = JSONObject().apply {
            put("name", entry.name)
            put("mime", entry.mime)
            put("size", entry.size)
            put("addedAt", entry.addedAt)
        }
        File(dir, "${entry.id}.idx")
            .writeBytes(VaultCrypto.encrypt(key, json.toString().toByteArray(Charsets.UTF_8)))
    }

    /**
     * Mendekripsi satu entri ke cache supaya bisa dibuka aplikasi lain.
     * Cache dibersihkan tiap kali brankas dikunci, lihat [clearCache].
     */
    fun decryptToCache(key: SecretKey, entry: VaultEntry): Result<File> = runCatching {
        val outDir = File(context.cacheDir, "vault_open").apply { mkdirs() }
        val out = File(outDir, entry.name)
        File(dir, "${entry.id}.bin").inputStream().use { input ->
            out.outputStream().use { output -> VaultCrypto.decryptStream(key, input, output) }
        }
        out
    }

    /** Mengeluarkan entri ke lokasi pilihan pengguna. */
    fun exportTo(key: SecretKey, entry: VaultEntry, target: Uri): Result<Unit> = runCatching {
        File(dir, "${entry.id}.bin").inputStream().use { input ->
            context.contentResolver.openOutputStream(target)?.use { output ->
                VaultCrypto.decryptStream(key, input, output)
            } ?: error("Tujuan ekspor tidak bisa ditulis")
        }
    }

    fun rename(key: SecretKey, entry: VaultEntry, newName: String): VaultEntry {
        val updated = entry.copy(name = newName)
        writeIndex(key, updated)
        return updated
    }

    fun delete(entry: VaultEntry) {
        File(dir, "${entry.id}.bin").delete()
        File(dir, "${entry.id}.idx").delete()
    }

    fun clearCache() {
        File(context.cacheDir, "vault_open").deleteRecursively()
    }

    fun totalBytes(): Long =
        dir.listFiles { f -> f.name.endsWith(".bin") }?.sumOf { it.length() } ?: 0L
}
