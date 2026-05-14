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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Lavada
import com.example.javeslaundry.database.Movimiento
import java.text.SimpleDateFormat
import java.util.*

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

    val opcionesEntrega = listOf("Pendiente", "Entregada")
    var estadoEntregaSeleccionado by remember { mutableStateOf("Pendiente") }
    var expandedEntrega by remember { mutableStateOf(false) }

    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var lavadaEnEdicion by remember { mutableStateOf<Lavada?>(null) }

    // Filtros
    var filtroEstadoEntrega by remember { mutableStateOf("Todos") }
    var filtroEstadoPago by remember { mutableStateOf("Todos") }
    var mostrarFiltros by remember { mutableStateOf(false) }
    
    // Rango de fechas
    val dateRangePickerState = rememberDateRangePickerState()
    var mostrarCalendario by remember { mutableStateOf(false) }

    // Diálogo de confirmación para eliminar
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    val lavadasFiltradas = lavadas.filter {
        val coincideBusqueda = it.cliente.contains(busqueda, ignoreCase = true) ||
                it.tipoPrenda.contains(busqueda, ignoreCase = true)
        
        val coincideEntrega = if (filtroEstadoEntrega == "Todos") true else it.estadoEntrega == filtroEstadoEntrega
        val coincidePago = if (filtroEstadoPago == "Todos") true else it.estadoPago == filtroEstadoPago
        
        val fechaInicio = dateRangePickerState.selectedStartDateMillis
        val fechaFin = dateRangePickerState.selectedEndDateMillis
        
        val coincideFecha = if (fechaInicio != null && fechaFin != null) {
            it.fechaCreacion in fechaInicio..fechaFin + 86399999
        } else if (fechaInicio != null) {
            it.fechaCreacion >= fechaInicio
        } else {
            true
        }
        
        coincideBusqueda && coincideEntrega && coincidePago && coincideFecha
    }

    val clientesFiltrados = clientesRegistrados.filter {
        it.nombre.contains(clienteNombre, ignoreCase = true)
    }

    fun limpiarCampos() {
        clienteNombre = ""
        selectedPrendas = emptySet()
        estadoPagoSeleccionado = "Pendiente"
        estadoEntregaSeleccionado = "Pendiente"
        cantidad = ""
        precio = ""
        lavadaEnEdicion = null
    }

    // Diálogo de calendario
    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    dateRangePickerState.setSelection(null, null)
                    mostrarCalendario = false 
                }) {
                    Text("Limpiar")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Selecciona rango de fechas", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.height(400.dp)
            )
        }
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
                },
                actions = {
                    IconButton(onClick = { mostrarCalendario = true }) {
                        Icon(
                            Icons.Default.CalendarMonth, 
                            contentDescription = "Fecha",
                            tint = if (dateRangePickerState.selectedStartDateMillis != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = "Filtros",
                            tint = if (filtroEstadoEntrega != "Todos" || filtroEstadoPago != "Todos") MaterialTheme.colorScheme.primary else LocalContentColor.current
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
            if (mostrarFiltros) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Filtrar por Entrega:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = filtroEstadoEntrega == "Todos", onClick = { filtroEstadoEntrega = "Todos" })
                            Text("Todos", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            RadioButton(selected = filtroEstadoEntrega == "Pendiente", onClick = { filtroEstadoEntrega = "Pendiente" })
                            Text("Pend.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            RadioButton(selected = filtroEstadoEntrega == "Entregada", onClick = { filtroEstadoEntrega = "Entregada" })
                            Text("Entreg.", fontSize = 12.sp)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("Filtrar por Pago:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = filtroEstadoPago == "Todos", onClick = { filtroEstadoPago = "Todos" })
                            Text("Todos", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            RadioButton(selected = filtroEstadoPago == "Pendiente", onClick = { filtroEstadoPago = "Pendiente" })
                            Text("Pend.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            RadioButton(selected = filtroEstadoPago == "Pagado", onClick = { filtroEstadoPago = "Pagado" })
                            Text("Pagado", fontSize = 12.sp)
                        }
                    }
                }
            }

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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Estados (solo al editar) ─────────────────────────────
            if (lavadaEnEdicion != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Estado de Pago
                    ExposedDropdownMenuBox(
                        expanded = expandedPago,
                        onExpandedChange = { expandedPago = !expandedPago },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = estadoPagoSeleccionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pago") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPago) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedPago, onDismissRequest = { expandedPago = false }) {
                            opcionesPago.forEach { pago ->
                                DropdownMenuItem(
                                    text = { Text(pago) },
                                    onClick = { estadoPagoSeleccionado = pago; expandedPago = false },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    // Estado de Entrega
                    ExposedDropdownMenuBox(
                        expanded = expandedEntrega,
                        onExpandedChange = { expandedEntrega = !expandedEntrega },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = estadoEntregaSeleccionado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Entrega") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEntrega) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedEntrega, onDismissRequest = { expandedEntrega = false }) {
                            opcionesEntrega.forEach { entrega ->
                                DropdownMenuItem(
                                    text = { Text(entrega) },
                                    onClick = { estadoEntregaSeleccionado = entrega; expandedEntrega = false },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
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
                                    dao.insertarLavada(
                                        Lavada(
                                            cliente = clienteNombre,
                                            tipoPrenda = tipoPrendaFinal,
                                            cantidad = cantidadInt,
                                            precio = precioDouble,
                                            estadoPago = "Pendiente",
                                            estadoEntrega = "Pendiente",
                                            fechaCreacion = System.currentTimeMillis()
                                        )
                                    )
                                } else {
                                    val antesPagado = lavadaEnEdicion!!.estadoPago.equals("Pagado", ignoreCase = true)
                                    val ahoraPagado = estadoPagoSeleccionado.equals("Pagado", ignoreCase = true)
                                    val precioAnterior = lavadaEnEdicion!!.precio
                                    
                                    val fechaEntregaFinal = if (estadoEntregaSeleccionado == "Entregada" && lavadaEnEdicion!!.fechaEntrega == null) {
                                        System.currentTimeMillis()
                                    } else if (estadoEntregaSeleccionado == "Pendiente") {
                                        null
                                    } else {
                                        lavadaEnEdicion!!.fechaEntrega
                                    }

                                    dao.actualizarLavada(
                                        lavadaEnEdicion!!.copy(
                                            cliente = clienteNombre,
                                            tipoPrenda = tipoPrendaFinal,
                                            cantidad = cantidadInt,
                                            precio = precioDouble,
                                            estadoPago = estadoPagoSeleccionado,
                                            estadoEntrega = estadoEntregaSeleccionado,
                                            fechaEntrega = fechaEntregaFinal
                                        )
                                    )

                                    when {
                                        !antesPagado && ahoraPagado -> {
                                            dao.insertarMovimiento(Movimiento(concepto = "Pago lavada - $clienteNombre ($tipoPrendaFinal)", monto = precioDouble, tipo = "ingreso"))
                                        }
                                        antesPagado && !ahoraPagado -> {
                                            dao.insertarMovimiento(Movimiento(concepto = "Reversión pago - $clienteNombre ($tipoPrendaFinal)", monto = precioAnterior, tipo = "egreso"))
                                        }
                                        antesPagado && ahoraPagado && precioAnterior != precioDouble -> {
                                            val diferencia = precioDouble - precioAnterior
                                            if (diferencia > 0) {
                                                dao.insertarMovimiento(Movimiento(concepto = "Ajuste precio lavada + $clienteNombre", monto = diferencia, tipo = "ingreso"))
                                            } else {
                                                dao.insertarMovimiento(Movimiento(concepto = "Ajuste precio lavada - $clienteNombre", monto = -diferencia, tipo = "egreso"))
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar")
                    }

                    Button(onClick = { limpiarCampos() }, modifier = Modifier.weight(1f)) {
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
                            estadoEntregaSeleccionado = lavada.estadoEntrega
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
        Text("Fecha",    modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
        Text("Cliente",  modifier = Modifier.weight(2f),   fontWeight = FontWeight.Bold)
        Text("Precio",   modifier = Modifier.weight(1f),   fontWeight = FontWeight.Bold)
        Text("Entrega",  modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
        Text("Pago",     modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilaLavada(lavada: Lavada, onEdit: () -> Unit) {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val fechaStr = sdf.format(Date(lavada.fechaCreacion))
    
    val colorPago = if (lavada.estadoPago == "Pagado") Color(0xFF2E7D32) else Color(0xFFC62828)
    val colorEntrega = if (lavada.estadoEntrega == "Entregada") Color(0xFF1976D2) else Color(0xFFF57C00)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .clickable { onEdit() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(fechaStr,                         modifier = Modifier.weight(1.2f), fontSize = 12.sp)
        Text(lavada.cliente,                   modifier = Modifier.weight(2f),   fontSize = 14.sp)
        Text("Q${"%.0f".format(lavada.precio)}", modifier = Modifier.weight(1f),   fontSize = 14.sp)
        Text(
            lavada.estadoEntrega,
            modifier = Modifier.weight(1.2f),
            color = colorEntrega,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        Text(
            lavada.estadoPago,
            modifier = Modifier.weight(1.2f),
            color = colorPago,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}