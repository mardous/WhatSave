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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.dialogs.UpdateDialog
import com.simplified.wsstatussaver.extensions.STORAGE_PERMISSION_REQUEST
import com.simplified.wsstatussaver.extensions.getGeneralThemeRes
import com.simplified.wsstatussaver.extensions.getPreferredClient
import com.simplified.wsstatussaver.extensions.hasQ
import com.simplified.wsstatussaver.extensions.hasStoragePermissions
import com.simplified.wsstatussaver.extensions.isNightModeEnabled
import com.simplified.wsstatussaver.extensions.isShownOnboard
import com.simplified.wsstatussaver.extensions.openSettings
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.extensions.requestPermissions
import com.simplified.wsstatussaver.extensions.requestWithoutOnboard
import com.simplified.wsstatussaver.extensions.useCustomFont
import com.simplified.wsstatussaver.extensions.whichFragment
import com.simplified.wsstatussaver.interfaces.IPermissionChangeListener
import com.simplified.wsstatussaver.update.isAbleToUpdate
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
open class StatusesActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    private val permissionsChangeListeners: MutableList<IPermissionChangeListener?> = ArrayList()
    private var hadPermissions = false

    private val viewModel by viewModel<WhatSaveViewModel>()
    private var globalNavController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setupTheme()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hadPermissions = hasStoragePermissions()
        ViewGroupCompat.installCompatInsetsDispatch(window.decorView)
        windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (hasQ()) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_main)

        val navigationHost = whichFragment<NavHostFragment>(R.id.global_container)
        globalNavController = navigationHost.navController.also {
            it.addOnDestinationChangedListener(this)
        }

        if (savedInstanceState == null) {
            searchUpdate()
        }
    }

    private fun setupTheme() {
        setTheme(getGeneralThemeRes())
        if (preferences().useCustomFont()) {
            setTheme(R.style.CustomFontThemeOverlay)
        }
    }

    private fun lightSystemBars(isLight: Boolean = !isNightModeEnabled) {
        windowInsetsController.isAppearanceLightStatusBars = isLight
        windowInsetsController.isAppearanceLightNavigationBars = isLight
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

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!hasStoragePermissions()) {
            requestPermissions(preferences().isShownOnboard)
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPermissions = hasStoragePermissions()
        if (hasPermissions != hadPermissions) {
            hadPermissions = hasPermissions
            onHasPermissionsChanged(hasPermissions)
        }
    }

    fun addPermissionsChangeListener(listener: IPermissionChangeListener) {
        permissionsChangeListeners.add(listener)
    }

    fun removePermissionsChangeListener(listener: IPermissionChangeListener) {
        permissionsChangeListeners.remove(listener)
    }

    private fun onHasPermissionsChanged(hasPermissions: Boolean) {
        for (listener in permissionsChangeListeners) {
            listener?.permissionsStateChanged(hasPermissions)
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

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.main_container).popBackStack()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            WRITE_EXTERNAL_STORAGE
                        )
                    ) {
                        //User has denied from permission dialog
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.permissions_denied_title)
                            .setMessage(R.string.permissions_denied_message)
                            .setPositiveButton(R.string.grant_action) { _: DialogInterface, _: Int ->
                                requestWithoutOnboard()
                            }
                            .setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int -> finish() }
                            .setCancelable(false)
                            .show()
                    } else {
                        // User has denied permission and checked never show permission dialog, so you can redirect to Application settings page
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.permissions_denied_title)
                            .setMessage(R.string.permissions_denied_message)
                            .setPositiveButton(R.string.open_settings_action) { _: DialogInterface, _: Int ->
                                openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            }
                            .setNeutralButton(android.R.string.cancel) { _: DialogInterface, _: Int -> finish() }
                            .setCancelable(false)
                            .show()
                    }
                }
                return
            }
        }
        hadPermissions = true
        onHasPermissionsChanged(true)
    }

    override fun onDestroy() {
        globalNavController?.removeOnDestinationChangedListener(this)
        super.onDestroy()
    }

    private fun Menu.setupWhatsAppMenuItem(activity: FragmentActivity) {
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
}