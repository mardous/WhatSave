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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    fun insetMessage(messageEntity: MessageEntity): Long

    @Delete
    fun removeMessage(messageEntity: MessageEntity)

    @Query("DELETE FROM received_messages WHERE received_from = :sender")
    fun deleteConversation(sender: String)

    @Query("SELECT received_from AS name," +
            "COUNT(message_id) AS message_count," +
            "MAX(message_content) AS latest_message," +
            "MAX(received_time) AS latest_message_time " +
            "FROM received_messages GROUP BY received_from ORDER BY received_time DESC")
    fun queryConversations(): LiveData<List<Conversation>>

    @Query("SELECT * FROM received_messages WHERE received_from = :sender ORDER BY received_time DESC")
    fun queryMessages(sender: String): LiveData<List<MessageEntity>>

    @Query("DELETE FROM received_messages")
    fun clearMessages()
}