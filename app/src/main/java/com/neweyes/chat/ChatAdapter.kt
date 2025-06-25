package com.neweyes.chat

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neweyes.R
import com.neweyes.databinding.ItemMessageOtherBinding
import com.neweyes.databinding.ItemMessageUserBinding
import com.neweyes.databinding.ItemMessageImageUserBinding
import com.squareup.picasso.Picasso
import android.widget.ImageView
import com.neweyes.databinding.ItemMessageImageOtherBinding

/**
 * Adapter para el RecyclerView del chat.
 * Soporta dos tipos de vistas: mensaje del usuario y mensaje de otro.
 */
class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER_TEXT = 1
        private const val VIEW_TYPE_USER_IMAGE = 2
        private const val VIEW_TYPE_OTHER = 3
        private const val VIEW_TYPE_OTHER_IMAGE = 4
    }


    private val messages = mutableListOf<Message>()

    // Devuelve el tipo de vista según isUser
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isUser && message.imageUri != null -> VIEW_TYPE_USER_IMAGE
            message.isUser -> VIEW_TYPE_USER_TEXT
            !message.isUser && message.imageUri != null -> VIEW_TYPE_OTHER_IMAGE
            else -> VIEW_TYPE_OTHER
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER_TEXT -> {
                val binding = ItemMessageUserBinding.inflate(inflater, parent, false)
                UserTextMessageViewHolder(binding)
            }
            VIEW_TYPE_USER_IMAGE -> {
                val binding = ItemMessageImageUserBinding.inflate(inflater, parent, false)
                UserImageMessageViewHolder(binding)
            }
            VIEW_TYPE_OTHER_IMAGE -> {
                val binding = ItemMessageImageOtherBinding.inflate(inflater, parent, false)
                OtherImageMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageOtherBinding.inflate(inflater, parent, false)
                OtherMessageViewHolder(binding)
            }
        }
    }



    override fun getItemCount(): Int = messages.size

    private var lastPosition = -1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val previousUsername = messages.getOrNull(position - 1)?.userName
        val showUsername = previousUsername != message.userName

        when (holder) {
            is UserTextMessageViewHolder -> holder.bind(message)
            is UserImageMessageViewHolder -> holder.bind(message)
            is OtherMessageViewHolder -> holder.bind(message, message.userName ?: "ChatBot", showUsername)
            is OtherImageMessageViewHolder -> holder.bind(message, message.userName ?: "ChatBot", showUsername)
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

    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }


    // ViewHolder para mensajes del usuario
    inner class UserTextMessageViewHolder(
        private val binding: ItemMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.textMessageUser.text = message.text
        }
    }

    inner class UserImageMessageViewHolder(
        private val binding: ItemMessageImageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            Picasso.get()
                .load(Uri.parse(message.imageUri))
                .placeholder(R.drawable.placeholder_image)
                .into(binding.imageMessageUser)

            binding.imageMessageUser.setOnClickListener {
                val dialog = android.app.Dialog(binding.root.context)
                val fullImageView = ImageView(binding.root.context).apply {
                    setImageDrawable(binding.imageMessageUser.drawable)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(android.graphics.Color.BLACK) // Fondo negro para pantalla completa
                    setOnClickListener { dialog.dismiss() }
                }

                dialog.setContentView(fullImageView)
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                dialog.show()
            }
        }
    }


    // ViewHolder para mensajes de otro usuario
    inner class OtherMessageViewHolder(
        private val binding: ItemMessageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, username: String = "Usuario", showUsername: Boolean = true) {
            binding.textMessageOther.text = message.text
            binding.textUsername.text = username
            binding.textUsername.visibility = if (showUsername) View.VISIBLE else View.GONE
        }
    }

    inner class OtherImageMessageViewHolder(
        private val binding: ItemMessageImageOtherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, username: String = "Usuario", showUsername: Boolean = true) {
            binding.textUsername.text = username
            binding.textUsername.visibility = if (showUsername) View.VISIBLE else View.GONE

            Picasso.get()
                .load(Uri.parse(message.imageUri))
                .placeholder(R.drawable.placeholder_image)
                .into(binding.imageMessageOther)

            binding.imageMessageOther.setOnClickListener {
                val dialog = android.app.Dialog(binding.root.context)
                val fullImageView = ImageView(binding.root.context).apply {
                    setImageDrawable(binding.imageMessageOther.drawable)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setOnClickListener { dialog.dismiss() }
                }

                dialog.setContentView(fullImageView)
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                dialog.show()
            }
        }
    }

}