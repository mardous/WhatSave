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

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StatusDao {
    @Insert
    fun saveStatus(status: StatusEntity): Long

    @Query("DELETE FROM saved_statuses WHERE save_name = :name")
    fun removeSave(name: String)

    @Query("DELETE FROM saved_statuses WHERE save_name IN (:names)")
    fun removeSaves(names: Set<String>)

    @Query("DELETE FROM saved_statuses WHERE status_type = :type")
    suspend fun removeSaves(type: Int)

    @Query("SELECT EXISTS(SELECT * FROM saved_statuses WHERE original_uri = :origin OR save_name = :name)")
    fun statusSaved(origin: Uri, name: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM saved_statuses WHERE original_uri = :origin OR save_name = :name)")
    fun statusSavedObservable(origin: Uri, name: String): LiveData<Boolean>

    @Query("SELECT * FROM saved_statuses WHERE status_type = :type")
    fun savedStatuses(type: Int): LiveData<List<StatusEntity>>
}