package com.example.javeslaundry

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMovimientos(
    dao: LaundryDao,
    onVolver: () -> Unit,
    onAgregarMovimiento: () -> Unit
) {
    val movimientos by dao.obtenerMovimientos().collectAsState(initial = emptyList())

    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Movimientos", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            Button(
                onClick = onAgregarMovimiento,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AGREGAR MOVIMIENTO")
            }

            Spacer(modifier = Modifier.height(16.dp))

            EncabezadoTablaMovimientos()

            LazyColumn {
                var balance = 0.0

                items(movimientos.reversed()) { movimiento ->

                    if (movimiento.tipo == "ingreso") {
                        balance += movimiento.monto
                    } else {
                        balance -= movimiento.monto
                    }

                    FilaMovimiento(
                        fecha = formatoFecha.format(Date(movimiento.fecha)),
                        concepto = movimiento.concepto,
                        ingreso = if (movimiento.tipo == "ingreso") movimiento.monto else null,
                        egreso = if (movimiento.tipo == "egreso") movimiento.monto else null,
                        balance = balance
                    )
                }
            }
        }
    }
}

@Composable
fun EncabezadoTablaMovimientos() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp)
    ) {
        Text("Fecha", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Concepto", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Ingreso", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Egreso", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Balance", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilaMovimiento(
    fecha: String,
    concepto: String,
    ingreso: Double?,
    egreso: Double?,
    balance: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(8.dp)
    ) {
        Text(fecha, modifier = Modifier.weight(2f))
        Text(concepto, modifier = Modifier.weight(2f))
        Text(ingreso?.let { "Q $it" } ?: "-", modifier = Modifier.weight(2f))
        Text(egreso?.let { "Q $it" } ?: "-", modifier = Modifier.weight(2f))
        Text("Q $balance", modifier = Modifier.weight(2f))
    }
}