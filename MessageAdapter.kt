package com.example.nearbychat.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nearbychat.R
import com.example.nearbychat.model.Message

class MessageAdapter(
    private val data: MutableList<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val IN = 0
        const val OUT = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (data[position].mine) OUT else IN

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == OUT) {
            val v = inflater.inflate(R.layout.item_message_out, parent, false)
            Vh(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_in, parent, false)
            Vh(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as Vh).bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    fun add(msg: Message) {
        data.add(msg)
        notifyItemInserted(data.size - 1)
    }

    class Vh(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: TextView = itemView.findViewById(R.id.tvText)
        fun bind(m: Message) { tv.text = m.text }
    }
}