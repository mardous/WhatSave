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
package com.simplified.wsstatussaver.adapter

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.simplified.wsstatussaver.model.StatusType
import com.simplified.wsstatussaver.fragments.base.AbsPagerFragment

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class PagerAdapter(private val fragment: Fragment, private val fragmentClassName: String) :
    FragmentStateAdapter(fragment) {

    private val mFragments: MutableList<FragmentHolder> = ArrayList()
    private val mFragmentFactory: FragmentFactory = fragment
        .childFragmentManager
        .fragmentFactory

    override fun getItemCount(): Int {
        return mFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        val mCurrentHolder = mFragments[position]
        return mFragmentFactory.instantiate(fragment.requireContext().classLoader, mCurrentHolder.className!!).apply {
            arguments = mCurrentHolder.arguments
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return mFragments[position].title!!
    }

    private class FragmentHolder {
        var className: String? = null
        var arguments: Bundle? = null
        var title: String? = null
    }

    init {
        for (type in StatusType.values()) {
            val holder = FragmentHolder().apply {
                className = fragmentClassName
                arguments = bundleOf(AbsPagerFragment.EXTRA_TYPE to type)
                title = fragment.getString(type.nameRes)
            }
            mFragments.add(holder)
        }
    }
}