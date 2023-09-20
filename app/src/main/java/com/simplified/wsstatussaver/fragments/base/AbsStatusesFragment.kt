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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.activities.setupWhatsAppMenuItem
import com.simplified.wsstatussaver.adapter.PagerAdapter
import com.simplified.wsstatussaver.databinding.FragmentStatusesBinding
import com.simplified.wsstatussaver.extensions.PREFERENCE_DEFAULT_CLIENT
import com.simplified.wsstatussaver.extensions.doOnPageSelected
import com.simplified.wsstatussaver.extensions.findCurrentFragment
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.interfaces.IScrollable
import com.simplified.wsstatussaver.model.StatusType

/**
 * @author Christians Martínez Alvarado (mardous)
 */
abstract class AbsStatusesFragment : BaseFragment(R.layout.fragment_statuses),
    SharedPreferences.OnSharedPreferenceChangeListener, IScrollable {

    private var _binding: FragmentStatusesBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabLayoutMediator: TabLayoutMediator
    protected var pagerAdapter: PagerAdapter? = null
    protected var currentType: StatusType
        get() = StatusType.values().first { type -> type.ordinal == binding.viewPager.currentItem }
        set(type) {
            binding.viewPager.currentItem = type.ordinal
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusesBinding.bind(view).apply {
            appBar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(requireContext())
            viewPager.doOnPageSelected(viewLifecycleOwner) {
                onBackPressedCallback.isEnabled = currentType != StatusType.IMAGE
            }
            viewPager.adapter = onCreatePagerAdapter().also { newPagerAdapter ->
                pagerAdapter = newPagerAdapter
            }
            viewPager.offscreenPageLimit = pagerAdapter!!.itemCount - 1
        }.also { viewBinding ->
            tabLayoutMediator =
                TabLayoutMediator(viewBinding.tabLayout, viewBinding.viewPager) { tab: TabLayout.Tab, position: Int ->
                    tab.text = pagerAdapter?.getPageTitle(position)
                }.also { mediator ->
                    mediator.attach()
                }
        }

        statusesActivity.setSupportActionBar(binding.toolbar)
        statusesActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        preferences().registerOnSharedPreferenceChangeListener(this)
    }

    protected abstract fun onCreatePagerAdapter(): PagerAdapter

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (PREFERENCE_DEFAULT_CLIENT == key) {
            _binding?.apply { toolbar.menu?.setupWhatsAppMenuItem(requireActivity()) }
        }
    }

    override fun onDestroyView() {
        preferences().unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
        tabLayoutMediator.detach()
        _binding = null
    }

    override fun onScrollToTop() {
        val currentFragment = binding.viewPager.findCurrentFragment(childFragmentManager)
        if (currentFragment is AbsPagerFragment) {
            currentFragment.onScrollToTop()
        }
    }

    internal fun getViewPager() = binding.viewPager

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            currentType = StatusType.IMAGE
        }
    }
}