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
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.StatusAdapter
import com.simplified.wsstatussaver.model.StatusQueryResult

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class SavedStatusesFragment : StatusesFragment() {

    override val lastResult: StatusQueryResult?
        get() = viewModel.getSavedStatuses().value

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusesActivity.setSupportActionBar(binding.toolbar)
        binding.collapsingToolbar.setTitle(getString(R.string.saved_label))
        viewModel.getSavedStatuses().apply {
            observe(viewLifecycleOwner) { result ->
                data(result)
            }
        }.also { liveData ->
            if (liveData.value == StatusQueryResult.Idle) {
                onRefresh()
            }
        }
    }

    //Everytime the 'Saved' tab becomes visible -> Force reload
    //So we beat the race condition that causing Unix time epoch
    override fun onResume() {
        super.onResume()
        onRefresh()
    }

    override fun createAdapter(): StatusAdapter =
        StatusAdapter(
            requireActivity(),
            this,
            isSaveEnabled = false,
            isDeleteEnabled = true,
            isWhatsAppIconEnabled = false
        )

    override fun onRefresh() {
        viewModel.loadSavedStatuses()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {}
}