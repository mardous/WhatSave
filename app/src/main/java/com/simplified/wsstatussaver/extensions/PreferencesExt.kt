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
package com.simplified.wsstatussaver.extensions

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.getApp
import com.simplified.wsstatussaver.model.PlaybackSpeed
import com.simplified.wsstatussaver.model.SaveLocation

private fun appStr(resId: Int) = getApp().applicationContext.getString(resId)

fun Context.preferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

fun Fragment.preferences() = requireContext().preferences()

fun SharedPreferences.useCustomFont() = getBoolean(PREFERENCE_USE_CUSTOM_FONT, true)

fun SharedPreferences.getDefaultDayNightMode() = getDefaultDayNightMode(getString(PREFERENCE_THEME_MODE, null))

fun getDefaultDayNightMode(nightMode: String?): Int {
    if (nightMode != null) when (nightMode) {
        NightMode.VALUE_DARK -> return AppCompatDelegate.MODE_NIGHT_YES
        NightMode.VALUE_LIGHT -> return AppCompatDelegate.MODE_NIGHT_NO
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    } else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
}

fun SharedPreferences.isJustBlack() = getBoolean(PREFERENCE_JUST_BLACK_THEME, false)

fun SharedPreferences.blacklistedSenders(): Set<String>? = getStringSet(BLACKLISTED_MESSAGE_SENDERS, null)

fun SharedPreferences.blacklistMessageSender(name: String) {
    var namesSet = getStringSet(BLACKLISTED_MESSAGE_SENDERS, null)
    if (namesSet == null) {
        edit { putStringSet(BLACKLISTED_MESSAGE_SENDERS, hashSetOf(name)) }
    } else {
        namesSet = namesSet.toMutableSet()
        namesSet.add(name)
        edit { putStringSet(BLACKLISTED_MESSAGE_SENDERS, namesSet) }
    }
}

fun SharedPreferences.whitelistMessageSender(name: String) {
    var namesSet = getStringSet(BLACKLISTED_MESSAGE_SENDERS, null)
    if (namesSet == null) {
        edit { putStringSet(BLACKLISTED_MESSAGE_SENDERS, hashSetOf(name)) }
    } else {
        namesSet = namesSet.toMutableSet()
        namesSet.remove(name)
        edit { putStringSet(BLACKLISTED_MESSAGE_SENDERS, namesSet) }
    }
}

fun SharedPreferences.isBlacklistedMessageSender(name: String): Boolean {
    val namesSet = getStringSet(BLACKLISTED_MESSAGE_SENDERS, emptySet())
    if (namesSet.isNullOrEmpty()) return false
    return namesSet.contains(name)
}

var SharedPreferences.isMessageViewEnabled
    get() = getBoolean(PREFERENCE_ENABLE_MESSAGE_VIEW, false)
    set(value) = edit {
        putBoolean(PREFERENCE_ENABLE_MESSAGE_VIEW, value)
    }

fun SharedPreferences.isExcludeSavedStatuses() = getBoolean(PREFERENCE_EXCLUDE_SAVED_STATUSES, false)

fun SharedPreferences.isWhatsappIcon() = getBoolean(PREFERENCE_WHATSAPP_ICON, false)

fun SharedPreferences.isQuickDeletion() = getBoolean(PREFERENCE_QUICK_DELETION, false)

fun SharedPreferences.getUpdateSearchMode() = getString(PREFERENCE_UPDATE_SEARCH_MODE, appStr(R.string.auto_update_policy))

fun SharedPreferences.isUpdateOnlyWifi() = getBoolean(PREFERENCE_UPDATE_ONLY_WIFI, false)

fun SharedPreferences.isAnalyticsEnabled() = getBoolean(PREFERENCE_ANALYTICS_ENABLED, true)

var SharedPreferences.isShownOnboard: Boolean
    get() = getBoolean(SHOULD_SHOW_ONBOARD, true)
    set(value) = edit {
        putBoolean(SHOULD_SHOW_ONBOARD, value)
    }

var SharedPreferences.lastUpdateSearch: Long
    get() = getLong(PREFERENCE_LAST_UPDATE_SEARCH, -1)
    set(value) = edit {
        putLong(PREFERENCE_LAST_UPDATE_SEARCH, value)
    }

var SharedPreferences.lastUpdateId: Long
    get() = getLong(PREFERENCE_LAST_UPDATE_ID, -1)
    set(value) = edit {
        putLong(PREFERENCE_LAST_UPDATE_ID, value)
    }

var SharedPreferences.defaultClientPackageName: String?
    get() = getString(PREFERENCE_DEFAULT_CLIENT, null)
    set(packageName) = edit {
        putString(PREFERENCE_DEFAULT_CLIENT, packageName)
    }

var SharedPreferences.saveLocation
    get() = getString(PREFERENCE_SAVE_LOCATION, null)?.toEnum<SaveLocation>() ?: SaveLocation.DCIM
    set(value) = edit { putString(PREFERENCE_SAVE_LOCATION, value.name) }

var SharedPreferences.playbackSpeed
    get() = getString(PREFERENCE_PLAYBACK_SPEED, null)?.toEnum<PlaybackSpeed>() ?: PlaybackSpeed.Normal
    set(value) = edit { putString(PREFERENCE_PLAYBACK_SPEED, value.name) }

fun SharedPreferences.bumpPlaybackSpeed(): PlaybackSpeed {
    return playbackSpeed.next().also { playbackSpeed = it }
}

fun SharedPreferences.resetPlaybackSpeed(): PlaybackSpeed {
    return PlaybackSpeed.Normal.also { playbackSpeed = it }
}

fun SharedPreferences.migratePreferences() {
    if (contains("night_mode")) {
        val oldValue = getString("night_mode", null)
        edit {
            val newValue = when (oldValue) {
                "yes" -> NightMode.VALUE_DARK
                "no" -> NightMode.VALUE_LIGHT
                else -> null
            }
            putString(PREFERENCE_THEME_MODE, newValue)
            remove("night_mode")
        }
    }
}

class NightMode {
    companion object {
        const val VALUE_DARK = "dark"
        const val VALUE_LIGHT = "light"
    }
}

class UpdateSearchMode {
    companion object {
        const val EVERY_DAY = "every_day"
        const val WEEKLY = "weekly"
        const val EVERY_FIFTEEN_DAYS = "every_fifteen_days"
        const val MONTHLY = "monthly"
        const val NEVER = "never"
    }
}

const val SHOULD_SHOW_ONBOARD = "should_show_onboard"
const val PREFERENCE_USE_CUSTOM_FONT = "use_custom_font"
const val PREFERENCE_THEME_MODE = "theme_mode"
const val PREFERENCE_JUST_BLACK_THEME = "just_black_theme"
const val PREFERENCE_STATUSES_LOCATION = "statuses_location"
const val PREFERENCE_LANGUAGE = "language_name"
const val PREFERENCE_WHATSAPP_ICON = "whatsapp_icon"
const val PREFERENCE_EXCLUDE_SAVED_STATUSES = "exclude_saved_statuses"
const val PREFERENCE_SAVE_LOCATION = "preferred_save_location"
const val PREFERENCE_QUICK_DELETION = "quick_deletion"
const val PREFERENCE_DEFAULT_CLIENT = "default_client"
const val PREFERENCE_GRANT_PERMISSIONS = "grant_permissions"
const val PREFERENCE_UPDATE_SEARCH_MODE = "update_search_mode"
const val PREFERENCE_UPDATE_ONLY_WIFI = "update_only_wifi"
const val PREFERENCE_LAST_UPDATE_SEARCH = "last_update_search"
const val PREFERENCE_LAST_UPDATE_ID = "last_update_id"
const val PREFERENCE_ANALYTICS_ENABLED = "analytics_enabled"
const val BLACKLISTED_MESSAGE_SENDERS = "blacklisted_message_senders"
const val PREFERENCE_ENABLE_MESSAGE_VIEW = "enable_message_view"
const val PREFERENCE_PLAYBACK_SPEED = "playback_speed"