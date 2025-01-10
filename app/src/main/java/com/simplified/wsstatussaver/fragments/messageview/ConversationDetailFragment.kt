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

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.app.ShareCompat
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialFadeThrough
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.MessageAdapter
import com.simplified.wsstatussaver.database.Conversation
import com.simplified.wsstatussaver.database.MessageEntity
import com.simplified.wsstatussaver.databinding.FragmentMessagesBinding
import com.simplified.wsstatussaver.extensions.applyBottomWindowInsets
import com.simplified.wsstatussaver.extensions.startActivitySafe
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.interfaces.IMessageCallback
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ConversationDetailFragment : BaseFragment(R.layout.fragment_messages), IMessageCallback {

    private val arguments by navArgs<ConversationDetailFragmentArgs>()
    private val viewModel: WhatSaveViewModel by activityViewModel()

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private var adapter: MessageAdapter? = null
    private var swipeManager: RecyclerViewSwipeManager? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null

    private val conversation: Conversation
        get() = arguments.extraConversation

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMessagesBinding.bind(view)
        binding.recyclerView.applyBottomWindowInsets()

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)

        setupToolbar()
        setupRecyclerView()

        viewModel.receivedMessages(conversation).observe(viewLifecycleOwner) {
            data(it)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.messages_from_x, conversation.name)
        statusesActivity.setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(requireContext(), arrayListOf(), this)
        swipeManager = RecyclerViewSwipeManager().also {
            wrappedAdapter = it.createWrappedAdapter(adapter!!)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = RefactoredDefaultItemAnimator()
        swipeManager!!.attachRecyclerView(binding.recyclerView)
    }

    private fun data(messages: List<MessageEntity>) {
        adapter?.data(messages)
        if (messages.isEmpty()) {
            findNavController().popBackStack()
        }
    }

    private fun sendText(content: String) {
        startActivitySafe(
            ShareCompat.IntentBuilder(requireContext())
                .setType("text/plain")
                .setText(content)
                .setChooserTitle(R.string.share_with)
                .createChooserIntent()
        )
    }

    override fun messageClick(message: MessageEntity) {
        sendText(message.content)
    }

    override fun messageSwiped(message: MessageEntity) {
        viewModel.deleteMessage(message)
    }

    override fun messageMultiSelectionClick(item: MenuItem, selection: List<MessageEntity>) {
        when (item.itemId) {
            R.id.action_delete -> {
                viewModel.deleteMessages(selection)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menu.removeItem(R.id.action_settings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.layoutManager = null
        swipeManager?.release()
        swipeManager = null
        WrapperAdapterUtils.releaseAll(wrappedAdapter)
        wrappedAdapter = null
        adapter = null
    }
}