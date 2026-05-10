package com.example.javeslaundry

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLavadas(
    dao: LaundryDao,
    onVolver: () -> Unit
) {
    val lavadas by dao.obtenerLavadas().collectAsState(initial = emptyList())
    val clientesRegistrados by dao.obtenerClientes().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var clienteNombre by remember { mutableStateOf("") }
    var expandedClientes by remember { mutableStateOf(false) }

    val opcionesPrendas = listOf(
        "Camisa", "Pantalón", "Vestido", "Saco",
        "Chaqueta", "Cobija", "Edredón", "Otros"
    )
    var selectedPrendas by remember { mutableStateOf(setOf<String>()) }
    var expandedPrendas by remember { mutableStateOf(false) }

    val opcionesPago = listOf("Pendiente", "Pagado")
    var estadoPagoSeleccionado by remember { mutableStateOf("Pendiente") }
    var expandedPago by remember { mutableStateOf(false) }

    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var lavadaEnEdicion by remember { mutableStateOf<Lavada?>(null) }

    // Diálogo de confirmación para eliminar
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    val lavadasFiltradas = lavadas.filter {
        it.cliente.contains(busqueda, ignoreCase = true) ||
                it.tipoPrenda.contains(busqueda, ignoreCase = true)
    }

    val clientesFiltrados = clientesRegistrados.filter {
        it.nombre.contains(clienteNombre, ignoreCase = true)
    }

    fun limpiarCampos() {
        clienteNombre = ""
        selectedPrendas = emptySet()
        estadoPagoSeleccionado = "Pendiente"
        cantidad = ""
        precio = ""
        lavadaEnEdicion = null
    }

    // Diálogo de confirmación de eliminación
    if (mostrarDialogoEliminar && lavadaEnEdicion != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar lavada") },
            text = {
                Text(
                    if (lavadaEnEdicion!!.estadoPago == "Pagado")
                        "Esta lavada ya fue pagada (Q ${"%.2f".format(lavadaEnEdicion!!.precio)}). " +
                                "Se registrará una devolución en movimientos. ¿Deseas continuar?"
                    else
                        "¿Seguro que deseas eliminar la lavada de ${lavadaEnEdicion!!.cliente}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val lavada = lavadaEnEdicion!!
                        // Si estaba pagada, registrar devolución/egreso
                        if (lavada.estadoPago == "Pagado") {
                            dao.insertarMovimiento(
                                Movimiento(
                                    concepto = "Devolución / cancelación lavada - ${lavada.cliente}",
                                    monto = lavada.precio,
                                    tipo = "egreso"
                                )
                            )
                        }
                        dao.eliminarLavada(lavada)
                        limpiarCampos()
                    }
                    mostrarDialogoEliminar = false
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lavadas", fontWeight = FontWeight.Bold) },
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

            // ── Selector de Cliente ──────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = expandedClientes,
                onExpandedChange = { expandedClientes = !expandedClientes },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = clienteNombre,
                    onValueChange = {
                        clienteNombre = it
                        expandedClientes = true
                    },
                    label = { Text("Cliente") },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClientes) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                if (clientesFiltrados.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedClientes,
                        onDismissRequest = { expandedClientes = false }
                    ) {
                        clientesFiltrados.forEach { clienteObj ->
                            DropdownMenuItem(
                                text = { Text(clienteObj.nombre) },
                                onClick = {
                                    clienteNombre = clienteObj.nombre
                                    expandedClientes = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Selector de Prendas (multi) ──────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = expandedPrendas,
                onExpandedChange = { expandedPrendas = !expandedPrendas },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedPrendas.isEmpty()) "Seleccionar prendas"
                    else selectedPrendas.joinToString(", "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de prenda") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrendas) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedPrendas,
                    onDismissRequest = { expandedPrendas = false }
                ) {
                    opcionesPrendas.forEach { prenda ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedPrendas.contains(prenda),
                                        onCheckedChange = null
                                    )
                                    Text(prenda, modifier = Modifier.padding(start = 8.dp))
                                }
                            },
                            onClick = {
                                selectedPrendas = if (selectedPrendas.contains(prenda))
                                    selectedPrendas - prenda
                                else
                                    selectedPrendas + prenda
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(8.dp))

            // ── Estado de Pago (solo al editar) ─────────────────────────────
            if (lavadaEnEdicion != null) {
                ExposedDropdownMenuBox(
                    expanded = expandedPago,
                    onExpandedChange = { expandedPago = !expandedPago },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = estadoPagoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado de pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPago) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPago,
                        onDismissRequest = { expandedPago = false }
                    ) {
                        opcionesPago.forEach { pago ->
                            DropdownMenuItem(
                                text = { Text(pago) },
                                onClick = {
                                    estadoPagoSeleccionado = pago
                                    expandedPago = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Botones ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cantidadInt = cantidad.toIntOrNull()
                        val precioDouble = precio.toDoubleOrNull()
                        val tipoPrendaFinal = selectedPrendas.joinToString(", ")

                        if (clienteNombre.isNotBlank() &&
                            tipoPrendaFinal.isNotBlank() &&
                            cantidadInt != null &&
                            precioDouble != null
                        ) {
                            scope.launch {
                                if (lavadaEnEdicion == null) {
                                    // ── NUEVA lavada: siempre Pendiente ─────
                                    dao.insertarLavada(
                                        Lavada(
                                            cliente = clienteNombre,
                                            tipoPrenda = tipoPrendaFinal,
                                            cantidad = cantidadInt,
                                            precio = precioDouble,
                                            estadoPago = "Pendiente"
                                        )
                                    )
                                } else {
                                    val antesPagado = lavadaEnEdicion!!.estadoPago
                                        .equals("Pagado", ignoreCase = true)
                                    val ahoraPagado = estadoPagoSeleccionado
                                        .equals("Pagado", ignoreCase = true)
                                    val precioAnterior = lavadaEnEdicion!!.precio

                                    // ── ACTUALIZAR lavada ────────────────────
                                    dao.actualizarLavada(
                                        lavadaEnEdicion!!.copy(
                                            cliente = clienteNombre,
                                            tipoPrenda = tipoPrendaFinal,
                                            cantidad = cantidadInt,
                                            precio = precioDouble,
                                            estadoPago = estadoPagoSeleccionado
                                        )
                                    )

                                    when {
                                        // Caso 1 → Pendiente a Pagado: registrar INGRESO
                                        !antesPagado && ahoraPagado -> {
                                            dao.insertarMovimiento(
                                                Movimiento(
                                                    concepto = "Pago lavada - $clienteNombre ($tipoPrendaFinal)",
                                                    monto = precioDouble,
                                                    tipo = "ingreso"
                                                )
                                            )
                                        }
                                        // Caso 2 → Pagado a Pendiente: registrar EGRESO (reversión)
                                        antesPagado && !ahoraPagado -> {
                                            dao.insertarMovimiento(
                                                Movimiento(
                                                    concepto = "Reversión pago - $clienteNombre ($tipoPrendaFinal)",
                                                    monto = precioAnterior,
                                                    tipo = "egreso"
                                                )
                                            )
                                        }
                                        // Caso 3 → Ya pagado y se cambia el precio: ajuste
                                        antesPagado && ahoraPagado && precioAnterior != precioDouble -> {
                                            val diferencia = precioDouble - precioAnterior
                                            if (diferencia > 0) {
                                                dao.insertarMovimiento(
                                                    Movimiento(
                                                        concepto = "Ajuste precio lavada + $clienteNombre",
                                                        monto = diferencia,
                                                        tipo = "ingreso"
                                                    )
                                                )
                                            } else {
                                                dao.insertarMovimiento(
                                                    Movimiento(
                                                        concepto = "Ajuste precio lavada - $clienteNombre",
                                                        monto = -diferencia,
                                                        tipo = "egreso"
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                limpiarCampos()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (lavadaEnEdicion == null) "Agregar" else "Actualizar")
                }

                if (lavadaEnEdicion != null) {
                    Button(
                        onClick = { mostrarDialogoEliminar = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }

                    Button(
                        onClick = { limpiarCampos() },
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
                label = { Text("Buscar lavada...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Lavadas registradas", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(8.dp))

            EncabezadoTablaLavadas()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(lavadasFiltradas) { lavada ->
                    FilaLavada(
                        lavada = lavada,
                        onEdit = {
                            lavadaEnEdicion = lavada
                            clienteNombre = lavada.cliente
                            selectedPrendas = if (lavada.tipoPrenda.isBlank()) emptySet()
                            else lavada.tipoPrenda.split(", ").toSet()
                            estadoPagoSeleccionado = lavada.estadoPago
                            cantidad = lavada.cantidad.toString()
                            precio = lavada.precio.toString()
                        }
                    )
                }
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
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("ID",      modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
        Text("Cliente", modifier = Modifier.weight(2f),   fontWeight = FontWeight.Bold)
        Text("Prenda",  modifier = Modifier.weight(2f),   fontWeight = FontWeight.Bold)
        Text("Cant.",   modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
        Text("Precio",  modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
        Text("Estado",  modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilaLavada(lavada: Lavada, onEdit: () -> Unit) {
    val colorEstado = if (lavada.estadoPago == "Pagado")
        Color(0xFF2E7D32) else Color(0xFFC62828)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .clickable { onEdit() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(lavada.id.toString(),             modifier = Modifier.weight(0.5f))
        Text(lavada.cliente,                   modifier = Modifier.weight(2f))
        Text(lavada.tipoPrenda,                modifier = Modifier.weight(2f))
        Text(lavada.cantidad.toString(),       modifier = Modifier.weight(0.7f))
        Text("Q ${"%.2f".format(lavada.precio)}", modifier = Modifier.weight(1.2f))
        Text(
            lavada.estadoPago,
            modifier = Modifier.weight(1.2f),
            color = colorEstado,
            fontWeight = FontWeight.SemiBold
        )
    }
}