package com.example.javeslaundry.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lavadas")
data class Lavada(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cliente: String,
    val tipoPrenda: String,
    val cantidad: Int,
    val precio: Double,
    val estadoPago: String = "Pendiente",
    val estadoEntrega: String = "Pendiente", // "Pendiente" o "Entregada"
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaEntrega: Long? = null
)