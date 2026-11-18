package xy.readerone.id.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import xy.readerone.id.databinding.FragmentEditorBinding
import xy.readerone.id.engine.DocumentConverter
import xy.readerone.id.engine.FileRepository
import java.io.File

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = FileRepository(requireContext())

        binding.btnSaveMd.setOnClickListener {
            val name = binding.etDocName.text.toString().trim().ifEmpty { "Catatan_${System.currentTimeMillis()}" }
            val content = binding.etDocContent.text.toString()
            val outFile = File(repo.workspaceDir, "$name.md")
            outFile.writeText(content, Charsets.UTF_8)
            Toast.makeText(requireContext(), "Disimpan ke ${outFile.name}", Toast.LENGTH_SHORT).show()
        }

        binding.btnExportPdf.setOnClickListener {
            val name = binding.etDocName.text.toString().trim().ifEmpty { "Dokumen_${System.currentTimeMillis()}" }
            val content = binding.etDocContent.text.toString()
            val outFile = File(repo.workspaceDir, "$name.pdf")
            DocumentConverter.textToPdf(content, outFile)
            Toast.makeText(requireContext(), "PDF dibuat: ${outFile.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
