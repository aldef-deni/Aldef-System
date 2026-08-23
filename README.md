# Aldef System

Aplikasi Android pribadi berbasis Kotlin + Jetpack Compose dengan enam modul:
pemindai QRIS, kompas, kalkulator (sekaligus pintu brankas berkas), speedometer
GPS, kalender libur nasional, dan **ALDEF AI** (asisten kontrol suara).

Dioptimalkan untuk **Realme 5 Pro (Android 11 / ColorOS 11.1, Snapdragon 712)**.

## Akses

| Hal | Nilai |
| --- | --- |
| Nama pengguna | `aldef` |
| Kata sandi | `deniretna` |
| PIN brankas (bawaan) | `1974` |

Tombol biometrik ada di bawah tombol **MASUK**. Ketukan pertama menautkan sidik
jari perangkat ke aplikasi; ketukan berikutnya langsung membuka akses tanpa
mengetik. Kalau perangkat belum punya sidik jari terdaftar, aplikasi membuka
layar pendaftaran biometrik Android.

## Cara masuk ke brankas

Brankas **tidak punya tombol di mana pun**. Buka modul **Kalkulator**, ketik PIN
brankas, lalu tekan `=`. Layar akan berpindah ke brankas alih-alih menghitung.

Ganti PIN bawaan lewat ikon perisai di dalam brankas. Mengganti PIN akan
mengenkripsi ulang seluruh isi brankas.

## Modul

**QRIS** — CameraX + ML Kit membaca kode QR, lalu muatannya diurai sebagai QR
EMVCo: nama merchant, kota, PAN, ID merchant, kriteria usaha, nominal, mata
uang, terminal, dan validasi CRC-16. Kode QR biasa tetap ditampilkan apa adanya.
Senter bisa dinyalakan dari layar pemindai.

**Kompas** — memakai sensor rotation-vector (jatuh ke akselerometer +
magnetometer bila tidak ada), dengan penapis lolos-rendah supaya jarum tidak
bergetar. Kalau izin lokasi diberikan, arah dikoreksi ke utara sejati memakai
deklinasi magnetik setempat dan penanda arah kiblat ikut muncul.

**Kalkulator** — parser turun-rekursif di atas `BigDecimal`, jadi `0.1 + 0.2`
menghasilkan `0.3`, bukan `0.30000000000000004`. Mendukung kurung, persen, dan
pratinjau hasil saat mengetik.

**Speedometer** — kecepatan dibaca dari GPS (bukan diturunkan dari selisih
posisi), lengkap dengan kecepatan tertinggi, rata-rata, jarak tempuh,
ketinggian, dan arah. Satuan km/j ↔ mph bisa ditukar dan pilihannya tersimpan.
Saat dibuka, jarum menyapu penuh ("self-test") dengan pin skala menyala biru.

**Kalender** — carousel bulan (bulan tengah besar, geser kiri/kanan), default di
bulan & tanggal sekarang. Hari Minggu dan **libur nasional Indonesia** ditandai
merah; hari libur diberi titik + didaftar di bawah grid. Sumber tanggal: tetap
(selalu akurat) + Kristen (dihitung dari Paskah, akurat tiap tahun) + lunar
(tabel 2025–2027 di `data/Holidays.kt`, sinkronkan dengan SKB resmi). Notifikasi
**H-1** muncul sehari sebelum tanggal merah (alarm harian pukul 19.00).

**Brankas** — dua tab: **Berkas** dan **Aplikasi**.

*Berkas* dienkripsi AES-256-GCM dengan kunci turunan PBKDF2 (120.000 iterasi)
dari PIN, lalu disimpan di direktori privat aplikasi sehingga tidak muncul di
galeri maupun pengelola berkas. PIN tidak pernah disimpan apa adanya — hanya
salt acak dan hash SHA-256-nya. Berkas ditambahkan lewat Storage Access
Framework, jadi aplikasi tidak butuh izin penyimpanan luas sama sekali.

*Aplikasi* — kunci & sembunyikan aplikasi terpasang. Dua mekanisme, karena
Android membatasi apa yang boleh dilakukan APK biasa:

- **Kunci (Akses Penggunaan):** aplikasi terkunci **tetap muncul di drawer**,
  tapi saat dibuka ditutupi layar palsu "Sistem Gagal — aplikasi rusak" lalu
  ditendang ke home. Deteksi aplikasi-depan memakai izin *Akses Penggunaan*
  (UsageStatsManager) lewat sebuah layanan latar depan — **sengaja bukan
  AccessibilityService**, karena Accessibility memicu blokir keras Google Play
  Protect saat install. Butuh izin *Akses Penggunaan* + *Tampilkan di atas
  aplikasi lain* (dipandu dari panel status di dalam tab). Uninstall Aldef
  System sendiri bisa dikunci lewat Device Admin.
- **Sembunyikan total & blokir uninstall (Device Owner):** aplikasi benar-benar
  hilang dari drawer & pencarian, dan uninstall aplikasi lain bisa diblokir.
  Butuh status Device Owner, dipasang sekali lewat ADB dari komputer:
  `adb shell dpm set-device-owner com.aldef.system/.applock.AldefDeviceAdminReceiver`
  (perangkat mungkin harus tanpa akun Google dulu — kadang perlu factory reset).
  Selama belum Device Owner, tombol Sembunyikan & Cegah-hapus nonaktif; fitur
  Kunci tetap jalan.

Dari dalam brankas, aplikasi terkunci/tersembunyi bisa dibuka normal (tombol
**Buka**) — kunci dilewati sesaat, dan aplikasi tersembunyi ditampilkan lalu
disembunyikan lagi saat Anda kembali ke brankas.

## ALDEF AI — kontrol suara

Asisten suara **lokal-first**: tanpa backend, tanpa API AI eksternal, tanpa root,
tanpa Accessibility, tanpa hidden API — hanya API resmi Android.

**Cara pakai:** aktifkan di kartu **ALDEF AI** → jalani **Panduan Penyiapan**
(izin mikrofon, overlay, baterai/autostart, tes) → sebuah **strip tipis** muncul
di tepi layar (kiri/kanan, bisa diatur), tersedia di Home & aplikasi lain. Geser
strip → **panel** slide masuk → ketuk **mikrofon ALDEFTECH** → ucapkan perintah.

**Pipeline:** Suara → `SpeechRecognizer` (id-ID) → teks → `IntentEngine` (aturan
lokal, natural language) → `ActionExecutor` (whitelist) → balasan `TextToSpeech`.

**Perintah yang dipahami** (kalimat bebas, bukan string persis — mis. "tolong
buka kamera", "saya ingin ambil foto"):

| Ucapan | Aksi |
| --- | --- |
| buka kamera | membuka kamera |
| buka galeri | membuka **Galeri bawaan Realme** (`com.coloros.gallery3d`), bukan Google Photos |
| buka browser | membuka browser |
| buka `<nama aplikasi>` | membuka **aplikasi terpasang apa pun** (mis. "buka whatsapp") — **kecuali aplikasi yang di-hide** di brankas |
| buka qris / kompas / kalkulator / speedometer / kalender | membuka **fitur Aldef System** |
| buka pengaturan | membuka Setelan sistem |
| kembali | menutup panel ALDEF AI |
| volume naik / turun | mengatur volume (AudioManager) |
| nyalakan / matikan bluetooth | membuka Setelan Bluetooth |
| jam berapa sekarang | **menyebutkan** jam saat ini |
| hari ini hari apa | **menyebutkan** nama hari |
| tanggal berapa hari ini | **menyebutkan** tanggal lengkap |
| lokasi saat ini | **menyebutkan** lokasi (reverse-geocode GPS) |
| cek cuaca hari ini | **menyebutkan** suhu & kondisi cuaca (Open-Meteo) |
| set alarm jam 6 pagi | **menyetel alarm langsung** (tanpa buka UI) via `AlarmClock` |

Perintah info (jam, hari, tanggal, lokasi, cuaca) **diucapkan langsung** oleh
ALDEF AI tanpa membuka aplikasi. Brankas sengaja **tidak** dapat dibuka via suara
demi menjaga kerahasiaannya.

**Continuous Listening** (opsional, default OFF): setelah menjawab, otomatis
mendengar lagi; jeda-aman setelah 3 siklus sepi agar mic tak menyala selamanya.

**Arsitektur** (`app/.../aldefai/`): `core` (prefs, optimasi perangkat) ·
`voice` (recognizer + state) · `intent` (engine aturan + `IntentRouter` yang
siap disambung AI on-device) · `action` (executor whitelist) · `tts` ·
`overlay` (strip + panel Compose) · `service` (FGS) · `ui` (settings + wizard).

**Batas resmi (bukan bug):**

- **"Kembali"** hanya menutup panel — menekan tombol Back sistem di aplikasi lain
  butuh Accessibility, yang sengaja tidak dipakai.
- **Bluetooth** dibuka lewat Setelan — `BluetoothAdapter.enable()` jadi no-op
  sejak targetSdk 33+.
- **Cuaca** memakai **Open-Meteo** (gratis, tanpa API key, bukan server sendiri)
  via GPS + internet — satu-satunya cara menyebutkan cuaca nyata tanpa membuka
  aplikasi. Jam, hari, dan tanggal murni lokal (tanpa internet). Lokasi memakai
  Geocoder bawaan Android.
- **Autostart ColorOS** tak punya API resmi — wizard hanya membuka halamannya,
  aktifkan manual + kunci di Recents agar strip bertahan setelah idle lama.
- Pengenalan suara id-ID umumnya **butuh internet** (mesin Google).

Semua fitur AI berbasis overlay `SYSTEM_ALERT_WINDOW` + foreground service
bertipe `specialUse|microphone`; tidak ada polling CPU, animasi digambar Canvas
ringan demi Snapdragon 712.

## Aset merek

- Splash & header: `Aldef Logo/aldef-landscape02.png` → `res/drawable-nodpi/aldef_logo_landscape.png`
- Ikon peluncur: `Aldef Logo/launcher-logo.png` → `res/mipmap-*/ic_launcher*.png`

Ikon dibangkitkan ulang lewat skrip PowerShell + System.Drawing bila logo
sumbernya berubah.

## Build

Prasyarat: JDK 17+ (mesin ini memakai Temurin 21) dan Android SDK dengan
platform 35.

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\gradlew.bat :app:assembleRelease
```

APK keluar di `app/build/outputs/apk/release/app-release.apk`.

Toolchain: Gradle 8.9, AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01,
`compileSdk`/`targetSdk` 35, `minSdk` 26.

### Penandatanganan

Kredensial rilis dibaca dari `keystore.properties` di akar proyek (tidak
di-commit). Kalau berkas itu tidak ada, build release tetap jalan tapi APK-nya
tidak ditandatangani — kegagalan penandatanganan tidak pernah diam-diam.

```properties
storeFile=keystore/aldef-release.jks
storePassword=...
keyAlias=aldef
keyPassword=...
```

### Catatan

- R8/minify sengaja dimatikan: ML Kit bersandar pada refleksi dan pengecilan
  kode gampang memutusnya diam-diam.
- Pustaka native x86/x86_64 dibuang lewat `abiFilters`; hapus blok `ndk` di
  `app/build.gradle` kalau ingin menjalankannya di emulator x86.
