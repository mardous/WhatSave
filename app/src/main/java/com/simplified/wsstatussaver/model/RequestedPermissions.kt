/*
 * Copyright (C) 2024 Christians Martínez Alvarado
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
package com.simplified.wsstatussaver.model

import android.os.Build

/**
 * Created by Christians (https://github.com/mardous)
 * Project: WhatSave Status Saver
 */
class RequestedPermissions(private val versions: IntRange, vararg val permissions: String) {

    constructor(version: Int, vararg permissions: String) : this(version..version, *permissions)

    fun isApplicable(): Boolean {
        if (versions.isEmpty()) {
            return false
        }
        val sdk = Build.VERSION.SDK_INT
        return sdk >= versions.first && sdk <= versions.last
    }
}