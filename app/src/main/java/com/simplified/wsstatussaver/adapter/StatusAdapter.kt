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

@file:Suppress("LeakingThis")

package com.simplified.wsstatussaver.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.base.AbsMultiSelectionAdapter
import com.simplified.wsstatussaver.databinding.ItemStatusBinding
import com.simplified.wsstatussaver.extensions.LongPressAction
import com.simplified.wsstatussaver.extensions.getClientIfInstalled
import com.simplified.wsstatussaver.extensions.getFormattedDate
import com.simplified.wsstatussaver.extensions.getLongPressAction
import com.simplified.wsstatussaver.extensions.getState
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.showStatusOptions
import com.simplified.wsstatussaver.interfaces.IMultiStatusCallback
import com.simplified.wsstatussaver.model.Status
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * @author Christians Martínez Alvarado (mardous)
 */
@SuppressLint("NotifyDataSetChanged")
open class StatusAdapter(
    protected val activity: FragmentActivity,
    private val requestManager: RequestManager,
    private val callback: IMultiStatusCallback,
    private var isSaveEnabled: Boolean,
    private var isDeleteEnabled: Boolean,
    isWhatsAppIconEnabled: Boolean
) : AbsMultiSelectionAdapter<Status, StatusAdapter.ViewHolder>(activity, R.menu.menu_statuses_selection) {

    var statuses: List<Status> by Delegates.observable(ArrayList()) { _: KProperty<*>, _: List<Status>, _: List<Status> ->
        notifyDataSetChanged()
    }
    var isSavingContent by Delegates.observable(false) { _: KProperty<*>, _: Boolean, _: Boolean ->
        notifyDataSetChanged()
    }
    var isWhatsAppIconEnabled by Delegates.observable(isWhatsAppIconEnabled) { _: KProperty<*>, _: Boolean, _: Boolean ->
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(R.layout.item_status, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val status = statuses[position]

        holder.itemView.isActivated = isItemSelected(status)

        if (holder.image != null) {
            requestManager.load(status.fileUri)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .format(DecodeFormat.PREFER_RGB_565)
                .centerCrop()
                .into(holder.image)
        }

        if (holder.date != null && holder.date.isVisible) {
            holder.date.text = status.getFormattedDate(activity)
        }

        if (holder.state != null && holder.state.isVisible) {
            holder.state.text = status.getState(activity)
        }

        if (holder.clientIcon != null) {
            holder.clientIcon.isVisible = false
            holder.clientIcon.setImageDrawable(null)
            if (isWhatsAppIconEnabled) {
                val client = activity.getClientIfInstalled(status.clientPackage)
                if (client != null) {
                    holder.clientIcon.isVisible = true
                    holder.clientIcon.setImageDrawable(client.getIcon(activity))
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return statuses[position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return statuses.size
    }

    override fun getIdentifier(position: Int): Status {
        return statuses[position]
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        if (!isSaveEnabled) {
            menu.removeItem(R.id.action_save)
        }
        if (!isDeleteEnabled) {
            menu.removeItem(R.id.action_delete)
        }
        return false
    }

    override fun onMultiSelectionItemClick(menuItem: MenuItem, selection: List<Status>) {
        callback.multiSelectionItemClick(menuItem, selection)
    }

    @SuppressLint("ClickableViewAccessibility")
    open inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener,
        OnLongClickListener {
        val image: ImageView?
        val date: TextView?
        val state: TextView?
        val clientIcon: ImageView?

        private val status: Status
            get() = statuses[layoutPosition]

        init {
            val binding = ItemStatusBinding.bind(itemView)
            image = binding.image
            date = binding.date
            state = binding.state
            state.isVisible = isSaveEnabled
            clientIcon = binding.clientIcon

            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            if (!isSavingContent) {
                if (isMultiSelectionMode()) {
                    toggleItemChecked(layoutPosition)
                } else {
                    activity.showStatusOptions(status, isSaveEnabled, isDeleteEnabled, callback)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (!isSavingContent) {
                when (activity.preferences().getLongPressAction()) {
                    LongPressAction.VALUE_MULTI_SELECTION -> {
                        return toggleItemChecked(layoutPosition)
                    }

                    LongPressAction.VALUE_PREVIEW -> {
                        callback.previewStatusClick(status)
                        return true
                    }

                    LongPressAction.VALUE_SAVE -> {
                        if (isSaveEnabled) {
                            callback.saveStatusClick(status)
                        }
                        return true
                    }

                    LongPressAction.VALUE_SHARE -> {
                        callback.shareStatusClick(status)
                        return true
                    }

                    LongPressAction.VALUE_DELETE -> {
                        if (isDeleteEnabled) {
                            callback.deleteStatusClick(status)
                        }
                        return true
                    }
                }
            }
            return false
        }
    }

    init {
        setHasStableIds(true)
    }
}