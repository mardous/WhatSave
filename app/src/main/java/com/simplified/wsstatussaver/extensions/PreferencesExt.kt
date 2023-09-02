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
import androidx.annotation.StringDef
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

fun Context.preferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

fun Fragment.preferences() = requireContext().preferences()

fun SharedPreferences.themeChanged(lastThemeChanged: Long): Boolean {
    return getLong(PREFERENCE_THEME_CHANGED, -1) > lastThemeChanged
}

fun SharedPreferences.markThemeChanged() {
    edit {
        putLong(PREFERENCE_THEME_CHANGED, System.currentTimeMillis())
    }
}

fun SharedPreferences.getDefaultDayNightMode() = getDefaultDayNightMode(getString(PREFERENCE_NIGHT_MODE, null))

fun getDefaultDayNightMode(nightMode: String?): Int {
    if (nightMode != null) when (nightMode) {
        NightMode.VALUE_YES -> return AppCompatDelegate.MODE_NIGHT_YES
        NightMode.VALUE_NO -> return AppCompatDelegate.MODE_NIGHT_NO
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    } else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
}

fun SharedPreferences.isJustBlack() = getBoolean(PREFERENCE_JUST_BLACK_THEME, false)

fun SharedPreferences.isRequireSaveName() = getBoolean(PREFERENCE_REQUIRE_SAVE_NAME, false)

fun SharedPreferences.isExcludeOldStatuses() = getBoolean(PREFERENCE_EXCLUDE_OLD_STATUSES, false)

fun SharedPreferences.isExcludeSavedStatuses() = getBoolean(PREFERENCE_EXCLUDE_SAVED_STATUSES, false)

fun SharedPreferences.getLongPressAction() = getString(PREFERENCE_LONG_PRESS_ACTION, LongPressAction.VALUE_SAVE)!!

fun SharedPreferences.isWhatsappIcon() = getBoolean(PREFERENCE_WHATSAPP_ICON, false)

fun SharedPreferences.isQuickDeletion() = getBoolean(PREFERENCE_QUICK_DELETION, false)

var SharedPreferences.defaultClientPackageName: String?
    get() = getString(PREFERENCE_DEFAULT_CLIENT, null)
    set(packageName) = edit {
        putString(PREFERENCE_DEFAULT_CLIENT, packageName)
    }

@Retention(AnnotationRetention.SOURCE)
@StringDef(NightMode.VALUE_NO, NightMode.VALUE_YES)
annotation class NightMode {
    companion object {
        const val VALUE_YES = "yes"
        const val VALUE_NO = "no"
    }
}

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    LongPressAction.VALUE_MULTI_SELECTION, LongPressAction.VALUE_PREVIEW,
    LongPressAction.VALUE_SAVE, LongPressAction.VALUE_SHARE, LongPressAction.VALUE_DELETE
)
annotation class LongPressAction {
    companion object {
        const val VALUE_MULTI_SELECTION = "multi-selection"
        const val VALUE_PREVIEW = "preview"
        const val VALUE_SAVE = "save"
        const val VALUE_SHARE = "share"
        const val VALUE_DELETE = "delete"
    }
}

const val PREFERENCE_NIGHT_MODE = "night_mode"
const val PREFERENCE_JUST_BLACK_THEME = "just_black_theme"
const val PREFERENCE_COLOR_THEME = "color_theme"
const val PREFERENCE_THEME_CHANGED = "theme_changed"
const val PREFERENCE_STATUSES_LOCATION = "statuses_location"
const val PREFERENCE_LONG_PRESS_ACTION = "long_press_action"
const val PREFERENCE_LANGUAGE = "language_name"
const val PREFERENCE_WHATSAPP_ICON = "whatsapp_icon"
const val PREFERENCE_REQUIRE_SAVE_NAME = "require_save_name"
const val PREFERENCE_EXCLUDE_SAVED_STATUSES = "exclude_saved_statuses"
const val PREFERENCE_EXCLUDE_OLD_STATUSES = "exclude_old_statuses"
const val PREFERENCE_QUICK_DELETION = "quick_deletion"
const val PREFERENCE_DEFAULT_CLIENT = "default_client"
