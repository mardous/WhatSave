/*
 * Copyright (C) 2023 Christians Martínez Alvarado
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 * the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.simplified.wsstatussaver.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.database.MessageEntity
import com.simplified.wsstatussaver.extensions.time
import com.simplified.wsstatussaver.interfaces.IMessageCallback

class MessageAdapter(
    private val context: Context,
    private var messages: List<MessageEntity>,
    private val callback: IMessageCallback
) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.message.text = message.content
        holder.time.text = message.time.time(useTimeFormat = true)
    }

    override fun getItemCount(): Int = messages.size

    @SuppressLint("NotifyDataSetChanged")
    fun data(messages: List<MessageEntity>) {
        this.messages = messages
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener,
        View.OnLongClickListener {
        val message: TextView = itemView.findViewById(R.id.message)
        val time: TextView = itemView.findViewById(R.id.time)

        private val currentMessage: MessageEntity?
            get() = layoutPosition.let { position -> if (position > -1) messages[position] else null }

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            currentMessage?.let { callback.messageClick(it) }
        }

        override fun onLongClick(v: View?): Boolean {
            currentMessage?.let { callback.messageLongClick(it) }
            return true
        }
    }
}