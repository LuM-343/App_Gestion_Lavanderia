package com.example.javeslaundry.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Cliente::class, Servicio::class, Movimiento::class], version = 1, exportSchema = false)
abstract class LavanderiaDatabase : RoomDatabase() {

    abstract fun lavanderiaDao(): LavanderiaDao

    companion object {
        @Volatile
        private var INSTANCE: LavanderiaDatabase? = null

        fun getDatabase(context: Context): LavanderiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LavanderiaDatabase::class.java,
                    "lavanderia_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}