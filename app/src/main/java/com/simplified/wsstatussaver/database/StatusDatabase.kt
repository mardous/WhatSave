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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [StatusEntity::class, MessageEntity::class], version = 2, exportSchema = false)
abstract class StatusDatabase : RoomDatabase() {
    abstract fun statusDao(): StatusDao
    abstract fun messageDao(): MessageDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE received_messages (" +
                    "message_id INTEGER NOT NULL, " +
                    "client_package TEXT, " +
                    "received_time INTEGER NOT NULL, " +
                    "received_from TEXT NOT NULL, " +
                    "message_content TEXT NOT NULL, " +
                    "PRIMARY KEY(message_id))"
        )
    }

}