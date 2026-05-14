package com.example.javeslaundry

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javeslaundry.database.Cliente
import com.example.javeslaundry.database.LaundryDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaClientes(
    dao: LaundryDao,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val clientes by dao.obtenerClientes().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }

    var clienteEnEdicion by remember { mutableStateOf<Cliente?>(null) }

    val esTelefonoValido = telefono.length == 8 && telefono.all { it.isDigit() }

    val clientesFiltrados = clientes.filter {
        it.nombre.contains(busqueda, ignoreCase = true) ||
                it.telefono.contains(busqueda, ignoreCase = true) ||
                it.direccion.contains(busqueda, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Clientes",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = telefono,
                onValueChange = { input ->
                    // Filtramos para que solo acepte números y máximo 8 dígitos
                    if (input.all { it.isDigit() }) {
                        if (input.length <= 8) {
                            telefono = input
                        }
                    } else {
                        Toast.makeText(context, "El teléfono solo debe contener números", Toast.LENGTH_SHORT).show()
                    }
                },
                label = { Text("Teléfono (8 dígitos)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = telefono.isNotEmpty() && !esTelefonoValido,
                supportingText = {
                    if (telefono.isNotEmpty() && !esTelefonoValido) {
                        Text("Debe ingresar exactamente 8 números")
                    }
                }
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
                        if (nombre.isNotBlank() && esTelefonoValido && direccion.isNotBlank()) {
                            scope.launch {
                                if (clienteEnEdicion == null) {
                                    dao.insertarCliente(
                                        Cliente(
                                            nombre = nombre,
                                            telefono = telefono,
                                            direccion = direccion
                                        )
                                    )
                                    Toast.makeText(context, "Cliente guardado", Toast.LENGTH_SHORT).show()
                                } else {
                                    dao.actualizarCliente(
                                        clienteEnEdicion!!.copy(
                                            nombre = nombre,
                                            telefono = telefono,
                                            direccion = direccion
                                        )
                                    )
                                    Toast.makeText(context, "Cliente actualizado", Toast.LENGTH_SHORT).show()
                                }
                                nombre = ""
                                telefono = ""
                                direccion = ""
                                clienteEnEdicion = null
                            }
                        } else if (!esTelefonoValido) {
                            Toast.makeText(context, "El número de teléfono debe ser de 8 dígitos", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (clienteEnEdicion == null) "Agregar" else "Actualizar")
                }

                if (clienteEnEdicion != null) {
                    Button(
                        onClick = {
                            nombre = ""
                            telefono = ""
                            direccion = ""
                            clienteEnEdicion = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
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
                    Box(
                        modifier = Modifier.clickable {
                            clienteEnEdicion = cliente
                            nombre = cliente.nombre
                            telefono = cliente.telefono
                            direccion = cliente.direccion
                        }
                    ) {
                        FilaCliente(cliente)
                    }
                }
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