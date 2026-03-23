package com.example.javeslaundry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LaundryDao {

    @Insert
    suspend fun insertarCliente(cliente: Cliente)

    @Query("SELECT * FROM clientes ORDER BY id DESC")
    fun obtenerClientes(): Flow<List<Cliente>>
}