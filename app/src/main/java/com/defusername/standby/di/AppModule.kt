package com.defusername.standby.di

import com.defusername.standby.data.repository.PowerStateRepositoryImpl
import com.defusername.standby.data.repository.SettingsRepositoryImpl
import com.defusername.standby.domain.repository.PowerStateRepository
import com.defusername.standby.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindPowerStateRepository(
        impl: PowerStateRepositoryImpl
    ): PowerStateRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
