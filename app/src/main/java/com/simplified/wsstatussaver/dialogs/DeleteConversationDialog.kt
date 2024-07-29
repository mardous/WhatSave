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
package com.simplified.wsstatussaver.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.database.Conversation
import com.simplified.wsstatussaver.databinding.DialogDeleteConversationBinding
import com.simplified.wsstatussaver.extensions.parcelableList
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class DeleteConversationDialog : DialogFragment() {

    private val viewModel: WhatSaveViewModel by activityViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDeleteConversationBinding.inflate(layoutInflater)
        val conversations = requireArguments().parcelableList(EXTRA_CONVERSATIONS, Conversation::class)!!
        val titleRes: Int
        if (conversations.size == 1) {
            titleRes = R.string.delete_conversation_title
            binding.message.setText(R.string.delete_conversation_confirmation)
            binding.blacklistSender.setText(R.string.blacklist_sender)
        } else {
            titleRes = R.string.delete_conversations_title
            binding.message.text = getString(R.string.delete_x_conversations_confirmation, conversations.size)
            binding.blacklistSender.setText(R.string.blacklist_senders)
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(binding.root)
            .setPositiveButton(R.string.delete_action) { _: DialogInterface, _: Int ->
                viewModel.deleteConversations(conversations, binding.blacklistSender.isChecked)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private const val EXTRA_CONVERSATIONS = "extra_conversations"

        fun create(conversation: Conversation) = create(listOf(conversation))

        fun create(conversations: List<Conversation>) =
            DeleteConversationDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(EXTRA_CONVERSATIONS, ArrayList(conversations))
                }
            }
    }
}