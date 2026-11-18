package xy.readerone.id.engine

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Parser 100% offline & sangat ringan untuk DOCX, XLSX, PPTX, ODT, EPUB, MD, CSV, TXT, JSON, XML, HTML.
 * Menggunakan ZipInputStream + XmlPullParser bawaan Android agar ukuran APK tetap sangat kecil.
 */
object UniversalParser {

    fun toEditorialHtml(context: Context, uri: Uri, extension: String, isDark: Boolean): String {
        val ext = extension.lowercase(Locale.ROOT)
        val bodyHtml = context.contentResolver.openInputStream(uri)?.use { input ->
            when (ext) {
                "docx" -> parseDocxToHtml(input)
                "xlsx" -> parseXlsxToHtml(input)
                "pptx" -> parsePptxToHtml(input)
                "odt" -> parseOdtToHtml(input)
                "epub" -> parseEpubToHtml(input)
                "md" -> markdownToHtml(readText(input))
                "csv" -> csvToHtml(readText(input))
                "html", "htm" -> return readText(input)
                else -> "<pre>${escapeHtml(readText(input))}</pre>"
            }
        } ?: "<p>Tidak dapat membaca file.</p>"

        val bg = if (isDark) "#121211" else "#FAF9F6"
        val fg = if (isDark) "#F4F4F0" else "#18181B"
        val border = if (isDark) "#2E2E2A" else "#E4E2DC"

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <style>
                body { font-family: -apple-system, sans-serif; background: $bg; color: $fg; line-height: 1.65; padding: 18px; margin: 0; font-size: 16px; }
                h1, h2, h3 { line-height: 1.3; margin-top: 1.2em; }
                p { margin: 0 0 12px 0; }
                pre { white-space: pre-wrap; word-wrap: break-word; font-family: monospace; font-size: 13.5px; }
                table { border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 14px; }
                th, td { border: 1px solid $border; padding: 8px 10px; text-align: left; }
                .slide-card { border: 1px solid $border; border-radius: 10px; padding: 14px; margin-bottom: 14px; }
              </style>
            </head>
            <body>$bodyHtml</body>
            </html>
        """.trimIndent()
    }

    fun extractPlainText(context: Context, uri: Uri, extension: String): String {
        val html = toEditorialHtml(context, uri, extension, false)
        return html.replace(Regex("<style[\\s\\S]*?</style>"), "")
            .replace(Regex("<[^>]+>"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun readText(input: InputStream): String =
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()

    private fun parseDocxToHtml(input: InputStream): String {
        val sb = StringBuilder()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val parser = Xml.newPullParser()
                    parser.setInput(zip, "UTF-8")
                    var event = parser.eventType
                    val paraText = StringBuilder()
                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> if (parser.name == "p") paraText.clear()
                            XmlPullParser.TEXT -> paraText.append(parser.text)
                            XmlPullParser.END_TAG -> if (parser.name == "p") {
                                val text = paraText.toString().trim()
                                if (text.isNotEmpty()) sb.append("<p>").append(escapeHtml(text)).append("</p>")
                            }
                        }
                        event = parser.next()
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }
        return sb.toString().ifEmpty { "<p>(Dokumen DOCX kosong)</p>" }
    }

    private fun parseOdtToHtml(input: InputStream): String {
        val sb = StringBuilder()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "content.xml") {
                    val parser = Xml.newPullParser()
                    parser.setInput(zip, "UTF-8")
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.TEXT && parser.text.isNotBlank()) {
                            sb.append("<p>").append(escapeHtml(parser.text)).append("</p>")
                        }
                        event = parser.next()
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }
        return sb.toString()
    }

    private fun parseXlsxToHtml(input: InputStream): String {
        val sharedStrings = mutableListOf<String>()
        val sheets = mutableMapOf<String, String>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val content = zip.readBytes().toString(Charsets.UTF_8)
                if (entry.name == "xl/sharedStrings.xml") {
                    Regex("<t[^>]*>(.*?)</t>").findAll(content).forEach {
                        sharedStrings.add(it.groupValues[1])
                    }
                } else if (entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml")) {
                    sheets[entry.name] = content
                }
                entry = zip.nextEntry
            }
        }
        val sb = StringBuilder()
        sheets.toSortedMap().forEach { (name, xml) ->
            sb.append("<h3>").append(name.substringAfterLast('/')).append("</h3><table>")
            Regex("<row[^>]*>(.*?)</row>").findAll(xml).forEach { rowMatch ->
                sb.append("<tr>")
                Regex("<c([^>]*)>.*?<v>(.*?)</v>.*?</c>").findAll(rowMatch.groupValues[1]).forEach { cell ->
                    val attrs = cell.groupValues[1]
                    val raw = cell.groupValues[2]
                    val value = if (attrs.contains("t=\"s\"")) {
                        sharedStrings.getOrNull(raw.toIntOrNull() ?: -1) ?: raw
                    } else raw
                    sb.append("<td>").append(escapeHtml(value)).append("</td>")
                }
                sb.append("</tr>")
            }
            sb.append("</table>")
        }
        return sb.toString().ifEmpty { "<p>(Spreadsheet kosong)</p>" }
    }

    private fun parsePptxToHtml(input: InputStream): String {
        val slides = mutableMapOf<String, String>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                    slides[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        val sb = StringBuilder()
        slides.toSortedMap().entries.forEachIndexed { idx, (_, xml) ->
            sb.append("<div class='slide-card'><strong>Slide ${idx + 1}</strong><br/>")
            Regex("<a:t>(.*?)</a:t>").findAll(xml).forEach {
                sb.append("<p>").append(escapeHtml(it.groupValues[1])).append("</p>")
            }
            sb.append("</div>")
        }
        return sb.toString().ifEmpty { "<p>(Presentasi kosong)</p>" }
    }

    private fun parseEpubToHtml(input: InputStream): String {
        val sb = StringBuilder()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".html") || entry.name.endsWith(".xhtml")) {
                    val raw = zip.readBytes().toString(Charsets.UTF_8)
                    val body = Regex("<body[^>]*>([\\s\\S]*?)</body>").find(raw)?.groupValues?.get(1) ?: raw
                    sb.append(body).append("<hr/>")
                }
                entry = zip.nextEntry
            }
        }
        return sb.toString().ifEmpty { "<p>(EPUB tidak berisi teks HTML)</p>" }
    }

    private fun markdownToHtml(md: String): String {
        return md.lines().joinToString("\n") { line ->
            when {
                line.startsWith("### ") -> "<h3>${escapeHtml(line.drop(4))}</h3>"
                line.startsWith("## ") -> "<h2>${escapeHtml(line.drop(3))}</h2>"
                line.startsWith("# ") -> "<h1>${escapeHtml(line.drop(2))}</h1>"
                line.startsWith("- ") -> "<p>• ${escapeHtml(line.drop(2))}</p>"
                line.isBlank() -> ""
                else -> "<p>${escapeHtml(line)}</p>"
            }
        }
    }

    private fun csvToHtml(csv: String): String {
        val rows = csv.lines().filter { it.isNotBlank() }
        val sb = StringBuilder("<table>")
        rows.forEachIndexed { idx, row ->
            val tag = if (idx == 0) "th" else "td"
            sb.append("<tr>")
            row.split(',').forEach { cell ->
                sb.append("<$tag>").append(escapeHtml(cell.trim())).append("</$tag>")
            }
            sb.append("</tr>")
        }
        sb.append("</table>")
        return sb.toString()
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}