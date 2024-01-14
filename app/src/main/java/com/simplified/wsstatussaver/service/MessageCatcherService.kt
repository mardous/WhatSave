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
package com.simplified.wsstatussaver.service

import android.app.Notification
import android.app.Person
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.os.BundleCompat.getParcelable
import androidx.core.os.BundleCompat.getParcelableArrayList
import com.simplified.wsstatussaver.database.MessageEntity
import com.simplified.wsstatussaver.extensions.*
import com.simplified.wsstatussaver.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessageCatcherService : NotificationListenerService(), KoinComponent {

    private val repository: Repository by inject()
    private val serviceScope = CoroutineScope(Job() + Main)

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (hasN() && !preferences().isMessageViewEnabled) {
            requestUnbind()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || !preferences().isMessageViewEnabled) return
        serviceScope.launch(IO) {
            val client = getClientIfInstalled(sbn.packageName)
            if (client != null) {
                val extras = sbn.notification.extras
                if (!isSelf(extras)) {
                    val received = sbn.notification.`when`
                    val senderName = getSenderName(extras)
                    val message = extras.getString(Notification.EXTRA_TEXT)
                    if (!senderName.isNullOrBlank() && !message.isNullOrBlank()) {
                        if (!preferences().isBlacklistedMessageSender(senderName)) {
                            val messageEntity = MessageEntity(
                                clientPackage = client.packageName,
                                time = received,
                                senderName = senderName,
                                content = message
                            )
                            repository.insertMessage(messageEntity)
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isSelf(extras: Bundle): Boolean {
        if (hasN()) {
            if (hasP()) {
                val messagingUser = getParcelable(extras, Notification.EXTRA_MESSAGING_PERSON, Person::class.java)
                return messagingUser?.name == extras.getString(Notification.EXTRA_TITLE)
            } else {
                val selfDisplayName = extras.getString(Notification.EXTRA_SELF_DISPLAY_NAME)
                return selfDisplayName == extras.getString(Notification.EXTRA_TITLE)
            }
        }
        return false
    }

    private fun getSenderName(extras: Bundle): String? {
        val title = extras.getString(Notification.EXTRA_TITLE)
        if (hasP()) {
            val people = getParcelableArrayList(extras, Notification.EXTRA_PEOPLE_LIST, Person::class.java)
            return people?.firstOrNull()?.name?.toString() ?: title
        }
        return title
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}