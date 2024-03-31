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
package com.simplified.wsstatussaver.activities.base

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.interfaces.IPermissionChangeListener

/**
 * @author Christians Martínez Alvarado (mardous)
 */
abstract class AbsBaseActivity : AppCompatActivity() {

    private val permissionsChangeListeners: MutableList<IPermissionChangeListener?> = ArrayList()

    private var hadPermissions = false
    private var lastThemeUpdate: Long = -1

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getGeneralThemeRes())
        super.onCreate(savedInstanceState)
        hadPermissions = hasStoragePermissions()
        lastThemeUpdate = System.currentTimeMillis()
        windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        val navigationBarColor = surfaceColor()
        onSetupSystemBars(navigationBarColor, navigationBarColor)
    }

    @Suppress("DEPRECATION")
    protected open fun onSetupSystemBars(@ColorInt statusBarColor: Int, @ColorInt navigationBarColor: Int) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        windowInsetsController.isAppearanceLightStatusBars = statusBarColor.isColorLight
        setNavigationBarColor(navigationBarColor)
    }

    protected fun setNavigationBarColor(navigationBarColor: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = navigationBarColor
            windowInsetsController.isAppearanceLightNavigationBars = navigationBarColor.isColorLight
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
            onHasPermissionsChanged()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun addPermissionsChangeListener(listener: IPermissionChangeListener) {
        permissionsChangeListeners.add(listener)
    }

    fun removePermissionsChangeListener(listener: IPermissionChangeListener) {
        permissionsChangeListeners.remove(listener)
    }

    private fun onHasPermissionsChanged() {
        for (listener in permissionsChangeListeners) {
            listener?.permissionsStateChanged()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
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
        onHasPermissionsChanged()
    }
}