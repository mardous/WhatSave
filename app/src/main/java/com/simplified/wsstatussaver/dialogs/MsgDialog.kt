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
import android.telephony.PhoneNumberFormattingTextWatcher
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.WhatSaveViewModel
import com.simplified.wsstatussaver.adapter.CountryAdapter
import com.simplified.wsstatussaver.databinding.DialogMsgBinding
import com.simplified.wsstatussaver.databinding.DialogRecyclerviewBinding
import com.simplified.wsstatussaver.extensions.encodedUrl
import com.simplified.wsstatussaver.extensions.showToast
import com.simplified.wsstatussaver.extensions.startActivitySafe
import com.simplified.wsstatussaver.interfaces.ICountryCallback
import com.simplified.wsstatussaver.mediator.WAMediator
import com.simplified.wsstatussaver.model.Country
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MsgDialog : DialogFragment(), ICountryCallback {

    private var _binding: DialogMsgBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatSaveViewModel by activityViewModel()
    private val phoneNumberUtil: PhoneNumberUtil by inject()
    private val mediator: WAMediator by inject()

    private var adapter: CountryAdapter? = null
    private var countriesDialog: Dialog? = null
    private var numberFormatTextWatcher: PhoneNumberFormattingTextWatcher? = null

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
        viewModel.getSelectedCountryObservable().observe(this) { country ->
            numberFormatTextWatcher?.let { textWatcher ->
                binding.phoneNumber.removeTextChangedListener(textWatcher)
            }
            numberFormatTextWatcher = PhoneNumberFormattingTextWatcher(country.isoCode).also { textWatcher ->
                binding.phoneNumber.addTextChangedListener(textWatcher)
            }
            binding.phoneNumberInputLayout.prefixText = country.getId()
            adapter?.selectedCode = country.isoCode
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

    private fun formatInput(input: String?, country: Country): String? {
        val number = try {
            phoneNumberUtil.parse(input, country.isoCode)
        } catch (e: NumberParseException) {
            null
        }
        if (number == null || !phoneNumberUtil.isValidNumberForRegion(number, country.isoCode)) {
            return null
        }
        return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
    }

    private fun sendMessage(dialog: Dialog) {
        val entered = binding.phoneNumber.text?.toString()
        val country = viewModel.getSelectedCountry() ?: return
        val formattedNumber = formatInput(entered, country)
        if (formattedNumber == null) {
            showToast(R.string.phone_number_invalid)
            return
        }
        val encodedMessage = binding.message.text?.toString()?.encodedUrl()
        val apiRequest = StringBuilder("https://api.whatsapp.com/send?phone=")
        apiRequest.append(formattedNumber)
        if (!encodedMessage.isNullOrBlank()) {
            apiRequest.append("&text=").append(encodedMessage)
        }
        val intent = Intent(Intent.ACTION_VIEW, apiRequest.toString().toUri())
        val whatsappClient = mediator.getDefaultClientOrAny()
        if (whatsappClient != null) {
            intent.setPackage(whatsappClient.packageName)
        }
        startActivitySafe(intent) { _: Throwable, activityNotFound: Boolean ->
            if (activityNotFound) showToast(R.string.wa_is_not_installed_title)
        }
        dialog.dismiss()
    }
}