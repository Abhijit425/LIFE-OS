package com.lifeos.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.lifeos.app.core.time.SystemTimeProvider
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.Migrations
import com.lifeos.app.data.local.dao.UserProfileDao
import com.lifeos.app.data.repository.DashboardRepositoryImpl
import com.lifeos.app.data.repository.PreferencesRepositoryImpl
import com.lifeos.app.data.repository.ProfileRepositoryImpl
import com.lifeos.app.domain.repository.DashboardRepository
import com.lifeos.app.domain.repository.PreferencesRepository
import com.lifeos.app.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeOsDatabase =
        Room.databaseBuilder(context, LifeOsDatabase::class.java, LifeOsDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            // No fallbackToDestructiveMigration: a missing migration must fail loudly in testing,
            // never silently wipe the user's life data.
            .build()

    @Provides
    fun provideUserProfileDao(db: LifeOsDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("lifeos_settings") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
    @Binds abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
    @Binds abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
