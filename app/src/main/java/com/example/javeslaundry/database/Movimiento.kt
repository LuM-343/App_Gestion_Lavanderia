package com.example.javeslaundry.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos")
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val idMovimiento: Int = 0,
    val concepto: String,
    val tipo: String,
    val monto: Double,
    val fecha: Long
)