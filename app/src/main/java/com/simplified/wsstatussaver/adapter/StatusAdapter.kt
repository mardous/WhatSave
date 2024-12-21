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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.video.VideoFrameDecoder
import com.google.android.material.card.MaterialCardView
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.base.AbsMultiSelectionAdapter
import com.simplified.wsstatussaver.databinding.ItemStatusBinding
import com.simplified.wsstatussaver.extensions.getClientIfInstalled
import com.simplified.wsstatussaver.extensions.getFormattedDate
import com.simplified.wsstatussaver.extensions.getState
import com.simplified.wsstatussaver.interfaces.IStatusCallback
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * @author Christians Martínez Alvarado (mardous)
 */
@SuppressLint("NotifyDataSetChanged")
open class StatusAdapter(
    protected val activity: FragmentActivity,
    private val callback: IStatusCallback,
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

        if (holder.cardView != null) {
            holder.cardView.isChecked = isItemSelected(status)
        } else {
            holder.itemView.isActivated = isItemSelected(status)
        }

        if (status.type == StatusType.VIDEO) {
            holder.image?.load(status.fileUri) {
                decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
            }
        } else {
            holder.image?.load(status.fileUri)
        }

        if (holder.state != null) {
            if (isSaveEnabled) {
                holder.state.text = status.getState(activity)
            } else {
                holder.state.text = status.getFormattedDate(activity)
            }
        }

        holder.playIcon?.isGone = isSaveEnabled || status.type == StatusType.IMAGE

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
        val state: TextView?
        val clientIcon: ImageView?
        val playIcon: ImageView?
        val cardView: MaterialCardView?

        private val status: Status
            get() = statuses[layoutPosition]

        init {
            val binding = ItemStatusBinding.bind(itemView)
            image = binding.image
            state = binding.state
            cardView = itemView as? MaterialCardView
            cardView?.isCheckable = true
            clientIcon = binding.clientIcon
            playIcon = binding.playIcon

            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            if (!isSavingContent) {
                if (isMultiSelectionMode()) {
                    toggleItemChecked(layoutPosition)
                } else {
                    callback.previewStatusesClick(statuses, layoutPosition)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (!isSavingContent) {
                return toggleItemChecked(layoutPosition)
            }
            return false
        }
    }

    init {
        setHasStableIds(true)
    }
}