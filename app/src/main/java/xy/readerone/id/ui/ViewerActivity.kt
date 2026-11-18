package xy.readerone.id.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xy.readerone.id.databinding.ActivityViewerBinding
import xy.readerone.id.engine.UniversalParser
import java.util.Locale

class ViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewerBinding
    private var pdfRenderer: PdfRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri: Uri = intent.data ?: run { finish(); return }
        val title = intent.getStringExtra("doc_name") ?: uri.lastPathSegment ?: "Dokumen"
        val ext = (intent.getStringExtra("doc_ext") ?: title.substringAfterLast('.', "txt"))
            .lowercase(Locale.ROOT)

        binding.tvViewerTitle.text = title
        binding.btnBack.setOnClickListener { finish() }
        binding.btnShare.setOnClickListener {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Bagikan Dokumen"))
        }

        renderByExtension(uri, ext)
    }

    private fun renderByExtension(uri: Uri, ext: String) {
        when (ext) {
            "pdf" -> renderPdf(uri)
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> {
                binding.ivImagePreview.visibility = View.VISIBLE
                binding.ivImagePreview.setImageURI(uri)
            }
            "mp4", "mkv", "webm", "3gp", "mp3", "wav", "ogg", "m4a" -> {
                binding.videoView.visibility = View.VISIBLE
                val controller = MediaController(this)
                controller.setAnchorView(binding.videoView)
                binding.videoView.setMediaController(controller)
                binding.videoView.setVideoURI(uri)
                binding.videoView.start()
            }
            else -> renderWithWebView(uri, ext)
        }
    }

    private fun renderPdf(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.rvPdfPages.visibility = View.VISIBLE
            binding.rvPdfPages.layoutManager = LinearLayoutManager(this)
            lifecycleScope.launch(Dispatchers.IO) {
                val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                pdfRenderer = PdfRenderer(pfd)
                val count = pdfRenderer?.pageCount ?: 0
                withContext(Dispatchers.Main) {
                    binding.rvPdfPages.adapter = PdfPageAdapter(pdfRenderer!!, count)
                }
            }
        } else {
            // Fallback Android 4.4 KitKat
            binding.webView.visibility = View.VISIBLE
            binding.webView.loadData(
                "<html><body style='font-family:sans-serif;padding:24px;'>" +
                    "<h3>Mode Kompatibilitas KitKat (API 19)</h3>" +
                    "<p>Gunakan tab <b>Konverter</b> untuk mengekstrak teks atau mengonversi PDF di Android 4.4.</p>" +
                    "</body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    private fun renderWithWebView(uri: Uri, ext: String) {
        binding.webView.visibility = View.VISIBLE
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        lifecycleScope.launch(Dispatchers.IO) {
            val html = UniversalParser.toEditorialHtml(this@ViewerActivity, uri, ext, isDark)
            withContext(Dispatchers.Main) {
                binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pdfRenderer?.close()
        }
    }

    private class PdfPageAdapter(
        private val renderer: PdfRenderer,
        private val count: Int
    ) : RecyclerView.Adapter<PdfPageAdapter.PageVH>() {

        class PageVH(val iv: ImageView) : RecyclerView.ViewHolder(iv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                setPadding(16, 16, 16, 16)
            }
            return PageVH(iv)
        }

        override fun getItemCount() = count

        override fun onBindViewHolder(holder: PageVH, position: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                renderer.openPage(position).use { page ->
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    holder.iv.setImageBitmap(bmp)
                }
            }
        }
    }
}
