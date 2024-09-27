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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.view.doOnPreDraw
import com.google.android.material.transition.MaterialSharedAxis
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.FragmentAboutBinding
import com.simplified.wsstatussaver.extensions.openWeb
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.getApp

/**
 * @author Christians M. A. (mardous)
 */
class AboutFragment : BaseFragment(R.layout.fragment_about), View.OnClickListener {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        _binding = FragmentAboutBinding.bind(view)
        binding.toolbar.setTitle(R.string.about_title)
        binding.version.text = getString(R.string.version_x, getApp().versionName)
        binding.shareApp.setOnClickListener(this)
        binding.contact.setOnClickListener(this)
        binding.author.setOnClickListener(this)
        binding.latestRelease.setOnClickListener(this)
        binding.forkOnGithub.setOnClickListener(this)
        binding.translations.setOnClickListener(this)
        binding.issueTracker.setOnClickListener(this)
        statusesActivity.setSupportActionBar(binding.toolbar)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menu.clear()
    }

    override fun onClick(v: View) {
        when (v) {
            binding.shareApp -> {
                ShareCompat.IntentBuilder(requireContext())
                    .setChooserTitle(R.string.share_app)
                    .setText(getString(R.string.app_share, LATEST_RELEASE_URL))
                    .setType("text/plain")
                    .startChooser()
            }

            binding.contact -> {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("mardous.contact@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.app_name)} - Support & questions")
                }
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_title)))
            }

            binding.author -> requireContext().openWeb(MARDOUS_URL)
            binding.latestRelease -> requireContext().openWeb(LATEST_RELEASE_URL)
            binding.translations -> requireContext().openWeb(TRANSLATIONS_URL)
            binding.forkOnGithub -> requireContext().openWeb(APP_GITHUB_URL)
            binding.issueTracker -> requireContext().openWeb(ISSUE_TRACKER_URL)
        }
    }

    companion object {
        private const val TRANSLATIONS_URL = "https://hosted.weblate.org/projects/whatsave/"
        private const val MARDOUS_URL = "https://github.com/mardous"
        private const val APP_GITHUB_URL = "$MARDOUS_URL/WhatSave"
        private const val LATEST_RELEASE_URL = "$APP_GITHUB_URL/releases/latest"
        private const val ISSUE_TRACKER_URL = "$APP_GITHUB_URL/issues"
        const val PRIVACY_POLICY = "$APP_GITHUB_URL/blob/master/PRIVACY.md"
    }
}