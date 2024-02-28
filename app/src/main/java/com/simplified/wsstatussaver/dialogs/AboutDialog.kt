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
package com.simplified.wsstatussaver.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.activities.LicenseActivity
import com.simplified.wsstatussaver.databinding.DialogAboutBinding
import com.simplified.wsstatussaver.extensions.openWeb
import com.simplified.wsstatussaver.extensions.toChooser
import com.simplified.wsstatussaver.getApp

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class AboutDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAboutBinding.inflate(layoutInflater).apply {
            version.text = getString(R.string.version_x, getApp().versionName)
            shareApp.setOnClickListener {
                shareApp()
            }
            appVersion.setOnClickListener {
                context?.openWeb(GITHUB_RELEASES)
            }
            author.setOnClickListener {
                context?.openWeb(MARDOUS_GITHUB)
            }
            forkOnGithub.setOnClickListener {
                context?.openWeb(WHATSAVE_GITHUB)
            }
            contact.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO)
                    .setData("mailto:mardous.contact@gmail.com".toUri())
                    .putExtra(Intent.EXTRA_EMAIL, "mardous.contact@gmail.com")
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                startActivity(intent.toChooser(getString(R.string.contact_title))!!)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about_label)
            .setView(binding.root)
            .setPositiveButton(R.string.close_action, null)
            .setNeutralButton(R.string.legal_notices) { _, _ ->
                startActivity(Intent(requireContext(), LicenseActivity::class.java))
            }
            .create()
    }

    private fun shareApp() {
        ShareCompat.IntentBuilder(requireContext())
            .setChooserTitle(R.string.share_app)
            .setText(getString(R.string.app_share, GITHUB_LATEST_RELEASE))
            .setType("text/plain")
            .startChooser()
    }

    companion object {
        private const val MARDOUS_GITHUB = "https://www.github.com/mardous"
        private const val WHATSAVE_GITHUB = "$MARDOUS_GITHUB/WhatSave"
        private const val GITHUB_RELEASES = "$WHATSAVE_GITHUB/releases"
        private const val GITHUB_LATEST_RELEASE = "$GITHUB_RELEASES/latest"
    }
}