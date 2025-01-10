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
package com.simplified.wsstatussaver.fragments.playback

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.annotation.OptIn
import androidx.core.view.doOnPreDraw
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.MaterialFadeThrough
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.adapter.PlaybackAdapter
import com.simplified.wsstatussaver.databinding.FragmentPlaybackBinding
import com.simplified.wsstatussaver.extensions.applyHorizontalWindowInsets
import com.simplified.wsstatussaver.fragments.base.BaseFragment

/**
 * @author Christians M. A. (mardous)
 */
class PlaybackFragment : BaseFragment(R.layout.fragment_playback), Player.Listener {

    private val navArgs: PlaybackFragmentArgs by navArgs()

    private var _binding: FragmentPlaybackBinding? = null
    private val binding get() = _binding!!

    private var adapter: PlaybackAdapter? = null

    private val statuses get() = navArgs.statuses.toList()
    private val startPosition get() = navArgs.position

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaybackBinding.bind(view)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)

        binding.toolbar.applyHorizontalWindowInsets(padding = false)
        statusesActivity.setSupportActionBar(binding.toolbar)
        statusesActivity.supportActionBar?.title = null

        initAdapter()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initAdapter() {
        adapter = PlaybackAdapter(this, statuses)
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startPosition, false)
    }

    companion object {
        const val EXTRA_STATUS = "status"
    }
}