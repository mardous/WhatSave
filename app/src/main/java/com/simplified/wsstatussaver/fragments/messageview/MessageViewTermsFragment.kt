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

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.FragmentMessageviewTermsBinding
import com.simplified.wsstatussaver.extensions.applyBottomWindowInsets
import com.simplified.wsstatussaver.extensions.formattedAsHtml
import com.simplified.wsstatussaver.extensions.hasR
import com.simplified.wsstatussaver.extensions.openSettings
import com.simplified.wsstatussaver.extensions.startActivitySafe
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.service.MessageCatcherService

class MessageViewTermsFragment : BaseFragment(R.layout.fragment_messageview_terms) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentMessageviewTermsBinding.bind(view)
        statusesActivity.setSupportActionBar(binding.toolbar)
        binding.text1.text = getText()
        binding.continueButton.setOnClickListener {
            if (hasR()) {
                val componentName = ComponentName(requireContext(), MessageCatcherService::class.java)
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
                startActivitySafe(intent)
            } else {
                requireContext().openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, packageName = null)
            }
            findNavController().popBackStack()
        }
        binding.scrollView.applyBottomWindowInsets()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menu.clear()
    }

    private fun getText(): CharSequence {
        val terms = resources.getStringArray(R.array.message_view_terms)
        val sb = StringBuilder()
        for (i in terms.indices) {
            sb.append("${i + 1}: ").append(terms[i])
            if (i < terms.size - 1)
                sb.append("<br/><br/>")
        }
        return sb.toString().formattedAsHtml()
    }
}