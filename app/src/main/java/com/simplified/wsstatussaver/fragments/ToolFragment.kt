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
package com.simplified.wsstatussaver.fragments

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.databinding.FragmentToolBinding
import com.simplified.wsstatussaver.extensions.applyBottomWindowInsets
import com.simplified.wsstatussaver.extensions.isMessageViewEnabled
import com.simplified.wsstatussaver.extensions.isNotificationListener
import com.simplified.wsstatussaver.extensions.launchSafe
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.logToolView
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ToolFragment : BaseFragment(R.layout.fragment_tool) {

    private val viewModel: WhatSaveViewModel by activityViewModel()
    private val keyguardManager: KeyguardManager by lazy { requireContext().getSystemService()!! }
    private lateinit var credentialsRequestLauncher: ActivityResultLauncher<Intent>

    private var _binding: FragmentToolBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentToolBinding.bind(view)
        binding.scrollView.applyBottomWindowInsets()
        binding.msgANumber.setOnClickListener {
            logToolView("MessageFragment", "Message a number")
            findNavController().navigate(R.id.messageFragment)
        }
        binding.messageView.setOnClickListener {
            logToolView("ConversationListFragment", "Message view")
            if (requireContext().isNotificationListener()) {
                openMessageView()
            } else {
                findNavController().navigate(R.id.messageViewTermsFragment)
            }
        }

        statusesActivity.setSupportActionBar(binding.toolbar)
        credentialsRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.unlockMessageView()
            } else {
                viewModel.getMessageViewLockObservable().removeObserver(credentialObserver)
            }
        }

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
    }

    private fun openMessageView() {
        viewModel.getMessageViewLockObservable().observe(viewLifecycleOwner, credentialObserver)
    }

    @Suppress("DEPRECATION")
    private val credentialObserver = Observer<Boolean> { isUnlocked ->
        if (isUnlocked || !preferences().isMessageViewEnabled) {
            findNavController().navigate(R.id.conversationsFragment)
        } else {
            val credentialsRequestIntel = keyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.message_view),
                getString(R.string.confirm_device_credentials)
            )
            if (credentialsRequestIntel != null) {
                credentialsRequestLauncher.launchSafe(credentialsRequestIntel)
            } else {
                viewModel.unlockMessageView()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}