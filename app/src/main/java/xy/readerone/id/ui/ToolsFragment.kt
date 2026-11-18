package xy.readerone.id.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xy.readerone.id.databinding.FragmentToolsBinding
import xy.readerone.id.engine.DocumentConverter
import xy.readerone.id.engine.FileRepository
import xy.readerone.id.engine.UniversalParser
import java.io.File

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!
    private var pendingAction: String = ""

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runConversion(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.cardImgToPdf.setOnClickListener {
            pendingAction = "IMG_PDF"
            pickFile.launch(arrayOf("image/*"))
        }
        binding.cardDocToPdf.setOnClickListener {
            pendingAction = "DOC_PDF"
            pickFile.launch(arrayOf("*/*"))
        }
        binding.cardPdfToImages.setOnClickListener {
            pendingAction = "PDF_PNG"
            pickFile.launch(arrayOf("application/pdf"))
        }
        binding.cardDocxToTxt.setOnClickListener {
            pendingAction = "DOC_TXT"
            pickFile.launch(arrayOf("*/*"))
        }
    }

    private fun runConversion(uri: Uri) {
        val repo = FileRepository(requireContext())
        val baseName = resolveName(uri).substringBeforeLast('.')
        binding.tvToolStatus.text = "Memproses konversi secara offline…"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val message = try {
                when (pendingAction) {
                    "IMG_PDF" -> {
                        val out = File(repo.workspaceDir, "${baseName}.pdf")
                        DocumentConverter.imageToPdf(requireContext(), uri, out)
                        "Berhasil: ${out.name} tersimpan di Pustaka."
                    }
                    "DOC_PDF" -> {
                        val ext = resolveName(uri).substringAfterLast('.', "txt")
                        val text = UniversalParser.extractPlainText(requireContext(), uri, ext)
                        val out = File(repo.workspaceDir, "${baseName}.pdf")
                        DocumentConverter.textToPdf(text, out)
                        "Berhasil: ${out.name} tersimpan di Pustaka."
                    }
                    "PDF_PNG" -> {
                        val pages = DocumentConverter.pdfToPngPages(requireContext(), uri, repo.workspaceDir, baseName)
                        "Berhasil mengekspor ${pages.size} halaman PNG ke Pustaka."
                    }
                    "DOC_TXT" -> {
                        val ext = resolveName(uri).substringAfterLast('.', "docx")
                        val text = UniversalParser.extractPlainText(requireContext(), uri, ext)
                        val out = File(repo.workspaceDir, "${baseName}.txt")
                        out.writeText(text, Charsets.UTF_8)
                        "Berhasil: ${out.name} tersimpan di Pustaka."
                    }
                    else -> "Aksi tidak dikenali."
                }
            } catch (e: Exception) {
                "Gagal: ${e.message}"
            }
            withContext(Dispatchers.Main) {
                binding.tvToolStatus.text = message
            }
        }
    }

    private fun resolveName(uri: Uri): String {
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx) ?: "dokumen"
            }
        }
        return uri.lastPathSegment ?: "dokumen"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
