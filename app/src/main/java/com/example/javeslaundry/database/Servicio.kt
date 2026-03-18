package com.example.javeslaundry.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "servicios",
    foreignKeys = [ForeignKey(entity = Cliente::class, parentColumns = ["idCliente"], childColumns = ["idCliente"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idCliente")]
)
data class Servicio(
    @PrimaryKey(autoGenerate = true) val idServicio: Int = 0,
    val idCliente: Int,
    val fecha: Long,
    val precio: Double,
    val observaciones: String,
    val estado: String,
    val pago: String
)