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
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.base.AbsMultiSelectionAdapter
import com.simplified.wsstatussaver.database.MessageEntity
import com.simplified.wsstatussaver.extensions.time
import com.simplified.wsstatussaver.interfaces.IMessageCallback

class MessageAdapter(
    private val context: Context,
    private var messages: List<MessageEntity>,
    private val callback: IMessageCallback
) : AbsMultiSelectionAdapter<MessageEntity, MessageAdapter.ViewHolder>(context, R.menu.menu_messages_selection),
    SwipeableItemAdapter<MessageAdapter.ViewHolder> {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.itemView.isActivated = isItemSelected(message)
        holder.message.text = message.content
        holder.time.text = message.time.time(useTimeFormat = true)
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemId(position: Int): Long = messages[position].id.toLong()

    override fun getIdentifier(position: Int) = messages[position]

    override fun onGetSwipeReactionType(holder: MessageAdapter.ViewHolder, position: Int, x: Int, y: Int): Int {
        if (isMultiSelectionMode()) {
            return SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_BOTH_H
        }
        return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
    }

    override fun onSwipeItemStarted(holder: MessageAdapter.ViewHolder, position: Int) {
        holder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun onSetSwipeBackground(holder: MessageAdapter.ViewHolder, position: Int, type: Int) {}

    override fun onSwipeItem(holder: MessageAdapter.ViewHolder, position: Int, result: Int): SwipeResultAction {
        return if (result == SwipeableItemConstants.RESULT_CANCELED) {
            SwipeResultActionDefault()
        } else {
            SwipeResultActionNotify(callback, messages[position])
        }
    }

    override fun onMultiSelectionItemClick(menuItem: MenuItem, selection: List<MessageEntity>) {
        callback.messageMultiSelectionClick(menuItem, selection)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun data(messages: List<MessageEntity>) {
        this.messages = messages
        notifyDataSetChanged()
    }

    init {
        setHasStableIds(true)
    }

    inner class ViewHolder(itemView: View) : AbstractSwipeableItemViewHolder(itemView), View.OnClickListener,
        View.OnLongClickListener {
        val message: TextView = itemView.findViewById(R.id.message)
        val time: TextView = itemView.findViewById(R.id.time)
        private val card: CardView = itemView.findViewById(R.id.card)

        private val currentMessage: MessageEntity?
            get() = layoutPosition.let { position -> if (position > -1) messages[position] else null }

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            if (isMultiSelectionMode()) {
                toggleItemChecked(layoutPosition)
            } else {
                currentMessage?.let { callback.messageClick(it) }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            return toggleItemChecked(layoutPosition)
        }

        override fun getSwipeableContainerView(): View {
            return card
        }
    }

    internal class SwipeResultActionNotify(
        private val callback: IMessageCallback,
        private val messageEntity: MessageEntity
    ) : SwipeResultActionRemoveItem() {

        override fun onPerformAction() {
            callback.messageSwiped(messageEntity)
        }
    }
}