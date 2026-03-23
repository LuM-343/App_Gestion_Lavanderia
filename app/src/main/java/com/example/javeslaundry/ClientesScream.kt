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
fun PantallaClientes(
    dao: LaundryDao,
    onVolver: () -> Unit
) {
    val clientes by dao.obtenerClientes().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }

    val clientesFiltrados = clientes.filter {
        it.nombre.contains(busqueda, ignoreCase = true) ||
                it.telefono.contains(busqueda, ignoreCase = true) ||
                it.direccion.contains(busqueda, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Clientes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (nombre.isNotBlank() && telefono.isNotBlank() && direccion.isNotBlank()) {
                        scope.launch {
                            dao.insertarCliente(
                                Cliente(
                                    nombre = nombre,
                                    telefono = telefono,
                                    direccion = direccion
                                )
                            )
                            nombre = ""
                            telefono = ""
                            direccion = ""
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
            label = { Text("Buscar cliente...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Clientes registrados",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        EncabezadoTablaClientes()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(clientesFiltrados) { cliente ->
                FilaCliente(cliente)
            }
        }
    }
}

@Composable
fun EncabezadoTablaClientes() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp)
    ) {
        Text("ID", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Nombre", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Teléfono", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
        Text("Dirección", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilaCliente(cliente: Cliente) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(8.dp)
    ) {
        Text(cliente.id.toString(), modifier = Modifier.weight(1f))
        Text(cliente.nombre, modifier = Modifier.weight(2f))
        Text(cliente.telefono, modifier = Modifier.weight(2f))
        Text(cliente.direccion, modifier = Modifier.weight(3f))
    }
}