package com.example.javeslaundry

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
    val estadoPago: String = "Pendiente" // Nuevo campo agregado
)