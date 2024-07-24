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
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.base.AbsMultiSelectionAdapter
import com.simplified.wsstatussaver.database.Conversation
import com.simplified.wsstatussaver.extensions.time
import com.simplified.wsstatussaver.interfaces.IConversationCallback
import java.util.concurrent.TimeUnit

class ConversationAdapter(
    private val context: Context,
    private var conversations: List<Conversation>,
    private val callback: IConversationCallback
) : AbsMultiSelectionAdapter<Conversation, ConversationAdapter.ViewHolder>(context, R.menu.menu_messages_selection),
    SwipeableItemAdapter<ConversationAdapter.ViewHolder> {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.itemView.isActivated = isItemSelected(conversation)
        holder.name?.text = conversation.name
        holder.message?.text = conversation.latestMessage
        holder.time?.text = conversation.latestMessageTime.time(5, TimeUnit.DAYS)
    }

    override fun getItemCount(): Int = conversations.size

    override fun getItemId(position: Int): Long {
        return conversations[position].id
    }

    override fun getIdentifier(position: Int) = conversations[position]

    override fun onMultiSelectionItemClick(menuItem: MenuItem, selection: List<Conversation>) {
        callback.conversationMultiSelectionClick(menuItem, selection)
    }

    override fun onGetSwipeReactionType(holder: ViewHolder, position: Int, x: Int, y: Int): Int {
        if (isMultiSelectionMode()) {
            return SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_BOTH_H
        }
        return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
    }

    override fun onSwipeItemStarted(holder: ViewHolder, position: Int) {
        holder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun onSetSwipeBackground(holder: ViewHolder, position: Int, type: Int) {}

    override fun onSwipeItem(holder: ViewHolder, position: Int, result: Int): SwipeResultAction {
        return if (result == SwipeableItemConstants.RESULT_CANCELED) {
            SwipeResultActionDefault()
        } else {
            SwipeResultActionNotify(callback, conversations[position])
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun data(dataSet: List<Conversation>) {
        this.conversations = dataSet
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : AbstractSwipeableItemViewHolder(itemView), View.OnClickListener,
        View.OnLongClickListener {

        val name: TextView? = itemView.findViewById(R.id.name)
        val message: TextView? = itemView.findViewById(R.id.message)
        val time: TextView? = itemView.findViewById(R.id.time)
        private val dummyContainer: View = itemView.findViewById(R.id.dummyContainer)

        private val currentItem: Conversation
            get() = conversations[layoutPosition]

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            if (isMultiSelectionMode()) {
                toggleItemChecked(layoutPosition)
            } else {
                callback.conversationClick(currentItem)
            }
        }

        override fun onLongClick(view: View): Boolean {
            return toggleItemChecked(layoutPosition)
        }

        override fun getSwipeableContainerView(): View {
            return dummyContainer
        }
    }

    internal class SwipeResultActionNotify(
        private val callback: IConversationCallback,
        private val conversation: Conversation
    ) : SwipeResultActionDefault() {

        override fun onPerformAction() {
            callback.conversationSwiped(conversation)
        }
    }
}