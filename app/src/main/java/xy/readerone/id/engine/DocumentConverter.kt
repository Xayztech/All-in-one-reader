package xy.readerone.id.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream

/**
 * Konverter 100% offline berbasis API bawaan Android (kompatibel sejak KitKat API 19).
 */
object DocumentConverter {

    fun textToPdf(text: String, outputFile: File): File {
        val pdf = PdfDocument()
        val pageWidth = 595 // A4 72dpi
        val pageHeight = 842
        val margin = 48f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11.5f
        }

        val lines = wrapText(text, paint, pageWidth - margin * 2)
        val lineHeight = 17f
        val linesPerPage = ((pageHeight - margin * 2) / lineHeight).toInt().coerceAtLeast(1)

        var pageNum = 1
        var index = 0
        while (index < lines.size || pageNum == 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            var y = margin + 12f
            val end = (index + linesPerPage).coerceAtMost(lines.size)
            for (i in index until end) {
                canvas.drawText(lines[i], margin, y, paint)
                y += lineHeight
            }
            pdf.finishPage(page)
            index = end
            if (index >= lines.size) break
        }

        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close()
        return outputFile
    }

    fun imageToPdf(context: Context, imageUri: Uri, outputFile: File): File {
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalStateException("Gagal membaca gambar")

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdf.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdf.finishPage(page)
        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close()
        bitmap.recycle()
        return outputFile
    }

    fun pdfToPngPages(context: Context, pdfUri: Uri, outputDir: File, prefix: String): List<File> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw UnsupportedOperationException("Ekspor PDF ke PNG membutuhkan Android 5.0 (Lollipop) ke atas.")
        }
        val outFiles = mutableListOf<File>()
        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val outFile = File(outputDir, "${prefix}_hal_${i + 1}.png")
                        FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                        bmp.recycle()
                        outFiles.add(outFile)
                    }
                }
            }
        }
        return outFiles
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.lines().forEach { rawLine ->
            if (rawLine.isEmpty()) {
                result.add("")
            } else {
                var current = ""
                rawLine.split(" ").forEach { word ->
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        current = candidate
                    } else {
                        if (current.isNotEmpty()) result.add(current)
                        current = word
                    }
                }
                if (current.isNotEmpty()) result.add(current)
            }
        }
        return result
    }
}
