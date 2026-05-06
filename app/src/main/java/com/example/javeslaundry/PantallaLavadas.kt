package com.example.javeslaundry

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLavadas(
    dao: LaundryDao,
    onVolver: () -> Unit
) {
    val lavadas by dao.obtenerLavadas().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var cliente by remember { mutableStateOf("") }
    
    // Opciones para el menú desplegable múltiple de Prendas
    val opcionesPrendas = listOf("Camisa", "Pantalón", "Vestido", "Saco", "Chaqueta", "Cobija", "Edredón", "Otros")
    var selectedPrendas by remember { mutableStateOf(setOf<String>()) }
    var expandedPrendas by remember { mutableStateOf(false) }

    // Opciones para el menú desplegable de Pago (Selección única)
    val opcionesPago = listOf("Pendiente", "Pagado")
    var estadoPagoSeleccionado by remember { mutableStateOf("Pendiente") }
    var expandedPago by remember { mutableStateOf(false) }

    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }

    // Estado para controlar si estamos creando o editando
    var lavadaEnEdicion by remember { mutableStateOf<Lavada?>(null) }

    val lavadasFiltradas = lavadas.filter {
        it.cliente.contains(busqueda, ignoreCase = true) ||
                it.tipoPrenda.contains(busqueda, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Lavadas",
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
                value = cliente,
                onValueChange = { cliente = it },
                label = { Text("Cliente") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Menú desplegable múltiple para Tipo de prenda
            ExposedDropdownMenuBox(
                expanded = expandedPrendas,
                onExpandedChange = { expandedPrendas = !expandedPrendas },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedPrendas.isEmpty()) "Seleccionar prendas" else selectedPrendas.joinToString(", "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de prenda") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrendas) },
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
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
                                    Text(text = prenda, modifier = Modifier.padding(start = 8.dp))
                                }
                            },
                            onClick = {
                                selectedPrendas = if (selectedPrendas.contains(prenda)) {
                                    selectedPrendas - prenda
                                } else {
                                    selectedPrendas + prenda
                                }
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

            // Menú desplegable de selección ÚNICA para Estado de Pago (SÓLO SE MUESTRA AL EDITAR)
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
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPago,
                        onDismissRequest = { expandedPago = false }
                    ) {
                        opcionesPago.forEach { pago ->
                            DropdownMenuItem(
                                text = { Text(text = pago) },
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

            // Fila con botones dinámicos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cantidadInt = cantidad.toIntOrNull()
                        val precioDouble = precio.toDoubleOrNull()
                        val tipoPrendaFinal = selectedPrendas.joinToString(", ")

                        if (
                            cliente.isNotBlank() &&
                            tipoPrendaFinal.isNotBlank() &&
                            cantidadInt != null &&
                            precioDouble != null
                        ) {
                            scope.launch {
                                if (lavadaEnEdicion == null) {
                                    // Insertar nueva lavada - Automáticamente Pendiente
                                    dao.insertarLavada(
                                        Lavada(
                                            cliente = cliente,
                                            tipoPrenda = tipoPrendaFinal,
                                            cantidad = cantidadInt,
                                            precio = precioDouble,
                                            estadoPago = "Pendiente"
                                        )
                                    )
                                } else {
                                    // Actualizar lavada existente
                                    val lavadaActualizada = lavadaEnEdicion!!.copy(
                                        cliente = cliente,
                                        tipoPrenda = tipoPrendaFinal,
                                        cantidad = cantidadInt,
                                        precio = precioDouble,
                                        estadoPago = estadoPagoSeleccionado
                                    )
                                    dao.actualizarLavada(lavadaActualizada)

                                    // LÓGICA DE MOVIMIENTOS: Si antes NO estaba pagado, y ahora SÍ lo está
                                    val antesPagado = lavadaEnEdicion!!.estadoPago.equals("Pagado", ignoreCase = true)
                                    val ahoraPagado = estadoPagoSeleccionado.equals("Pagado", ignoreCase = true)
                                    
                                    if (!antesPagado && ahoraPagado) {
                                        dao.insertarMovimiento(
                                            Movimiento(
                                                concepto = "Pago de lavada - $cliente",
                                                monto = precioDouble,
                                                tipo = "ingreso"
                                            )
                                        )
                                    }
                                }
                                // Limpiar campos
                                cliente = ""
                                selectedPrendas = emptySet()
                                estadoPagoSeleccionado = "Pendiente"
                                cantidad = ""
                                precio = ""
                                lavadaEnEdicion = null
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (lavadaEnEdicion == null) "Agregar" else "Actualizar")
                }

                // Botón de cancelar solo visible al editar
                if (lavadaEnEdicion != null) {
                    Button(
                        onClick = {
                            cliente = ""
                            selectedPrendas = emptySet()
                            estadoPagoSeleccionado = "Pendiente"
                            cantidad = ""
                            precio = ""
                            lavadaEnEdicion = null
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
                    FilaLavada(
                        lavada = lavada,
                        onEdit = {
                            lavadaEnEdicion = lavada
                            cliente = lavada.cliente
                            selectedPrendas = if (lavada.tipoPrenda.isBlank()) emptySet() else lavada.tipoPrenda.split(", ").toSet()
                            estadoPagoSeleccionado = lavada.estadoPago
                            cantidad = lavada.cantidad.toString()
                            precio = lavada.precio.toString()
                        },
                        onDelete = {
                            scope.launch {
                                dao.eliminarLavada(lavada)
                                if (lavadaEnEdicion?.id == lavada.id) {
                                    lavadaEnEdicion = null
                                    cliente = ""
                                    selectedPrendas = emptySet()
                                    estadoPagoSeleccionado = "Pendiente"
                                    cantidad = ""
                                    precio = ""
                                }
                            }
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
        Text("ID", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
        Text("Cliente", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
        Text("Prenda", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
        Text("Cant.", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
        Text("Precio", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Estado", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("Acción", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilaLavada(
    lavada: Lavada,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(lavada.id.toString(), modifier = Modifier.weight(0.5f))
        Text(lavada.cliente, modifier = Modifier.weight(1.5f))
        Text(lavada.tipoPrenda, modifier = Modifier.weight(1.5f))
        Text(lavada.cantidad.toString(), modifier = Modifier.weight(0.7f))
        Text("Q ${lavada.precio}", modifier = Modifier.weight(1f))
        Text(lavada.estadoPago, modifier = Modifier.weight(1f))
        
        Row(modifier = Modifier.weight(1f)) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}