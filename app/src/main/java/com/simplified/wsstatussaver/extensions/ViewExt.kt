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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun RecyclerView.Adapter<*>?.isNullOrEmpty(): Boolean = this == null || this.itemCount == 0

fun ViewPager2.doOnPageSelected(lifecycleOwner: LifecycleOwner, onSelected: (position: Int) -> Unit) {
    val lifecycle = lifecycleOwner.lifecycle
    if (lifecycle.currentState == Lifecycle.State.DESTROYED) return
    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            onSelected(position)
        }
    }
    registerOnPageChangeCallback(callback)
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                unregisterOnPageChangeCallback(callback)
            }
        }
    })
}

fun ViewPager2.findCurrentFragment(fragmentManager: FragmentManager): Fragment? {
    return fragmentManager.findFragmentByTag("f$currentItem")
}