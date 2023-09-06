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
package com.simplified.wsstatussaver.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.core.util.Predicate
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.CountryAdapter
import com.simplified.wsstatussaver.databinding.DialogMsgBinding
import com.simplified.wsstatussaver.databinding.DialogRecyclerviewBinding
import com.simplified.wsstatussaver.extensions.startActivitySafe
import com.simplified.wsstatussaver.interfaces.ICountryCallback
import com.simplified.wsstatussaver.model.Country
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.net.URLEncoder

class MsgDialog : DialogFragment(), ICountryCallback {

    private var _binding: DialogMsgBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatSaveViewModel by activityViewModel()

    private var adapter: CountryAdapter? = null
    private var countriesDialog: Dialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMsgBinding.inflate(layoutInflater)
        setupDialogView()
        createCountriesDialog()
        observeLiveData()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.msg_a_number)
            .setView(binding.root)
            .setPositiveButton(R.string.send_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        sendMessage(dialog)
                    }
                    viewModel.loadCountries()
                    viewModel.loadSelectedCountry()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onCountryClick(country: Country) {
        viewModel.setSelectedCountry(country)
        countriesDialog?.dismiss()
    }

    private fun observeLiveData() {
        viewModel.getCountriesObservable().observe(this) {
            adapter?.countries = it
        }
        viewModel.getSelectedCountryObservable().observe(this) {
            binding.phoneNumberInputLayout.prefixText = it.getId()
            adapter?.selectedCode = it.isoCode
        }
    }

    private fun setupDialogView() {
        binding.phoneNumberInputLayout.setEndIconOnClickListener {
            countriesDialog?.show()
        }
    }

    private fun createCountriesDialog() {
        adapter = CountryAdapter(requireContext(), viewModel.getCountries(), this)
        val binding = DialogRecyclerviewBinding.inflate(layoutInflater)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        countriesDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_a_country_title)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun sendMessage(dialog: Dialog) {
        val country = viewModel.getSelectedCountry() ?: return
        val phoneNumber = getFormattedPhoneNumber(country, binding.phoneNumber.text?.toString())
        if (phoneNumber == null) {
            Toast.makeText(requireContext(), R.string.phone_number_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        var message = binding.message.text?.toString()
        if (!message.isNullOrBlank()) {
            message = kotlin.runCatching { URLEncoder.encode(message, "UTF-8") }.getOrNull()
        }

        val sb = StringBuilder("https://api.whatsapp.com/send?phone=")
        sb.append(phoneNumber)
        if (!message.isNullOrBlank()) {
            sb.append("&text=").append(message)
        }

        startActivitySafe(Intent(Intent.ACTION_VIEW, sb.toString().toUri()))
        dialog.dismiss()
    }

    private fun getFormattedPhoneNumber(country: Country, number: String?): String? {
        if (number.isNullOrBlank() || !number.isDigitsOnly()) {
            return null
        }
        if (country.format != null) {
            val predicate = Predicate<String> { input -> country.format.count { it == 'X' } == input.length }
            if (!predicate.test(number)) {
                if (number.length >= 2) {
                    val trimmed = number.substring(1)
                    if (predicate.test(trimmed)) {
                        return "${country.getFormattedCode()}$trimmed"
                    }
                    return null
                }
                return null
            }
            return "${country.getFormattedCode()}$number"
        }
        return "${country.getFormattedCode()}$number"
    }
}