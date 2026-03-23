package com.example.javeslaundry

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PantallaLavadas(
    dao: LaundryDao,
    onVolver: () -> Unit
) {
    val lavadas by dao.obtenerLavadas().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var cliente by remember { mutableStateOf("") }
    var tipoPrenda by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }

    val lavadasFiltradas = lavadas.filter {
        it.cliente.contains(busqueda, ignoreCase = true) ||
                it.tipoPrenda.contains(busqueda, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Lavadas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = cliente,
            onValueChange = { cliente = it },
            label = { Text("Cliente") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tipoPrenda,
            onValueChange = { tipoPrenda = it },
            label = { Text("Tipo de prenda") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cantidad,
            onValueChange = { cantidad = it },
            label = { Text("Cantidad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = precio,
            onValueChange = { precio = it },
            label = { Text("Precio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val cantidadInt = cantidad.toIntOrNull()
                    val precioDouble = precio.toDoubleOrNull()

                    if (
                        cliente.isNotBlank() &&
                        tipoPrenda.isNotBlank() &&
                        cantidadInt != null &&
                        precioDouble != null
                    ) {
                        scope.launch {
                            dao.insertarLavada(
                                Lavada(
                                    cliente = cliente,
                                    tipoPrenda = tipoPrenda,
                                    cantidad = cantidadInt,
                                    precio = precioDouble
                                )
                            )
                            cliente = ""
                            tipoPrenda = ""
                            cantidad = ""
                            precio = ""
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Agregar")
            }

            Button(
                onClick = onVolver,
                modifier = Modifier.weight(1f)
            ) {
                Text("Volver")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar lavada...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Lavadas registradas",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        EncabezadoTablaLavadas()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(lavadasFiltradas) { lavada ->
                FilaLavada(lavada)
            }
        }
    }
}

@Composable
fun EncabezadoTablaLavadas() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp)
    ) {
        Text("ID", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Cliente", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Prenda", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Cant.", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Precio", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilaLavada(lavada: Lavada) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(8.dp)
    ) {
        Text(lavada.id.toString(), modifier = Modifier.weight(1f))
        Text(lavada.cliente, modifier = Modifier.weight(2f))
        Text(lavada.tipoPrenda, modifier = Modifier.weight(2f))
        Text(lavada.cantidad.toString(), modifier = Modifier.weight(1f))
        Text("Q ${lavada.precio}", modifier = Modifier.weight(2f))
    }
}