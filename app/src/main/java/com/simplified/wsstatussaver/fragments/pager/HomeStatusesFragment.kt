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
package com.simplified.wsstatussaver.fragments.pager

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.StatusAdapter
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.fragments.base.AbsPagerFragment
import com.simplified.wsstatussaver.mediator.WAMediator
import com.simplified.wsstatussaver.model.Status
import com.simplified.wsstatussaver.model.StatusType
import org.koin.android.ext.android.inject

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class HomeStatusesFragment : AbsPagerFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val mediator: WAMediator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getStatuses(statusType).observe(viewLifecycleOwner) { statuses ->
            statusAdapter?.statuses = statuses
            binding.swipeRefreshLayout.isRefreshing = false
        }

        preferences().registerOnSharedPreferenceChangeListener(this)
    }

    private fun updateWhatsAppState() {
        if (mediator.isAnyWhatsappInstalled()) {
            binding.emptyTitle.text = getString(R.string.no_x_statuses_title, getString(statusType.nameRes))
            binding.emptyText.text = getString(R.string.you_should_open_wa_and_download_some_statuses)
            binding.emptyButton.text = getString(R.string.open_wa_action)
        } else {
            binding.emptyTitle.text = getString(R.string.wa_is_not_installed_title)
            binding.emptyText.text = getString(R.string.this_application_will_not_work)
            binding.emptyButton.text = getString(R.string.install_wa_action)
        }
    }

    override fun onCreateAdapter(): StatusAdapter {
        return StatusAdapter(
            requireActivity(),
            Glide.with(this),
            this,
            isSaveEnabled = true,
            isDeleteEnabled = false,
            isWhatsAppIconEnabled = preferences().isWhatsappIcon()
        )
    }

    override fun onResume() {
        super.onResume()
        loadStatuses()
    }

    override fun onDestroyView() {
        preferences().unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        when (key) {
            PREFERENCE_DEFAULT_CLIENT,
            PREFERENCE_STATUSES_LOCATION,
            PREFERENCE_EXCLUDE_OLD_STATUSES,
            PREFERENCE_EXCLUDE_SAVED_STATUSES -> loadStatuses()

            PREFERENCE_WHATSAPP_ICON -> statusAdapter?.isWhatsAppIconEnabled = preferences.isWhatsappIcon()
        }
    }

    override fun onDeleteStatusClick(status: Status) {}

    override fun onLoadStatuses(type: StatusType) {
        updateWhatsAppState()
        viewModel.loadStatuses(type)
    }

    override fun onEmptyViewButtonClick() {
        if (mediator.isAnyWhatsappInstalled()) {
            startActivitySafe(mediator.getWhatsAppLaunchIntent()) { e, _ ->
                showToast(getString(R.string.could_not_open_wa, e))
            }
        } else {
            requestContext { context -> context.openGooglePlay("com.whatsapp") }
        }
    }
}