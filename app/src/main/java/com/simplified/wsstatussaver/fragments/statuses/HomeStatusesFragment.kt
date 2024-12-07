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
package com.simplified.wsstatussaver.fragments.statuses

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.simplified.wsstatussaver.adapter.StatusAdapter
import com.simplified.wsstatussaver.extensions.PREFERENCE_DEFAULT_CLIENT
import com.simplified.wsstatussaver.extensions.PREFERENCE_EXCLUDE_SAVED_STATUSES
import com.simplified.wsstatussaver.extensions.PREFERENCE_STATUSES_LOCATION
import com.simplified.wsstatussaver.extensions.PREFERENCE_WHATSAPP_ICON
import com.simplified.wsstatussaver.extensions.isWhatsappIcon
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.model.StatusQueryResult
import com.simplified.wsstatussaver.model.StatusType

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class HomeStatusesFragment : StatusesFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getStatuses(statusType).apply {
            observe(viewLifecycleOwner) { result ->
                data(result)
            }
        }.also { liveData ->
            if (liveData.value == StatusQueryResult.Idle) {
                onLoadStatuses(statusType)
            }
        }
        preferences().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateAdapter(): StatusAdapter {
        return StatusAdapter(
            requireActivity(),
            this,
            isSaveEnabled = true,
            isDeleteEnabled = false,
            isWhatsAppIconEnabled = preferences().isWhatsappIcon()
        )
    }

    override fun onDestroyView() {
        preferences().unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            PREFERENCE_DEFAULT_CLIENT,
            PREFERENCE_STATUSES_LOCATION,
            PREFERENCE_EXCLUDE_SAVED_STATUSES -> onLoadStatuses(statusType)

            PREFERENCE_WHATSAPP_ICON -> statusAdapter?.isWhatsAppIconEnabled = preferences.isWhatsappIcon()
        }
    }

    override fun onLoadStatuses(type: StatusType) {
        viewModel.loadStatuses(type)
    }

}