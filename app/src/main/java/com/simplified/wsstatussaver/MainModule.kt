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
package com.simplified.wsstatussaver

import androidx.room.Room
import com.simplified.wsstatussaver.database.MIGRATION_1_2
import com.simplified.wsstatussaver.database.StatusDatabase
import com.simplified.wsstatussaver.network.ktorHttpClient
import com.simplified.wsstatussaver.repository.CountryRepository
import com.simplified.wsstatussaver.repository.CountryRepositoryImpl
import com.simplified.wsstatussaver.repository.MessageRepository
import com.simplified.wsstatussaver.repository.MessageRepositoryImpl
import com.simplified.wsstatussaver.repository.Repository
import com.simplified.wsstatussaver.repository.RepositoryImpl
import com.simplified.wsstatussaver.repository.StatusesRepository
import com.simplified.wsstatussaver.repository.StatusesRepositoryImpl
import com.simplified.wsstatussaver.storage.Storage
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

private val networkModule = module {
    single {
        ktorHttpClient()
    }
}

private val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), StatusDatabase::class.java, "statuses.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    factory {
        get<StatusDatabase>().statusDao()
    }

    factory {
        get<StatusDatabase>().messageDao()
    }
}

private val managerModule = module {
    single {
        PhoneNumberUtil.createInstance(androidContext())
    }
    single {
        Storage(androidContext())
    }
}

private val statusesModule = module {
    single {
        CountryRepositoryImpl(androidContext())
    } bind CountryRepository::class

    single {
        StatusesRepositoryImpl(androidContext(), get(), get())
    } bind StatusesRepository::class

    single {
        MessageRepositoryImpl(get())
    } bind MessageRepository::class

    single {
        RepositoryImpl(get(), get(), get())
    } bind Repository::class
}

private val viewModelModule = module {
    viewModel {
        WhatSaveViewModel(get(), get(), get())
    }
}

val appModules = listOf(networkModule, dataModule, managerModule, statusesModule, viewModelModule)