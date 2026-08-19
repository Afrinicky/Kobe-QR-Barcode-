package com.kobe.qrbarcode.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CodeEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun codeDao(): CodeDao

    companion object {
        const val NAME = "kobe-codes.db"
    }
}
