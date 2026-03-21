package com.example.javeslaundry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javeslaundry.database.Servicio
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PantallaLavadas(
    servicios: List<Servicio>,
    onAgregarClick: () -> Unit,
    onServicioClick: (Servicio) -> Unit
) {
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Lavadas",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onAgregarClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("+ Agregar Lavada")
        }

        LazyColumn {
            items(servicios) { servicio ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onServicioClick(servicio) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = fmt.format(Date(servicio.fecha)),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${servicio.estado}  |  Q${servicio.precio}  |  ${servicio.pago}",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

