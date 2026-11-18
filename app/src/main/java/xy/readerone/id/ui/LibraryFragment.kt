package xy.readerone.id.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import xy.readerone.id.R
import xy.readerone.id.databinding.FragmentLibraryBinding
import xy.readerone.id.engine.FileRepository
import xy.readerone.id.model.DocCategory
import xy.readerone.id.model.DocItem

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: FileRepository
    private lateinit var adapter: DocumentAdapter
    private var allItems: List<DocItem> = emptyList()
    private var selectedCategory: DocCategory? = null

    private val openDocLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            val item = repo.registerUri(uri)
            refreshList()
            openViewer(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = FileRepository(requireContext())
        adapter = DocumentAdapter(
            onClick = { openViewer(it) },
            onMoreClick = { showManageDialog(it) }
        )

        binding.rvDocuments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDocuments.adapter = adapter

        binding.btnPickFile.setOnClickListener {
            openDocLauncher.launch(arrayOf("*/*"))
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.chipFilterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chipPdf -> DocCategory.PDF
                R.id.chipDocs -> DocCategory.OFFICE_TEXT
                R.id.chipMedia -> DocCategory.MEDIA
                else -> null
            }
            applyFilters()
        }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        allItems = repo.listDocuments()
        applyFilters()
    }

    private fun applyFilters() {
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        val filtered = allItems.filter { item ->
            val matchCat = selectedCategory == null || item.category == selectedCategory
            val matchQuery = query.isEmpty() || item.name.contains(query, ignoreCase = true)
            matchCat && matchQuery
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openViewer(item: DocItem) {
        val intent = Intent(requireContext(), ViewerActivity::class.java).apply {
            data = item.uri
            putExtra("doc_name", item.name)
            putExtra("doc_ext", item.extension)
        }
        startActivity(intent)
    }

    private fun showManageDialog(item: DocItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setItems(arrayOf("Buka Dokumen", "Hapus dari Pustaka")) { _, which ->
                when (which) {
                    0 -> openViewer(item)
                    1 -> {
                        repo.removeItem(item)
                        refreshList()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
