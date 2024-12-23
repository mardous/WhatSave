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

import com.simplified.wsstatussaver.R

/**
 * @author Christians M. A. (mardous)
 */
enum class PlaybackSpeed(val labelRes: Int, val iconRes: Int, val speed: Float) {
    UltraSlow(R.string.ultra_slow, R.drawable.ic_speed_025_24dp, 0.25f),
    VerySlow(R.string.very_slow, R.drawable.ic_speed_05x_24px, 0.50f),
    Slow(R.string.slow, R.drawable.ic_speed_07x_24px, 0.75f),
    Normal(R.string.normal_speed, R.drawable.ic_speed_1x_24dp, 1f),
    Fast(R.string.fast, R.drawable.ic_speed_125_24px, 1.25f),
    VeryFast(R.string.very_fast, R.drawable.ic_speed_15x_24px, 1.50f),
    UltraFast(R.string.ultra_fast, R.drawable.ic_speed_175_24dp, 1.75f);

    fun next(): PlaybackSpeed {
        val values = PlaybackSpeed.entries
        val currentIndex = values.indexOf(this)
        val nextIndex = (currentIndex + 1) % values.size
        return values[nextIndex]
    }
}