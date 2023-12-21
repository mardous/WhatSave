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
package com.simplified.wsstatussaver.fragments.messageview

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.OVER_SCROLL_NEVER
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.ConversationAdapter
import com.simplified.wsstatussaver.database.Conversation
import com.simplified.wsstatussaver.databinding.DialogDeleteConversationBinding
import com.simplified.wsstatussaver.databinding.FragmentConversationsBinding
import com.simplified.wsstatussaver.dialogs.BlacklistedSenderDialog
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.fragments.binding.ConversationsBinding
import com.simplified.wsstatussaver.interfaces.IConversationCallback
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ConversationListFragment : BaseFragment(R.layout.fragment_conversations), CompoundButton.OnCheckedChangeListener,
    IConversationCallback {

    private val viewModel: WhatSaveViewModel by activityViewModel()

    private var _binding: ConversationsBinding? = null
    private val binding get() = _binding!!

    private var adapter: ConversationAdapter? = null
    private var blockSwitchListener: Boolean = false

    private var isMessageViewEnabled: Boolean
        get() = requireContext().preferences().isMessageViewEnabled
        set(value) {
            requireContext().preferences().isMessageViewEnabled = value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewBinding = FragmentConversationsBinding.bind(view)
        _binding = ConversationsBinding(viewBinding)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().addTarget(binding.container)
        reenterTransition = MaterialFadeThrough().addTarget(binding.container)

        adapter = ConversationAdapter(requireContext(), arrayListOf(), this).also {
            it.registerAdapterDataObserver(adapterDataObserver)
        }

        binding.toolbar.setTitle(R.string.message_view)
        statusesActivity.setSupportActionBar(binding.toolbar)
        setupSwitch()
        setupRecyclerView()

        viewModel.messageSenders().observe(viewLifecycleOwner) {
            adapter?.data(it)
        }
    }

    override fun onResume() {
        super.onResume()
        requestContext {
            if (!it.isNotificationListener()) {
                findNavController().popBackStack()
            }
        }
    }

    private val adapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            if (adapter.isNullOrEmpty()) {
                updateEmptyView()
                binding.emptyView.isVisible = true
                binding.recyclerView.overScrollMode = OVER_SCROLL_NEVER
            } else {
                binding.emptyView.isVisible = false
                binding.recyclerView.overScrollMode = getIntRes(R.integer.overScrollMode)
            }
        }
    }

    private fun setupSwitch() {
        binding.switchWithContainer.isChecked = preferences().isMessageViewEnabled
        binding.switchWithContainer.setOnCheckedChangeListener(this)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            recycleChildrenOnDetach = true
        }
        binding.recyclerView.adapter = adapter
    }

    private fun updateEmptyView() {
        if (isMessageViewEnabled) {
            binding.emptyTitle.setText(R.string.empty)
            binding.emptyText.setText(R.string.no_conversations)
        } else {
            binding.emptyTitle.setText(R.string.message_view_is_disabled)
            binding.emptyText.setText(R.string.you_wont_be_able_to_see_messages_here)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.menu_conversations, menu)
        menu.removeItem(R.id.action_settings)
        menu.removeItem(R.id.action_about)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_blacklisted_senders) {
            BlacklistedSenderDialog().show(childFragmentManager, "BLACKLISTED_SENDER")
            return true
        }
        return super.onMenuItemSelected(menuItem)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (blockSwitchListener) {
            blockSwitchListener = false
            return
        }
        if (!isChecked) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.disable_message_view_title)
                .setMessage(R.string.disable_message_view_confirmation)
                .setPositiveButton(R.string.yes_action) { _: DialogInterface, _: Int ->
                    isMessageViewEnabled = false
                    updateEmptyView()
                    viewModel.deleteAllMessages()
                }
                .setNegativeButton(R.string.no_action) { _: DialogInterface, _: Int ->
                    setStateManually(buttonView, true)
                }
                .setOnCancelListener {
                    setStateManually(buttonView, true)
                }
                .show()
        } else {
            isMessageViewEnabled = true
            updateEmptyView()
            requestContext {
                if (!it.bindNotificationListener()) {
                    setStateManually(buttonView, false)
                }
            }
        }
    }

    private fun setStateManually(buttonView: CompoundButton, isEnabled: Boolean) {
        blockSwitchListener = true
        buttonView.isChecked = isEnabled
    }

    override fun onConversationClick(conversation: Conversation) {
        val arguments = ConversationDetailFragmentArgs.Builder(conversation)
            .build()
            .toBundle()

        findNavController().navigate(R.id.messagesFragment, arguments)
    }

    override fun onConversationLongClick(conversation: Conversation) {
        val binding = DialogDeleteConversationBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_conversation_title)
            .setView(binding.root)
            .setPositiveButton(R.string.delete_action) { _: DialogInterface, _: Int ->
                viewModel.deleteConversation(conversation, addToBlacklist = binding.blacklistSender.isChecked)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.layoutManager = null
        adapter?.unregisterAdapterDataObserver(adapterDataObserver)
        adapter = null
    }
}