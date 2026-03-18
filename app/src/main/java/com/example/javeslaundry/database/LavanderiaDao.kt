package com.example.javeslaundry.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface LavanderiaDao {
    @Insert
    suspend fun insertarCliente(cliente: Cliente): Long

    @Query("SELECT * FROM clientes")
    suspend fun obtenerClientes(): List<Cliente>
}