package com.neweyes.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neweyes.R
/*
class MessageAdapter(private val messageList: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        // Aquí es donde va la lógica que mencionaste:
        val container = holder.itemView.findViewById<LinearLayout>(R.id.message_container)
        val messageText = holder.itemView.findViewById<TextView>(R.id.messageText)

        messageText.text = message.text

        if (message.isUser) {
            container.gravity = Gravity.END
            messageText.setBackgroundResource(R.drawable.message_bg_user)
        } else {
            container.gravity = Gravity.START
            messageText.setBackgroundResource(R.drawable.message_bg_bot)
        }
    }

    override fun getItemCount(): Int = messageList.size
}
*/