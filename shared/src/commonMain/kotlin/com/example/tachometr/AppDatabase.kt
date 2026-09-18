package com.example.tachometr

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

import androidx.room3.ConstructedBy
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [LocationPoint::class, PathSession::class], version = 2)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun sessionDao(): SessionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

// Očekáváme, že každá platforma dodá svůj vlastní builder
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class DatabaseBuilder {
    fun create(): RoomDatabase.Builder<AppDatabase>
}

private var dbInstance: AppDatabase? = null

// Společná funkce pro vytvoření instance s novým SQLite ovladačem
fun getDatabase(builder: DatabaseBuilder): AppDatabase {
    if (dbInstance == null) {
        dbInstance = builder.create()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    return dbInstance!!
}