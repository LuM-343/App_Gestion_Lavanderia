package com.example.javeslaundry

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.javeslaundry.database.LaundryDao
import java.text.SimpleDateFormat
import java.util.*

// Verde  #2E7D32  → r=46,  g=125, b=50
// Rojo   #C62828  → r=198, g=40,  b=40
// FondoV #E8F5E9  → r=232, g=245, b=233
// FondoR #FFEBEE  → r=255, g=235, b=238
val LaundryVerde      = Color(red = 46,  green = 125, blue = 50)
val LaundryRojo       = Color(red = 198, green = 40,  blue = 40)
val LaundryFondoVerde = Color(red = 232, green = 245, blue = 233)
val LaundryFondoRojo  = Color(red = 255, green = 235, blue = 238)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMovimientos(
    dao: LaundryDao,
    onVolver: () -> Unit,
    onAgregarMovimiento: () -> Unit
) {
    val movimientosDesc by dao.obtenerMovimientos().collectAsState(initial = emptyList())
    val movimientosAsc  = movimientosDesc.reversed()

    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Calcular balance acumulado en orden cronológico
    val balances    = mutableListOf<Double>()
    var acumulado   = 0.0
    for (mov in movimientosAsc) {
        acumulado += if (mov.tipo == "ingreso") mov.monto else -mov.monto
        balances.add(acumulado)
    }

    val filas        = movimientosAsc.zip(balances).reversed()
    val balanceFinal = balances.lastOrNull() ?: 0.0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Movimientos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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

            Spacer(modifier = Modifier.height(12.dp))

            // Tarjeta balance actual
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (balanceFinal >= 0) LaundryFondoVerde else LaundryFondoRojo
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text       = "Balance actual",
                        fontWeight = FontWeight.SemiBold,
                        style      = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text       = "Q ${"%.2f".format(balanceFinal)}",
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium,
                        color      = if (balanceFinal >= 0) LaundryVerde else LaundryRojo
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            EncabezadoTablaMovimientos()

            LazyColumn {
                items(filas) { fila ->
                    val mov     = fila.first
                    val balance = fila.second
                    FilaMovimiento(
                        fecha    = formatoFecha.format(Date(mov.fecha)),
                        concepto = mov.concepto,
                        ingreso  = if (mov.tipo == "ingreso") mov.monto else null,
                        egreso   = if (mov.tipo == "egreso")  mov.monto else null,
                        balance  = balance
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
        Text("Fecha",    modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold)
        Text("Concepto", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold)
        Text("Ingreso",  modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
        Text("Egreso",   modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
        Text("Balance",  modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
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
        Text(
            text     = fecha,
            modifier = Modifier.weight(1.8f)
        )
        Text(
            text     = concepto,
            modifier = Modifier.weight(2.5f)
        )
        Text(
            text     = ingreso?.let { "Q ${"%.2f".format(it)}" } ?: "-",
            modifier = Modifier.weight(1.5f),
            color    = if (ingreso != null) LaundryVerde else Color.Unspecified
        )
        Text(
            text     = egreso?.let { "Q ${"%.2f".format(it)}" } ?: "-",
            modifier = Modifier.weight(1.5f),
            color    = if (egreso != null) LaundryRojo else Color.Unspecified
        )
        Text(
            text       = "Q ${"%.2f".format(balance)}",
            modifier   = Modifier.weight(1.5f),
            color      = if (balance >= 0) LaundryVerde else LaundryRojo,
            fontWeight = FontWeight.SemiBold
        )
    }
}