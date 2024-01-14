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
package com.simplified.wsstatussaver.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.simplified.wsstatussaver.model.StatusType

@Entity(tableName = "saved_statuses")
class StatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "status_type")
    val type: StatusType,
    @ColumnInfo(name = "original_name")
    val name: String,
    @ColumnInfo(name = "original_path")
    val origin: String,
    @ColumnInfo(name = "original_date_modified")
    val dateModified: Long,
    @ColumnInfo(name = "original_size")
    val size: Long,
    @ColumnInfo(name = "original_client")
    val client: String?,
    @ColumnInfo(name = "save_name")
    val saveName: String
)