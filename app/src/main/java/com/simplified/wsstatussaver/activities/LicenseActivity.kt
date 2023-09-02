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
package com.simplified.wsstatussaver.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import com.simplified.wsstatussaver.databinding.ActivityLicenseBinding
import com.simplified.wsstatussaver.extensions.isNightModeEnabled
import com.simplified.wsstatussaver.extensions.lightenColor
import com.simplified.wsstatussaver.extensions.primaryColor
import com.simplified.wsstatussaver.extensions.surfaceColor
import com.simplified.wsstatussaver.activities.base.AbsBaseActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class LicenseActivity : AbsBaseActivity() {

    private lateinit var binding: ActivityLicenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        try {
            val buf = StringBuilder()
            val json = assets.open("licenses.html")
            BufferedReader(InputStreamReader(json, StandardCharsets.UTF_8)).use { br ->
                var str: String?
                while (br.readLine().also { str = it } != null) {
                    buf.append(str)
                }
            }

            // Inject color values for WebView body background and links
            val isDark = isNightModeEnabled
            val backgroundColor = colorToCSS(surfaceColor(Color.parseColor(if (isDark) "#424242" else "#ffffff")))
            val contentColor = colorToCSS(Color.parseColor(if (isDark) "#ffffff" else "#000000"))
            val changeLog = buf.toString()
                .replace("{style-placeholder}",
                    String.format("body { background-color: %s; color: %s; }", backgroundColor, contentColor)
                )
                .replace("{link-color}", colorToCSS(primaryColor()))
                .replace("{link-color-active}", colorToCSS(primaryColor().lightenColor))
            binding.license.loadData(changeLog, "text/html", "UTF-8")
        } catch (e: Throwable) {
            binding.license.loadData("<h1>Unable to load</h1><p>" + e.localizedMessage + "</p>", "text/html", "UTF-8")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun colorToCSS(color: Int): String {
        return String.format(
            "rgb(%d, %d, %d)",
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        ) // on API 29, WebView doesn't load with hex colors
    }
}