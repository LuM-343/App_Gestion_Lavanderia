package com.example.javeslaundry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LaundryDao {

    // CLIENTES
    @Insert
    suspend fun insertarCliente(cliente: Cliente)

    @Query("SELECT * FROM clientes ORDER BY id DESC")
    fun obtenerClientes(): Flow<List<Cliente>>

    // LAVADAS
    @Insert
    suspend fun insertarLavada(lavada: Lavada)

    @Query("SELECT * FROM lavadas ORDER BY id DESC")
    fun obtenerLavadas(): Flow<List<Lavada>>

    // MOVIMIENTOS
    @Insert
    suspend fun insertarMovimiento(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    fun obtenerMovimientos(): Flow<List<Movimiento>>

    @androidx.room.Update
    suspend fun actualizarCliente(cliente: Cliente)

    @androidx.room.Update
    suspend fun actualizarLavada(lavada: Lavada)
}