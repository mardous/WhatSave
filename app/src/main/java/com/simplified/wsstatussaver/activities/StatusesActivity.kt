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
package com.simplified.wsstatussaver.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.activities.base.BaseActivity
import com.simplified.wsstatussaver.dialogs.UpdateDialog
import com.simplified.wsstatussaver.extensions.getPreferredClient
import com.simplified.wsstatussaver.extensions.whichFragment
import com.simplified.wsstatussaver.update.isAbleToUpdate
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class StatusesActivity : BaseActivity(), NavController.OnDestinationChangedListener {

    private val viewModel by viewModel<WhatSaveViewModel>()
    private var globalNavController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navigationHost = whichFragment<NavHostFragment>(R.id.global_container)
        globalNavController = navigationHost.navController.also {
            it.addOnDestinationChangedListener(this)
        }

        if (savedInstanceState == null) {
            searchUpdate()
        }
    }

    private fun searchUpdate() {
        if (isAbleToUpdate()) {
            viewModel.getLatestUpdate().observe(this) { updateInfo ->
                if (updateInfo.isDownloadable(this)) {
                    UpdateDialog.create(updateInfo).show(supportFragmentManager, "UPDATE_FOUND")
                }
            }
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        when (destination.id) {
            R.id.mainFragment -> lightSystemBars()
            R.id.playbackFragment -> lightSystemBars(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.setupWhatsAppMenuItem(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                findNavController(R.id.main_container).navigate(R.id.settingsFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.main_container).navigateUp()

    override fun onDestroy() {
        globalNavController?.removeOnDestinationChangedListener(this)
        super.onDestroy()
    }
}

fun Menu.setupWhatsAppMenuItem(activity: FragmentActivity) {
    this.removeItem(R.id.action_launch_client)

    val client = activity.getPreferredClient()
    if (client != null) {
        this.add(
            Menu.NONE, R.id.action_launch_client,
            Menu.FIRST, activity.getString(R.string.launch_x_client, client.displayName)
        )
            .setIcon(R.drawable.ic_open_in_new_24dp)
            .setIntent(client.getLaunchIntent(activity.packageManager))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
}