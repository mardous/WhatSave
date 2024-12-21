/*
 * Copyright (C) 2024 Christians Martínez Alvarado
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
package com.simplified.wsstatussaver.fragments

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.extensions.applyLandscapeInsetter
import com.simplified.wsstatussaver.extensions.currentFragment
import com.simplified.wsstatussaver.extensions.getBottomInsets
import com.simplified.wsstatussaver.extensions.hide
import com.simplified.wsstatussaver.extensions.requireWindow
import com.simplified.wsstatussaver.extensions.show
import com.simplified.wsstatussaver.extensions.whichFragment
import dev.chrisbanes.insetter.applyInsetter

/**
 * @author Christians M. A. (mardous)
 */
class MainFragment : Fragment(R.layout.fragment_main),
    NavigationBarView.OnItemReselectedListener,
    NavController.OnDestinationChangedListener {

    private lateinit var contentView: FrameLayout
    private lateinit var navigationView: NavigationBarView
    private lateinit var childNavController: NavController

    private var windowInsets: WindowInsetsCompat? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentView = view.findViewById(R.id.main_container)
        contentView.applyLandscapeInsetter {
            type(navigationBars = true, displayCutout = true) {
                padding(right = true)
            }
        }
        navigationView = view.findViewById(R.id.navigation_view)
        navigationView.setOnItemReselectedListener(this)
        if (navigationView is BottomNavigationView) {
            navigationView.applyInsetter {
                type(navigationBars = true) {
                    padding(vertical = true)
                    margin(horizontal = true)
                }
            }
        } else if (navigationView is NavigationRailView) {
            navigationView.applyInsetter {
                type(navigationBars = true, displayCutout = true) {
                    padding(left = true)
                }
            }
        }

        requireWindow().decorView.setOnApplyWindowInsetsListener { _, insets ->
            windowInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
            insets
        }

        childNavController = whichFragment<NavHostFragment>(R.id.main_container).navController
        childNavController.addOnDestinationChangedListener(this)
        navigationView.setupWithNavController(childNavController)
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
        if (currentFragment is SectionFragment) {
            currentFragment.scrollToTop()
        }
    }

    override fun onDestroyView() {
        childNavController.removeOnDestinationChangedListener(this)
        super.onDestroyView()
    }

    private fun hideBottomBar(hide: Boolean) {
        if (hide) navigationView.hide() else navigationView.show()
        if (navigationView is NavigationRailView) return
        val navHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
        val navHeightWithInsets = navHeight + windowInsets.getBottomInsets()
        contentView.updatePadding(bottom = if (!hide) navHeightWithInsets else 0)
    }
}