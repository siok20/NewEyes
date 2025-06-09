package com.neweyes.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neweyes.databinding.ItemMessageOtherBinding
import com.neweyes.databinding.ItemMessageUserBinding

/**
 * Adapter para el RecyclerView del chat.
 * Soporta dos tipos de vistas: mensaje del usuario y mensaje de otro.
 */
class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_OTHER = 2
    }

    private val messages = mutableListOf<Message>()

    // Devuelve el tipo de vista según isUser
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            // Inflate para mensaje de usuario
            val binding = ItemMessageUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            UserMessageViewHolder(binding)
        } else {
            // Inflate para mensaje de otro usuario
            val binding = ItemMessageOtherBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            OtherMessageViewHolder(binding)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserMessageViewHolder) {
            holder.bind(message)
        } else if (holder is OtherMessageViewHolder) {
            holder.bind(message)
        }
    }

    /**
     * Añade un mensaje al final de la lista y notifica al RecyclerView.
     * Importante: debe ser llamado desde el hilo principal.
     */
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    /**
     * Limpia todos los mensajes (si en algún momento lo necesitas).
     */
    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }

    // ViewHolder para mensajes del usuario
    inner class UserMessageViewHolder(
        private val binding: ItemMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textMessageUser.text = message.text
        }
    }

    // ViewHolder para mensajes de otro usuario
    inner class OtherMessageViewHolder(
        private val binding: ItemMessageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textMessageOther.text = message.text
        }
    }
}