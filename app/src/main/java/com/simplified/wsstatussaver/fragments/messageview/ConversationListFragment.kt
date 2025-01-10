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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.OVER_SCROLL_NEVER
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.ConversationAdapter
import com.simplified.wsstatussaver.database.Conversation
import com.simplified.wsstatussaver.databinding.FragmentConversationsBinding
import com.simplified.wsstatussaver.dialogs.BlacklistedSenderDialog
import com.simplified.wsstatussaver.dialogs.DeleteConversationDialog
import com.simplified.wsstatussaver.extensions.applyBottomWindowInsets
import com.simplified.wsstatussaver.extensions.bindNotificationListener
import com.simplified.wsstatussaver.extensions.getIntRes
import com.simplified.wsstatussaver.extensions.isMessageViewEnabled
import com.simplified.wsstatussaver.extensions.isNotificationListener
import com.simplified.wsstatussaver.extensions.isNullOrEmpty
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.requestContext
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
    private var swipeManager: RecyclerViewSwipeManager? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
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
        binding.scrollView.applyBottomWindowInsets()
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)

        adapter = ConversationAdapter(requireContext(), arrayListOf(), this).also {
            it.registerAdapterDataObserver(adapterDataObserver)
        }

        binding.toolbar.setTitle(R.string.message_view)
        statusesActivity.setSupportActionBar(binding.toolbar)
        setupSwitch()
        setupRecyclerView()

        viewModel.messageSenders().observe(viewLifecycleOwner) {
            adapter?.data(it)
            updateEmptyView()
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
                binding.emptyView.isVisible = true
                binding.recyclerView.overScrollMode = OVER_SCROLL_NEVER
            } else {
                binding.emptyView.isVisible = false
                binding.recyclerView.overScrollMode = getIntRes(R.integer.overScrollMode)
            }
        }
    }

    private fun setupSwitch() {
        binding.switchWithContainer.isChecked = isMessageViewEnabled
        binding.switchWithContainer.setOnCheckedChangeListener(this)
    }

    private fun setupRecyclerView() {
        swipeManager = RecyclerViewSwipeManager().also {
            wrappedAdapter = it.createWrappedAdapter(adapter!!)
        }
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            recycleChildrenOnDetach = true
        }
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = RefactoredDefaultItemAnimator()
        swipeManager!!.attachRecyclerView(binding.recyclerView)
    }

    private fun updateEmptyView() {
        binding.emptyTitle.setText(R.string.empty)
        binding.emptyText.setText(R.string.no_conversations)
        binding.progressIndicator.hide()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.menu_conversations, menu)
        menu.removeItem(R.id.action_settings)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_blacklisted_senders -> {
                BlacklistedSenderDialog().show(childFragmentManager, "BLACKLISTED_SENDER")
                return true
            }

            R.id.action_clear_messages -> {
                viewModel.deleteAllMessages()
                return true
            }

            else -> return super.onMenuItemSelected(menuItem)
        }
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

    override fun conversationClick(conversation: Conversation) {
        val arguments = ConversationDetailFragmentArgs.Builder(conversation)
            .build()
            .toBundle()

        findNavController().navigate(R.id.messagesFragment, arguments)
    }

    override fun conversationSwiped(conversation: Conversation) {
        DeleteConversationDialog.create(conversation)
            .show(childFragmentManager, "DELETE_CONVERSATION")
    }

    override fun conversationMultiSelectionClick(item: MenuItem, selection: List<Conversation>) {
        when (item.itemId) {
            R.id.action_delete -> {
                DeleteConversationDialog.create(selection)
                    .show(childFragmentManager, "DELETE_CONVERSATION")
            }
        }
    }

    override fun onPause() {
        swipeManager?.cancelSwipe()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.layoutManager = null
        adapter?.unregisterAdapterDataObserver(adapterDataObserver)
        swipeManager?.release()
        swipeManager = null
        WrapperAdapterUtils.releaseAll(wrappedAdapter)
        wrappedAdapter = null
        adapter = null
    }
}