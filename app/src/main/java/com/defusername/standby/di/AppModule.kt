package com.defusername.standby.di

import com.defusername.standby.data.repository.PermissionRepositoryImpl
import com.defusername.standby.data.repository.PowerStateRepositoryImpl
import com.defusername.standby.data.repository.SettingsRepositoryImpl
import com.defusername.standby.data.notification.NotificationRepositoryImpl
import com.defusername.standby.domain.repository.PermissionRepository
import com.defusername.standby.domain.repository.PowerStateRepository
import com.defusername.standby.domain.repository.SettingsRepository
import com.defusername.standby.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        impl: PermissionRepositoryImpl
    ): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository

    companion object {
        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    }
}
