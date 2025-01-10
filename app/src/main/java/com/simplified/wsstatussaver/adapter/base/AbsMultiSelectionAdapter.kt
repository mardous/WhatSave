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
package com.simplified.wsstatussaver.adapter.base

import android.annotation.SuppressLint
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.simplified.wsstatussaver.R

/**
 * @author Christians Martínez Alvarado (mardous)
 */
@SuppressLint("NotifyDataSetChanged")
abstract class AbsMultiSelectionAdapter<Data, VH : ViewHolder>(
    private val context: Context,
    private val multiMenuRes: Int,
) : RecyclerView.Adapter<VH>(), ActionMode.Callback {

    private var actionMode: ActionMode? = null
    private val checked = ArrayList<Data>()

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val menuInflater = mode.menuInflater
        menuInflater.inflate(multiMenuRes, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.action_select_all) {
            checkAll()
        } else {
            onMultiSelectionItemClick(item, ArrayList(checked))
            finishActionMode()
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        clearChecked()
        actionMode = null
        onBackPressedCallback.remove()
    }

    protected abstract fun onMultiSelectionItemClick(menuItem: MenuItem, selection: List<Data>)

    protected abstract fun getIdentifier(position: Int): Data?

    protected fun isItemSelected(item: Data) = actionMode != null && checked.contains(item)

    protected fun isMultiSelectionMode() = actionMode != null

    protected fun toggleItemChecked(position: Int): Boolean {
        val identifier = getIdentifier(position) ?: return false
        if (!checked.remove(identifier)) {
            checked.add(identifier)
        }
        notifyItemChanged(position)
        updateCab()
        return true
    }

    private fun checkAll() {
        if (actionMode != null) {
            checked.clear()
            for (i in 0 until itemCount) {
                val identifier = getIdentifier(i) ?: continue
                checked.add(identifier)
            }
            notifyDataSetChanged()
            updateCab()
        }
    }

    private fun clearChecked() {
        checked.clear()
        notifyDataSetChanged()
    }

    private fun updateCab() {
        if (actionMode == null) {
            actionMode = (context as AppCompatActivity).startSupportActionMode(this)
            context.onBackPressedDispatcher.addCallback(onBackPressedCallback)
        }
        val size = checked.size
        if (size <= 0) actionMode?.finish()
        else actionMode?.title = context.getString(R.string.x_selected, size)
    }

    fun finishActionMode() {
        actionMode?.finish()
        clearChecked()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (actionMode != null) {
                actionMode?.finish()
                remove()
            }
        }
    }
}