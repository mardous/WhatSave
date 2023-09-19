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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.activities.base.AbsBaseActivity
import com.simplified.wsstatussaver.databinding.ActivitySettingsBinding
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.preferences.DefaultClientPreference
import com.simplified.wsstatussaver.preferences.DefaultClientPreferenceDialog
import com.simplified.wsstatussaver.preferences.StoragePreference
import com.simplified.wsstatussaver.preferences.StoragePreferenceDialog
import com.simplified.wsstatussaver.setAnalyticsEnabled

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class SettingsActivity : AbsBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        binding.appBar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(this)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var settingsFragment: SettingsFragment? = whichFragment(R.id.settings_container)
        if (settingsFragment == null) {
            settingsFragment = SettingsFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, settingsFragment)
                .commit()
        } else {
            settingsFragment.invalidatePreferences()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            context?.preferences()?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            invalidatePreferences()
        }

        override fun onDestroyView() {
            context?.preferences()?.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroyView()
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

        override fun onSharedPreferenceChanged(p0: SharedPreferences?, keys: String?) {
            if (PREFERENCE_COLOR_THEME == keys) {
                themeChanged()
            }
        }

        private fun themeChanged() {
            activity?.preferences()?.markThemeChanged()
            activity?.recreate()
        }

        fun invalidatePreferences() {
            val nightMode = findPreference<Preference>(PREFERENCE_NIGHT_MODE)
            if (nightMode != null) {
                nightMode.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference?, o: Any? ->
                        AppCompatDelegate.setDefaultNightMode(getDefaultDayNightMode(o as String?))
                        themeChanged()
                        true
                    }
            }

            val justBlackTheme = findPreference<SwitchPreferenceCompat>(PREFERENCE_JUST_BLACK_THEME)
            if (justBlackTheme != null) {
                justBlackTheme.isEnabled = justBlackTheme.context.isNightModeEnabled
                justBlackTheme.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                    themeChanged()
                    true
                }
            }

            val longPressAction = findPreference<Preference>(PREFERENCE_LONG_PRESS_ACTION)
            if (longPressAction != null) {
                longPressAction.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference?, o: Any ->
                        val actionName = o as String
                        if (LongPressAction.VALUE_DELETE == actionName) {
                            Toast.makeText(
                                requireContext(),
                                R.string.statuses_deletion_is_not_permitted,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }
            }

            if (hasR()) {
                findPreference<Preference>(PREFERENCE_QUICK_DELETION)?.isVisible = false
            }

            findPreference<Preference>(PREFERENCE_LANGUAGE)?.setOnPreferenceChangeListener { _, newValue ->
                val languageName = newValue as? String
                if (languageName == "auto") {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageName))
                }
                activity?.recreate()
                true
            }

            findPreference<Preference>(PREFERENCE_ANALYTICS_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
                setAnalyticsEnabled((newValue as Boolean))
                true
            }
        }
    }
}