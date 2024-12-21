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

import android.os.Bundle
import android.view.View
import com.simplified.wsstatussaver.model.StatusQueryResult
import com.simplified.wsstatussaver.model.StatusType

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class ImageStatusesFragment : StatusesFragment() {

    private val imageType: StatusType = StatusType.IMAGE

    override val lastResult: StatusQueryResult?
        get() = viewModel.getStatuses(imageType).value

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusesActivity.setSupportActionBar(binding.toolbar)
        binding.collapsingToolbar.setTitle(getString(imageType.nameRes))
        viewModel.getStatuses(imageType).apply {
            observe(viewLifecycleOwner) { result ->
                data(result)
            }
        }.also { liveData ->
            if (liveData.value == StatusQueryResult.Idle) {
                onRefresh()
            }
        }
    }

    override fun onRefresh() {
        viewModel.loadStatuses(imageType)
    }

}