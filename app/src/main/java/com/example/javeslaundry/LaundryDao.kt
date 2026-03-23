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

    @Insert
    suspend fun insertarLavada(lavada: Lavada)

    @Query("SELECT * FROM lavadas ORDER BY id DESC")
    fun obtenerLavadas(): Flow<List<Lavada>>
}