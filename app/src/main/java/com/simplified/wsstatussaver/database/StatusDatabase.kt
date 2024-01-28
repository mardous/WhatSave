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
import androidx.core.net.toUri
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplified.wsstatussaver.model.StatusType

@Database(entities = [StatusEntity::class, MessageEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StatusDatabase : RoomDatabase() {
    abstract fun statusDao(): StatusDao
    abstract fun messageDao(): MessageDao
}

class Converters {
    @TypeConverter
    fun fromStatusTypeToString(statusType: StatusType) = statusType.name

    @TypeConverter
    fun toStatusTypeFromString(str: String) = enumValueOf<StatusType>(str)

    @TypeConverter
    fun fromUriToString(uri: Uri) = uri.toString()

    @TypeConverter
    fun toUriFromString(str: String) = str.toUri()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE saved_statuses")
        db.execSQL(
            "CREATE TABLE saved_statuses (" +
                    "status_id INTEGER NOT NULL, " +
                    "status_type TEXT NOT NULL, " +
                    "original_name TEXT, " +
                    "original_uri TEXT NOT NULL, " +
                    "original_date_modified INTEGER NOT NULL, " +
                    "original_size INTEGER NOT NULL, " +
                    "original_client TEXT, " +
                    "save_name TEXT NOT NULL, " +
                    "PRIMARY KEY(status_id))"
        )

        db.execSQL(
            "CREATE TABLE received_messages (" +
                    "message_id INTEGER NOT NULL, " +
                    "client_package TEXT, " +
                    "received_time INTEGER NOT NULL, " +
                    "received_from TEXT NOT NULL, " +
                    "message_content TEXT NOT NULL, " +
                    "PRIMARY KEY(message_id))"
        )

        db.execSQL(
            "CREATE UNIQUE INDEX messages_index ON received_messages (received_time, received_from, message_content)"
        )
    }
}