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
import android.view.Window
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.annotation.IdRes
import androidx.annotation.IntegerRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment

fun Fragment.requireWindow(): Window = requireActivity().window

fun Fragment.dip(@DimenRes res: Int) = resources.getDimensionPixelSize(res)

fun Fragment.getIntRes(@IntegerRes int: Int): Int {
    return resources.getInteger(int)
}

fun Fragment.findActivityNavController(id: Int) = requireActivity().findNavController(id)

@Suppress("UNCHECKED_CAST")
fun <T> FragmentActivity.whichFragment(@IdRes id: Int): T {
    return supportFragmentManager.findFragmentById(id) as T
}

@Suppress("UNCHECKED_CAST")
fun <T> Fragment.whichFragment(@IdRes id: Int): T {
    return childFragmentManager.findFragmentById(id) as T
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

fun Fragment.getOnBackPressedDispatcher() = requireActivity().onBackPressedDispatcher

fun Fragment.currentFragment(navHostId: Int): Fragment? {
    val navHostFragment: NavHostFragment =
        childFragmentManager.findFragmentById(navHostId) as NavHostFragment
    return navHostFragment.childFragmentManager.fragments.firstOrNull()
}