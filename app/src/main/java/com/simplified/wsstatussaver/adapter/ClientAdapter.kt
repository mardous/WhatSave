/*
 * Copyright (C) 2024 Christians Martínez Alvarado
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
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.interfaces.IClientCallback
import com.simplified.wsstatussaver.model.WaClient

class ClientAdapter(
    private val context: Context,
    private val itemLayoutRes: Int,
    private val callback: IClientCallback
) :
    RecyclerView.Adapter<ClientAdapter.ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)
    private var clients: List<WaClient> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(itemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val client = clients[position]
        holder.icon?.setImageDrawable(client.getIcon(context))
        holder.name?.text = client.displayName
        configureCheckIcon(holder, client)
    }

    private fun configureCheckIcon(holder: ViewHolder, client: WaClient) {
        val checkMode = callback.checkModeForClient(client)
        if (itemCount == 1) {
            holder.check?.isChecked = true
            holder.check?.isEnabled = false
            holder.itemView.isEnabled = false
        } else {
            holder.check?.isChecked = checkMode == IClientCallback.MODE_CHECKED
        }
    }

    override fun getItemCount(): Int = clients.size

    @SuppressLint("NotifyDataSetChanged")
    fun setClients(clients: List<WaClient>) {
        this.clients = clients
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var icon: ImageView? = itemView.findViewById(R.id.icon)
        var name: TextView? = itemView.findViewById(R.id.name)
        var check: CompoundButton? = itemView.findViewById(R.id.check)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val currentClient = clients[layoutPosition]
            callback.clientClick(currentClient)
        }
    }
}