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
package com.simplified.wsstatussaver.fragments.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.simplified.wsstatussaver.activities.StatusesActivity
import com.simplified.wsstatussaver.extensions.hasQ

abstract class BaseFragment @JvmOverloads constructor(@LayoutRes layoutRes: Int = 0) : Fragment(layoutRes),
    MenuProvider {

    protected val statusesActivity: StatusesActivity
        get() = activity as StatusesActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.STARTED)
    }

    protected fun hasStoragePermission() = statusesActivity.hasStoragePermissions()

    protected fun requestPermission(isShowOnboard: Boolean = hasQ()) =
        statusesActivity.requestStoragePermissions(isShowOnboard)

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }
}