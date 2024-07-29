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

import androidx.lifecycle.LiveData
import com.simplified.wsstatussaver.database.Conversation
import com.simplified.wsstatussaver.database.MessageDao
import com.simplified.wsstatussaver.database.MessageEntity
import com.simplified.wsstatussaver.extensions.time

interface MessageRepository {
    fun listConversations(): LiveData<List<Conversation>>
    fun listMessages(sender: String): LiveData<List<MessageEntity>>
    fun textFromConversations(conversations: List<Conversation>): String
    fun textFromMessages(messages: List<MessageEntity>): String
    suspend fun insertMessage(message: MessageEntity): Long
    suspend fun removeMessage(message: MessageEntity)
    suspend fun removeMessages(messages: List<MessageEntity>)
    suspend fun deleteConversations(conversations: List<String>)
    suspend fun clearMessages()
}

class MessageRepositoryImpl(private val messageDao: MessageDao) : MessageRepository {

    override fun listConversations(): LiveData<List<Conversation>> =
        messageDao.queryConversations()

    override fun listMessages(sender: String): LiveData<List<MessageEntity>> =
        messageDao.queryMessages(sender)

    override fun textFromConversations(conversations: List<Conversation>): String {
        val messages = conversations.flatMap {
            messageDao.listMessages(it.name)
        }
        return textFromMessages(messages)
    }

    override fun textFromMessages(messages: List<MessageEntity>): String {
        val sb = StringBuilder()
        val groups = messages.groupBy { it.senderName }
        for (group in groups) {
            val sortedMessages = group.value.sortedByDescending { it.time }
            sb.appendLine()
                .append(group.key.uppercase())
                .append(":")
                .append("\n\n")
            for (message in sortedMessages) {
                sb.append(message.time.time(maxPrettyTime = 0, useTimeFormat = true))
                    .append(": ")
                    .append(message.content.trim())
                    .append("\n\n")
            }
        }
        return sb.toString()
    }

    override suspend fun insertMessage(message: MessageEntity) =
        messageDao.insetMessage(message)

    override suspend fun removeMessage(message: MessageEntity) =
        messageDao.removeMessage(message)

    override suspend fun removeMessages(messages: List<MessageEntity>) {
        messageDao.removeMessages(messages)
    }

    override suspend fun deleteConversations(conversations: List<String>) {
        if (conversations.isNotEmpty()) for (conversation in conversations) {
            messageDao.deleteConversation(conversation)
        }
    }

    override suspend fun clearMessages() {
        messageDao.clearMessages()
    }
}