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
import com.example.javeslaundry.database.Cliente

@Composable
fun PantallaClientes(
    clientes: List<Cliente>,
    onAgregarClick: () -> Unit,
    onClienteClick: (Cliente) -> Unit
) {
    var busqueda by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Clientes",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onAgregarClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("+ Agregar Cliente")
        }

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar cliente...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        val clientesFiltrados = clientes.filter {
            it.nombre.contains(busqueda, ignoreCase = true)
        }

        LazyColumn {
            items(clientesFiltrados) { cliente ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onClienteClick(cliente) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = cliente.nombre, fontSize = 16.sp)
                        Text(text = "${cliente.telefono}  |  ${cliente.direccion}", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}