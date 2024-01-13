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

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.doOnPreDraw
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialFadeThrough
import com.simplified.wsstatussaver.*
import com.simplified.wsstatussaver.databinding.FragmentSettingsBinding
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.preferences.DefaultClientPreference
import com.simplified.wsstatussaver.preferences.DefaultClientPreferenceDialog
import com.simplified.wsstatussaver.preferences.StoragePreference
import com.simplified.wsstatussaver.preferences.StoragePreferenceDialog

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
        binding.appBar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(requireContext())
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
            invalidatePreferences()
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            when (preference) {
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
            findPreference<Preference>(PREFERENCE_NIGHT_MODE)
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
            findPreference<Preference>(PREFERENCE_LONG_PRESS_ACTION)
                ?.setOnPreferenceChangeListener { _: Preference?, o: Any ->
                    val actionName = o as String
                    if (LongPressAction.VALUE_DELETE == actionName) {
                        showToast(R.string.statuses_deletion_is_not_permitted)
                    }
                    logLongPressActionSelected(actionName)
                    true
                }
            findPreference<Preference>(PREFERENCE_QUICK_DELETION)?.isVisible = !hasR()
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
        }
    }
}