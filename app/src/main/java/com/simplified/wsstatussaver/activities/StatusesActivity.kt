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

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationBarView
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.activities.base.AbsBaseActivity
import com.simplified.wsstatussaver.dialogs.UpdateDialog
import com.simplified.wsstatussaver.extensions.WHATSAVE_ANIM_TIME
import com.simplified.wsstatussaver.extensions.currentFragment
import com.simplified.wsstatussaver.extensions.getPreferredClient
import com.simplified.wsstatussaver.extensions.hide
import com.simplified.wsstatussaver.extensions.show
import com.simplified.wsstatussaver.extensions.surfaceColor
import com.simplified.wsstatussaver.extensions.whichFragment
import com.simplified.wsstatussaver.fragments.base.AbsStatusesFragment
import com.simplified.wsstatussaver.update.isAbleToUpdate
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class StatusesActivity : AbsBaseActivity(), NavigationBarView.OnItemReselectedListener,
    NavController.OnDestinationChangedListener {

    private val viewModel by viewModel<WhatSaveViewModel>()
    private lateinit var contentView: FrameLayout
    private lateinit var navigationView: NavigationBarView

    @ColorInt
    private var navigationBarColor: Int = 0
    private var navigationBarColorAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        contentView = findViewById(R.id.main_container)
        navigationView = findViewById(R.id.navigation_view)
        navigationView.setOnItemReselectedListener(this)

        navigationBarColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSurfaceContainer,
            Color.BLACK
        )

        val navController = whichFragment<NavHostFragment>(R.id.main_container)?.navController
        if (navController != null) {
            navController.addOnDestinationChangedListener(this)
            navigationView.setupWithNavController(navController)
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

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        when (destination.id) {
            R.id.homeFragment,
            R.id.savedFragment,
            R.id.toolsFragment -> hideBottomBar(false)
            else -> hideBottomBar(true)
        }
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        val currentFragment = currentFragment(R.id.main_container)
        if (currentFragment is AbsStatusesFragment) {
            currentFragment.scrollToTop()
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.main_container).navigateUp()

    override fun onDestroy() {
        findNavController(R.id.main_container).removeOnDestinationChangedListener(this)
        super.onDestroy()
    }

    private fun hideBottomBar(hide: Boolean) {
        if (hide) {
            navigationView.hide()
            animateNavigationBarColor(surfaceColor())
        }else {
            navigationView.show()
            animateNavigationBarColor(navigationBarColor)
        }
        val contentPadding = if (!hide) resources.getDimensionPixelSize(R.dimen.bottom_nav_height) else 0
        contentView.updatePadding(bottom = contentPadding)
    }

    private fun animateNavigationBarColor(color: Int) {
        navigationBarColorAnimator?.cancel()
        navigationBarColorAnimator = ValueAnimator
            .ofArgb(window.navigationBarColor, color).apply {
                duration = WHATSAVE_ANIM_TIME
                interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
                addUpdateListener { animation: ValueAnimator ->
                    setNavigationBarColor(animation.animatedValue as Int)
                }
                start()
            }
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