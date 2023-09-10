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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.simplified.wsstatussaver.database.StatusDatabase
import com.simplified.wsstatussaver.mediator.WAMediator
import com.simplified.wsstatussaver.repository.*
import com.simplified.wsstatussaver.storage.Storage
import com.simplified.wsstatussaver.update.provideDefaultCache
import com.simplified.wsstatussaver.update.provideOkHttp
import com.simplified.wsstatussaver.update.provideUpdateService
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

private val firebaseModule = module {
    single {
        FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }
}

private val networkModule = module {
    factory {
        provideDefaultCache()
    }
    factory {
        provideOkHttp(get(), get())
    }
    single {
        provideUpdateService(get())
    }
}

private val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), StatusDatabase::class.java, "statuses.db")
            .build()
    }

    factory {
        get<StatusDatabase>().statusDao()
    }
}

private val managerModule = module {
    single {
        WAMediator(androidContext())
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
        StatusesRepositoryImpl(androidContext(), get(), get(), get())
    } bind StatusesRepository::class

    single {
        RepositoryImpl(get(), get())
    } bind Repository::class
}

private val viewModelModule = module {
    viewModel {
        WhatSaveViewModel(get(), get(), get(), get())
    }
}

val appModules = listOf(firebaseModule, networkModule, dataModule, managerModule, statusesModule, viewModelModule)