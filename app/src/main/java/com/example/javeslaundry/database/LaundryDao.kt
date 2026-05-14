package com.example.javeslaundry.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LaundryDao {

    // CLIENTES
    @Insert
    suspend fun insertarCliente(cliente: Cliente)

    @Query("SELECT * FROM clientes ORDER BY id DESC")
    fun obtenerClientes(): Flow<List<Cliente>>

    @Update
    suspend fun actualizarCliente(cliente: Cliente)

    // LAVADAS
    @Insert
    suspend fun insertarLavada(lavada: Lavada)

    @Query("SELECT * FROM lavadas ORDER BY id DESC")
    fun obtenerLavadas(): Flow<List<Lavada>>

    @Query("SELECT * FROM lavadas WHERE estadoEntrega = :estado ORDER BY fechaCreacion DESC")
    fun obtenerLavadasPorEstado(estado: String): Flow<List<Lavada>>

    @Query("SELECT * FROM lavadas WHERE fechaCreacion BETWEEN :inicio AND :fin ORDER BY fechaCreacion DESC")
    fun obtenerLavadasPorRangoFechas(inicio: Long, fin: Long): Flow<List<Lavada>>

    @Query("SELECT * FROM lavadas WHERE estadoEntrega = :estado AND fechaCreacion BETWEEN :inicio AND :fin ORDER BY fechaCreacion DESC")
    fun obtenerLavadasPorEstadoYRangoFechas(estado: String, inicio: Long, fin: Long): Flow<List<Lavada>>

    @Update
    suspend fun actualizarLavada(lavada: Lavada)

    @Delete
    suspend fun eliminarLavada(lavada: Lavada)

    // MOVIMIENTOS
    @Insert
    suspend fun insertarMovimiento(movimiento: Movimiento)

    @Update
    suspend fun actualizarMovimiento(movimiento: Movimiento)

    @Delete
    suspend fun eliminarMovimiento(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    fun obtenerMovimientos(): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos WHERE tipo = :tipo ORDER BY fecha DESC")
    fun obtenerMovimientosPorTipo(tipo: String): Flow<List<Movimiento>>

    // Conceptos únicos por tipo para autocompletado
    @Query("SELECT DISTINCT concepto FROM movimientos WHERE tipo = :tipo ORDER BY concepto ASC")
    fun obtenerConceptosPorTipo(tipo: String): Flow<List<String>>
}