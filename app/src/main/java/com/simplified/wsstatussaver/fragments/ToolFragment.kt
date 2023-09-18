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
import android.view.View
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.databinding.FragmentToolBinding
import com.simplified.wsstatussaver.dialogs.MsgDialog
import com.simplified.wsstatussaver.fragments.base.BaseFragment
import com.simplified.wsstatussaver.logToolView

class ToolFragment : BaseFragment(R.layout.fragment_tool) {

    private var _binding: FragmentToolBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentToolBinding.bind(view)
        _binding!!.msgANumber.setOnClickListener {
            logToolView("MsgDialog", "Message a number")
            MsgDialog().show(childFragmentManager, "SEND_MSG")
        }
        statusesActivity.setSupportActionBar(binding.toolbar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}