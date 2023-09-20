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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.activities.base.AbsBaseActivity
import com.simplified.wsstatussaver.dialogs.AboutDialog
import com.simplified.wsstatussaver.dialogs.PrivacyDialog
import com.simplified.wsstatussaver.dialogs.UpdateDialog
import com.simplified.wsstatussaver.extensions.currentFragment
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.privacyPolicyAccepted
import com.simplified.wsstatussaver.extensions.whichFragment
import com.simplified.wsstatussaver.fragments.base.AbsStatusesFragment
import com.simplified.wsstatussaver.mediator.WAMediator
import com.simplified.wsstatussaver.mediator.getLaunchIntent
import com.simplified.wsstatussaver.update.isAbleToUpdate
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class StatusesActivity : AbsBaseActivity(), NavigationBarView.OnItemReselectedListener {

    private val viewModel by viewModel<WhatSaveViewModel>()
    private lateinit var navigationView: NavigationBarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigationView = findViewById(R.id.navigation_view)
        navigationView.setOnItemReselectedListener(this)

        val navController = whichFragment<NavHostFragment>(R.id.main_container)?.navController
        if (navController != null) {
            navigationView.setupWithNavController(navController)
        }

        checkPrivacyAccepted()
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

    private fun checkPrivacyAccepted() {
        if (!preferences().privacyPolicyAccepted) {
            PrivacyDialog().show(supportFragmentManager, "PRIVACY_AND_TERMS")
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
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }

            R.id.action_about -> {
                AboutDialog().show(supportFragmentManager, "ABOUT")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val currentFragment = currentFragment(R.id.main_container)
        if (currentFragment is AbsStatusesFragment) {
            currentFragment.onScrollToTop()
        }
    }
}

fun Menu.setupWhatsAppMenuItem(activity: FragmentActivity) {
    this.removeItem(R.id.action_launch_client)

    val client = activity.get<WAMediator>().getDefaultClientOrAny()
    if (client != null) {
        this.add(
            Menu.NONE, R.id.action_launch_client,
            Menu.FIRST, activity.getString(R.string.launch_x_client, client.getLabel(activity))
        )
            .setIcon(R.drawable.ic_whatsapp_24dp)
            .setIntent(client.getLaunchIntent(activity.packageManager))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
}