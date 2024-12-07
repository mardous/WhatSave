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

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.view.drawToBitmap
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin

const val WHATSAVE_ANIM_TIME = 350L

fun View.isLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * Potentially animate showing a [NavigationBarView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot of the view, and animate this in, only changing the visibility (and
 * thus layout) when the animation completes.
 */
fun NavigationBarView.show() {
    if (this is NavigationRailView) return
    if (isVisible) return

    val parent = parent as ViewGroup
    // View needs to be laid out to create a snapshot & know position to animate. If view isn't
    // laid out yet, need to do this manually.
    if (!isLaidOut) {
        measure(
            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.AT_MOST)
        )
        layout(parent.left, parent.height - measuredHeight, parent.right, parent.height)
    }

    val drawable = BitmapDrawable(context.resources, drawToBitmap())
    drawable.setBounds(left, parent.height, right, parent.height + height)
    parent.overlay.add(drawable)
    ValueAnimator.ofInt(parent.height, top).apply {
        duration = WHATSAVE_ANIM_TIME
        interpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.accelerate_decelerate)
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(left, newTop, right, newTop + height)
        }
        doOnEnd {
            parent.overlay.remove(drawable)
            isVisible = true
        }
        start()
    }
}

/**
 * Potentially animate hiding a [BottomNavigationView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot, instantly hide the view (so content lays out to fill), then animate
 * out the snapshot.
 */
fun NavigationBarView.hide() {
    if (this is NavigationRailView) return
    if (isGone) return

    if (!isLaidOut) {
        isGone = true
        return
    }

    val drawable = BitmapDrawable(context.resources, drawToBitmap())
    val parent = parent as ViewGroup
    drawable.setBounds(left, top, right, bottom)
    parent.overlay.add(drawable)
    isGone = true
    ValueAnimator.ofInt(top, parent.height).apply {
        duration = WHATSAVE_ANIM_TIME
        interpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.accelerate_decelerate)
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(left, newTop, right, newTop + height)
        }
        doOnEnd {
            parent.overlay.remove(drawable)
        }
        start()
    }
}

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
            lifecycle.removeObserver(this)
        }
    })
}

fun ViewPager2.findCurrentFragment(fragmentManager: FragmentManager): Fragment? {
    return fragmentManager.findFragmentByTag("f$currentItem")
}

fun TextView.setMarkdownText(str: String) {
    val markwon = Markwon.builder(context)
        .usePlugin(HtmlPlugin.create())
        .build()

    markwon.setMarkdown(this, str)
}

fun CompoundButton.check(isChecked: Boolean) {
    post { this.isChecked = isChecked }
}