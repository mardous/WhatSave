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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.databinding.DialogRecyclerviewBinding
import com.simplified.wsstatussaver.databinding.ItemStorageVolumeBinding
import com.simplified.wsstatussaver.storage.Storage
import com.simplified.wsstatussaver.storage.StorageDevice
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class StoragePreferenceDialog : DialogFragment() {

    private val viewModel: WhatSaveViewModel by activityViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogRecyclerviewBinding.inflate(layoutInflater)

        viewModel.getStorageDevices().observe(this) {
            if (it.isEmpty()) {
                binding.empty.setText(R.string.no_storage_device_found)
                binding.empty.isVisible = true
                binding.recyclerView.isVisible = false
            } else {
                binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerView.adapter = Adapter(requireContext(), it)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.statuses_location_title)
            .setView(binding.root)
            .setNegativeButton(R.string.close_action, null)
            .create().also {
                it.setOnShowListener {
                    viewModel.loadStorageDevices()
                }
            }
    }

    private class Adapter constructor(private val context: Context, private val storageVolumes: List<StorageDevice>) :
        RecyclerView.Adapter<Adapter.ViewHolder>(), KoinComponent {

        private val storage: Storage by inject()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemStorageVolumeBinding.inflate(LayoutInflater.from(context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val storageVolume = storageVolumes[position]

            holder.icon?.setImageResource(storageVolume.iconRes)
            holder.name?.setText(storageVolume.nameRes)

            if (storage.isDefaultStatusesLocation(storageVolume)) {
                holder.info?.setText(R.string.statuses_location_default)
            } else holder.info?.visibility = View.GONE

            holder.radioButton?.isChecked = storage.isStatusesLocation(storageVolume)
        }

        override fun getItemCount(): Int = storageVolumes.size

        inner class ViewHolder(binding: ItemStorageVolumeBinding) : RecyclerView.ViewHolder(binding.root),
            View.OnClickListener {
            var icon: ImageView? = binding.icon
            var name: TextView? = binding.name
            var info: TextView? = binding.info
            var radioButton: RadioButton? = binding.radioButton

            override fun onClick(view: View) {
                storage.setStatusesLocation(storageVolumes[layoutPosition])
                notifyDataSetChanged()
            }

            init {
                itemView.setOnClickListener(this)
            }
        }

    }
}