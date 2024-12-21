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

import com.simplified.wsstatussaver.databinding.FragmentStatusesBinding

class StatusesBinding(binding: FragmentStatusesBinding) {
    val toolbar = binding.toolbar
    val collapsingToolbar = binding.collapsingToolbarLayout
    val swipeRefreshLayout = binding.swipeRefreshLayout
    val recyclerView = binding.recyclerView
    val emptyView = binding.emptyView.root
    val emptyTitle = binding.emptyView.emptyTitle
    val emptyText = binding.emptyView.emptyText
    val emptyButton = binding.emptyView.emptyButton
}