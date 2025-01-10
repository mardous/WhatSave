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
package com.simplified.wsstatussaver.fragments.binding

import com.simplified.wsstatussaver.databinding.FragmentMessageANumberBinding

class MessageBinding(binding: FragmentMessageANumberBinding) {
    val toolbar = binding.toolbar
    val scrollView = binding.scrollView
    val phoneInputLayout = binding.messageANumberContent.phoneNumberInputLayout
    val phoneNumber = binding.messageANumberContent.phoneNumber
    val message = binding.messageANumberContent.message
    val shareButton = binding.messageANumberContent.shareButton
    val sendButton = binding.messageANumberContent.sendButton
}