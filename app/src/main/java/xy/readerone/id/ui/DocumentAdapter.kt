package xy.readerone.id.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xy.readerone.id.databinding.ItemDocumentBinding
import xy.readerone.id.model.DocItem

class DocumentAdapter(
    private val onClick: (DocItem) -> Unit,
    private val onMoreClick: (DocItem) -> Unit
) : ListAdapter<DocItem, DocumentAdapter.DocViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocViewHolder(private val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocItem) {
            binding.tvExtBadge.text = item.extension.take(4)
            binding.tvDocTitle.text = item.name
            binding.tvDocMeta.text = item.formattedMeta()
            binding.root.setOnClickListener { onClick(item) }
            binding.btnMore.setOnClickListener { onMoreClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DocItem>() {
            override fun areItemsTheSame(oldItem: DocItem, newItem: DocItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: DocItem, newItem: DocItem) = oldItem == newItem
        }
    }
}
