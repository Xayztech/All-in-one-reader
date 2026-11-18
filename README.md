# All In One Reader (`xy.readerone.id`) — v0.0.1

Aplikasi pembaca, editor, konverter, dan pengelola dokumen **100% offline, tanpa iklan, ringan, dan mulus** untuk Android **4.4 KitKat (API 19)** hingga Android terbaru (**API 35**).
 
## Fitur Utama
- **Tanpa Internet & Tanpa Iklan:** Tidak menggunakan izin `INTERNET` sama sekali di `AndroidManifest.xml`.
- **Universal Reader (Multi-Format):**
  - **PDF:** Render cepat dengan `PdfRenderer` (API 21+) dan fallback kompatibel KitKat.
  - **Dokumen Office (`.docx`, `.xlsx`, `.pptx`, `.odt`, `.epub`):** Parser OOXML/ZIP native ringan (`ZipInputStream` + `XmlPullParser`) tanpa library berat puluhan MB.
  - **Teks & Web (`.txt`, `.md`, `.csv`, `.json`, `.xml`, `.html`):** Render tipografi bersih dengan tabel otomatis untuk CSV.
  - **Media (`.jpg`, `.png`, `.webp`, `.gif`, `.mp4`, `.mkv`, `.mp3`, `.wav`):** Penampil gambar dan pemutar video/audio terintegrasi.
- **Konverter Offline:** Gambar ke PDF, Teks/Markdown/HTML/DOCX ke PDF (menggunakan `android.graphics.pdf.PdfDocument` API 19+), serta ekspor DOCX/HTML ke TXT.
- **Editor & Pembuat Dokumen:** Buat atau edit catatan, Markdown, teks, lalu simpan atau ekspor langsung ke PDF.
- **Desain Editorial Minimalis:** Palet *Warm Paper & Ink* dengan Mode Terang & Mode Gelap, tanpa elemen dekoratif berlebihan.

## Cara Build menjadi File `.apk`
1. Buka folder `AllInOneReader` ini menggunakan **Android Studio** (Jellyfish / Koala / Ladybug atau terbaru).
2. Tunggu proses *Gradle Sync* selesai.
3. Untuk langsung mencoba APK: klik **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Atau via terminal di root proyek:
   ```bash
   ./gradlew assembleRelease
   ```
   *(Konfigurasikan keystore di `app/build.gradle.kts` jika ingin menandatangani release APK, atau gunakan `./gradlew assembleDebug` untuk APK debug siap instal).*
