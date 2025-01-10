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
package com.simplified.wsstatussaver.fragments.playback.image

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import coil3.load
import com.google.android.material.button.MaterialButton
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.FragmentImageBinding
import com.simplified.wsstatussaver.extensions.applyWindowInsets
import com.simplified.wsstatussaver.extensions.paddingSpace
import com.simplified.wsstatussaver.fragments.playback.PlaybackChildFragment

/**
 * @author Christians M. A. (mardous)
 */
class ImageFragment : PlaybackChildFragment(R.layout.fragment_image) {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!
    private val imageView get() = binding.image

    override val saveButton: MaterialButton
        get() = binding.playbackActionButton.save

    override val shareButton: MaterialButton
        get() = binding.playbackActionButton.share

    override val deleteButton: MaterialButton
        get() = binding.playbackActionButton.delete

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImageBinding.bind(view)
        binding.playbackActionButton.root.apply {
            applyWindowInsets(left = true, right = true, bottom = true, addedSpace = paddingSpace())
        }
        imageView.load(status.fileUri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}