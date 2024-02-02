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

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.simplified.wsstatussaver.R
import com.simplified.wsstatussaver.interfaces.ICountryCallback
import com.simplified.wsstatussaver.model.Country
import org.koin.core.component.KoinComponent
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

@SuppressLint("NotifyDataSetChanged")
class CountryAdapter(
    private val context: Context,
    countries: List<Country>,
    private val callback: ICountryCallback,
) : RecyclerView.Adapter<CountryAdapter.ViewHolder>(), KoinComponent {

    var countries: List<Country> by Delegates.observable(countries) { _: KProperty<*>, _: List<Country>, _: List<Country> ->
        notifyDataSetChanged()
    }

    var selectedCode: String? by Delegates.observable(null) { _: KProperty<*>, _: String?, _: String? ->
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(context).inflate(R.layout.item_country, parent, false).let {
            ViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val country = countries[position]
        holder.codeView.text = country.getFormattedCode()
        holder.nameView.text = country.displayName
        holder.checkView.isVisible = country.isoCode == selectedCode
    }

    override fun getItemCount(): Int = countries.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        internal val codeView: TextView = itemView.findViewById(R.id.countryCode)
        internal val nameView: TextView = itemView.findViewById(R.id.countryName)
        internal val checkView: ImageView = itemView.findViewById(R.id.checkIcon)

        private val country: Country?
            get() = countries.getOrNull(layoutPosition)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val country = this.country ?: return
            callback.countryClick(country)
        }
    }
}