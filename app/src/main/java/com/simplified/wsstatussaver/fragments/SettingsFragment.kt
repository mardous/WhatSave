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
package com.simplified.wsstatussaver.fragments

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.transition.MaterialFadeThrough
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.FragmentSettingsBinding
import com.simplified.wsstatussaver.extensions.PREFERENCE_ANALYTICS_ENABLED
import com.simplified.wsstatussaver.extensions.PREFERENCE_DEFAULT_CLIENT
import com.simplified.wsstatussaver.extensions.PREFERENCE_GRANT_PERMISSIONS
import com.simplified.wsstatussaver.extensions.PREFERENCE_JUST_BLACK_THEME
import com.simplified.wsstatussaver.extensions.PREFERENCE_LANGUAGE
import com.simplified.wsstatussaver.extensions.PREFERENCE_QUICK_DELETION
import com.simplified.wsstatussaver.extensions.PREFERENCE_STATUSES_LOCATION
import com.simplified.wsstatussaver.extensions.PREFERENCE_THEME_MODE
import com.simplified.wsstatussaver.extensions.PREFERENCE_USE_CUSTOM_FONT
import com.simplified.wsstatussaver.extensions.applyBottomWindowInsets
import com.simplified.wsstatussaver.extensions.findActivityNavController
import com.simplified.wsstatussaver.extensions.getDefaultDayNightMode
import com.simplified.wsstatussaver.extensions.isNightModeEnabled
import com.simplified.wsstatussaver.extensions.whichFragment
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.logLanguageSelected
import com.simplified.wsstatussaver.logThemeSelected
import com.simplified.wsstatussaver.preferences.DefaultClientPreference
import com.simplified.wsstatussaver.preferences.DefaultClientPreferenceDialog
import com.simplified.wsstatussaver.preferences.SaveLocationPreference
import com.simplified.wsstatussaver.preferences.SaveLocationPreferenceDialog
import com.simplified.wsstatussaver.preferences.StoragePreference
import com.simplified.wsstatussaver.preferences.StoragePreferenceDialog
import com.simplified.wsstatussaver.setAnalyticsEnabled

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSettingsBinding.bind(view)
        postponeEnterTransition()
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)
        view.doOnPreDraw { startPostponedEnterTransition() }
        statusesActivity.setSupportActionBar(binding.toolbar)

        var settingsFragment: SettingsFragment? = whichFragment(R.id.settings_container)
        if (settingsFragment == null) {
            settingsFragment = SettingsFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, settingsFragment)
                .commit()
        } else {
            settingsFragment.invalidatePreferences()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateMenu(menu, menuInflater)
        menu.clear()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.applyBottomWindowInsets()

            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                findNavController().navigate(R.id.aboutFragment)
                true
            }
            invalidatePreferences()
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            when (preference) {
                is SaveLocationPreference -> {
                    SaveLocationPreferenceDialog().show(childFragmentManager, "SAVE_LOCATION")
                    return
                }

                is DefaultClientPreference -> {
                    DefaultClientPreferenceDialog().show(childFragmentManager, "INSTALLED_CLIENTS")
                    return
                }

                is StoragePreference -> {
                    StoragePreferenceDialog().show(childFragmentManager, "STORAGE_DIALOG")
                    return
                }
            }
            super.onDisplayPreferenceDialog(preference)
        }

        fun invalidatePreferences() {
            findPreference<Preference>(PREFERENCE_THEME_MODE)
                ?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                    val themeName = newValue as String
                    AppCompatDelegate.setDefaultNightMode(getDefaultDayNightMode(themeName))
                    logThemeSelected(themeName)
                    true
                }
            findPreference<SwitchPreferenceCompat>(PREFERENCE_JUST_BLACK_THEME)
                ?.apply {
                    isEnabled = requireContext().isNightModeEnabled
                    setOnPreferenceChangeListener { _, _ ->
                        requireActivity().recreate()
                        true
                    }
                }
            findPreference<Preference>(PREFERENCE_USE_CUSTOM_FONT)
                ?.setOnPreferenceChangeListener { _, _ ->
                    requireActivity().recreate()
                    true
                }
            findPreference<Preference>(PREFERENCE_LANGUAGE)?.setOnPreferenceChangeListener { _, newValue ->
                val languageName = newValue as String
                if (languageName == "auto") {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageName))
                }
                logLanguageSelected(languageName)
                true
            }
            findPreference<Preference>(PREFERENCE_ANALYTICS_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
                setAnalyticsEnabled((newValue as Boolean))
                true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                findPreference<Preference>(PREFERENCE_STATUSES_LOCATION)?.isVisible = false
                findPreference<Preference>(PREFERENCE_DEFAULT_CLIENT)?.isVisible = false
                findPreference<Preference>(PREFERENCE_GRANT_PERMISSIONS)?.apply {
                    isVisible = true
                    setOnPreferenceClickListener {
                        findActivityNavController(R.id.main_container)
                            .navigate(R.id.onboardFragment, bundleOf("isFromSettings" to true))
                        true
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    findPreference<Preference>(PREFERENCE_QUICK_DELETION)?.isVisible = false
                }
            }
        }
    }
}