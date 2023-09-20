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

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment

fun Fragment.findActivityNavController(id: Int) = requireActivity().findNavController(id)

@Suppress("UNCHECKED_CAST")
fun <T : Fragment?> FragmentActivity.whichFragment(containerId: Int): T? {
    if (containerId != View.NO_ID) {
        val fragment = supportFragmentManager.findFragmentById(containerId) ?: return null
        return fragment as T
    }
    return null
}

fun Fragment.requestContext(consumer: ContextConsumer) {
    val context = context ?: return
    consumer(context)
}

fun Fragment.requestView(consumer: ViewConsumer) {
    val view = view ?: return
    consumer(view)
}

/**
 * Tries to start an activity using the given [Intent],
 * handling the case where the activity cannot be found.
 */
fun Fragment.startActivitySafe(intent: Intent?, onError: ExceptionConsumer? = null) {
    intent.doWithIntent(onError) { startActivity(it) }
}

fun Fragment.showToast(messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    context?.showToast(messageRes, duration)
}

fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    context?.showToast(message, duration)
}

fun AppCompatActivity.currentFragment(navHostId: Int): Fragment? {
    val navHostFragment: NavHostFragment =
        supportFragmentManager.findFragmentById(navHostId) as NavHostFragment
    return navHostFragment.childFragmentManager.fragments.firstOrNull()
}