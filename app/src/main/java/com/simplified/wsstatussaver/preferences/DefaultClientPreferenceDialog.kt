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
package com.simplified.wsstatussaver.preferences

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.databinding.DialogRecyclerviewBinding
import com.simplified.wsstatussaver.databinding.ItemClientBinding
import com.simplified.wsstatussaver.mediator.WAClient
import com.simplified.wsstatussaver.mediator.WAMediator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class DefaultClientPreferenceDialog : DialogFragment(), OnShowListener {

    private var _binding: DialogRecyclerviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatSaveViewModel by activityViewModel()
    private val mediator: WAMediator by inject()

    private lateinit var clientAdapter: Adapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogRecyclerviewBinding.inflate(layoutInflater)
        binding.empty.setText(R.string.installed_clients_empty)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = Adapter(binding.root.context, mediator).apply {
            registerAdapterDataObserver(adapterDataObserver)
        }.also {
            clientAdapter = it
        }

        viewModel.getInstalledClients().observe(this) { installedClients ->
            clientAdapter.setClients(installedClients)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.default_client_title)
            .setView(binding.root)
            .setNegativeButton(R.string.close_action, null)
            .create().also {
                it.setOnShowListener(this)
            }
    }

    private val adapterDataObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            binding.empty.isVisible = clientAdapter.itemCount == 0
        }
    }

    override fun onShow(dialogInterface: DialogInterface) {
        viewModel.loadClients()
    }

    override fun onDismiss(dialog: DialogInterface) {
        clientAdapter.unregisterAdapterDataObserver(adapterDataObserver)
        super.onDismiss(dialog)
    }

    private class Adapter(private val context: Context, private val mediator: WAMediator) :
        RecyclerView.Adapter<Adapter.ViewHolder>() {

        private val layoutInflater = LayoutInflater.from(context)

        private var clients: List<WAClient> = ArrayList()
        private var defaultClient: WAClient? by Delegates.observable(mediator.getDefaultClient()) { _: KProperty<*>, _: WAClient?, client: WAClient? ->
            mediator.setDefaultClient(client)
            if (client == null) {
                Toast.makeText(context, R.string.default_client_cleared, Toast.LENGTH_SHORT).show()
            } else Toast.makeText(
                context,
                context.getString(R.string.x_is_the_default_client_now, client.getLabel(context)),
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemClientBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val client = clients[position]
            holder.icon?.setImageDrawable(client.getIcon(context))
            holder.name?.text = client.getLabel(context)
            holder.description?.text = client.getDescription(context)
            holder.check?.isChecked = client == defaultClient
        }

        override fun getItemCount(): Int = clients.size

        fun setClients(clients: List<WAClient>) {
            this.clients = clients
            notifyDataSetChanged()
        }

        inner class ViewHolder(binding: ItemClientBinding) : RecyclerView.ViewHolder(binding.root),
            View.OnClickListener {
            var icon: ImageView? = binding.icon
            var name: TextView? = binding.name
            var description: TextView? = binding.description
            var check: RadioButton? = binding.check

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(view: View?) {
                val currentClient = clients[layoutPosition]
                defaultClient = if (currentClient == defaultClient) null else currentClient
                notifyDataSetChanged()
            }
        }
    }
}