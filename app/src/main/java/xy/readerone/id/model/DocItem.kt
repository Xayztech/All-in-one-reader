package xy.readerone.id.model

import android.net.Uri
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DocCategory {
    PDF, OFFICE_TEXT, MEDIA, OTHER
}

data class DocItem(
    val id: String,
    val name: String,
    val uriString: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long
) {
    val uri: Uri get() = Uri.parse(uriString)

    val category: DocCategory
        get() = when (extension.lowercase(Locale.ROOT)) {
            "pdf" -> DocCategory.PDF
            "docx", "doc", "xlsx", "pptx", "odt", "epub",
            "txt", "md", "csv", "json", "xml", "html", "htm", "rtf" -> DocCategory.OFFICE_TEXT
            "jpg", "jpeg", "png", "webp", "gif", "bmp",
            "mp4", "mkv", "webm", "3gp", "mp3", "wav", "ogg", "m4a" -> DocCategory.MEDIA
            else -> DocCategory.OTHER
        }

    fun formattedMeta(): String {
        val sizeStr = formatSize(sizeBytes)
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(lastModified))
        return "$sizeStr · $dateStr"
    }

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
                .coerceIn(0, units.lastIndex)
            return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
        }
    }
}
