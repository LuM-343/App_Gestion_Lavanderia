package com.example.javeslaundry.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos")
data class Movimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: Long = System.currentTimeMillis(),
    val concepto: String,
    val descripcion: String = "",
    val monto: Double,
    val tipo: String,
    val categoria: String = "General"
)