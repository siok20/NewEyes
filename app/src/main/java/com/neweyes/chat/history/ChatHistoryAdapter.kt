package com.neweyes.chat.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neweyes.databinding.ItemChatHistoryBinding

class ChatHistoryAdapter(
    private val onChatClick: (Long) -> Unit,
    private val onChatLongClick: ((Long) -> Unit)? = null
) : RecyclerView.Adapter<ChatHistoryAdapter.ChatViewHolder>() {

    private val items = mutableListOf<Chat>()

    fun submitList(newItems: List<Chat>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ChatViewHolder(private val binding: ItemChatHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: Chat) {
            binding.chatTitle.text = summary.title
            binding.chatTimestamp.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", summary.timestamp)
            binding.root.setOnClickListener { onChatClick(summary.id) }
            binding.root.setOnLongClickListener {
                onChatLongClick?.invoke(summary.id)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
