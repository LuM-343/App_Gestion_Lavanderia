package com.example.javeslaundry.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val idCliente: Int = 0,
    val nombre: String,
    val telefono: Int,
    val direccion: String,
    val extra: String?
)