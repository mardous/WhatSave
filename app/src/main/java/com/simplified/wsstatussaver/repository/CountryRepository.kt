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
package com.simplified.wsstatussaver.repository

import android.content.Context
import androidx.core.content.edit
import com.simplified.wsstatussaver.extensions.preferences
import com.simplified.wsstatussaver.model.Country
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Locale

interface CountryRepository {
    suspend fun allCountries(): List<Country>
    suspend fun defaultCountry(): Country
    fun defaultCountry(country: Country)
    suspend fun isDefaultCountry(country: Country): Boolean
}

class CountryRepositoryImpl(private val context: Context) :
    CountryRepository {

    private val preferences = context.preferences()
    private var countries: List<Country>? = null

    override suspend fun allCountries(): List<Country> {
        if (countries != null) {
            return countries!!
        }

        countries = try {
            context.assets.open("countries.json").use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                Json.decodeFromString<List<Country>>(content).sortedBy { it.displayName }
            }
        } catch (e: IOException) {
            println("Error reading countries.json: ${e.message}")
            emptyList()
        } catch (e: SerializationException) {
            println("Error deserializing countries.json: ${e.message}")
            emptyList()
        }
        return countries!!
    }

    override suspend fun defaultCountry(): Country {
        var defaultCountry = preferences.getString(DEFAULT_COUNTRY_KEY, null)
        if (defaultCountry.isNullOrEmpty()) {
            defaultCountry = Locale.getDefault().country
        }
        val countries = allCountries()
        return if (countries.isEmpty()) {
            throw IllegalStateException("No countries available to set as default.")
        } else {
            countries.firstOrNull { it.isoCode == defaultCountry }
                ?: allCountries().first()
        }
    }

    override fun defaultCountry(country: Country) {
        preferences.edit {
            putString(DEFAULT_COUNTRY_KEY, country.isoCode)
        }
    }

    override suspend fun isDefaultCountry(country: Country): Boolean {
        return defaultCountry().isoCode == country.isoCode
    }

    companion object {
        private const val DEFAULT_COUNTRY_KEY = "default_country"
    }
}