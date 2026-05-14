package com.example.javeslaundry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Lavada
import com.example.javeslaundry.database.Movimiento
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioLavada(
    dao: LaundryDao,
    lavadaAEditar: Lavada? = null,
    onVolver: () -> Unit,
    onGuardarExitoso: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clientesRegistrados by dao.obtenerClientes().collectAsState(initial = emptyList())

    var clienteNombre by remember { mutableStateOf(lavadaAEditar?.cliente ?: "") }
    var expandedClientes by remember { mutableStateOf(false) }

    val opcionesPrendas = listOf(
        "Ropa Comun", "Ropa Delicada", "Ropa Muy Sucia",
        "Edredones/Ponchos", "Sacos/Vestidos", "Otros"
    )
    var selectedPrendas by remember { 
        mutableStateOf(
            if (lavadaAEditar?.tipoPrenda.isNullOrBlank()) emptySet<String>()
            else lavadaAEditar!!.tipoPrenda.split(", ").toSet()
        ) 
    }
    var expandedPrendas by remember { mutableStateOf(false) }

    val opcionesPago = listOf("Pendiente", "Pagado")
    var estadoPagoSeleccionado by remember { mutableStateOf(lavadaAEditar?.estadoPago ?: "Pendiente") }
    var expandedPago by remember { mutableStateOf(false) }

    val opcionesEntrega = listOf("Pendiente", "Entregada")
    var estadoEntregaSeleccionado by remember { mutableStateOf(lavadaAEditar?.estadoEntrega ?: "Pendiente") }
    var expandedEntrega by remember { mutableStateOf(false) }

    var cantidad by remember { mutableStateOf(lavadaAEditar?.cantidad?.toString() ?: "") }
    var precio by remember { mutableStateOf(lavadaAEditar?.precio?.toString() ?: "") }

    val clientesFiltrados = clientesRegistrados.filter {
        it.nombre.contains(clienteNombre, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (lavadaAEditar == null) "Nueva Lavada" else "Editar Lavada", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selector de Cliente
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
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClientes) }
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
                                }
                            )
                        }
                    }
                }
            }

            // Selector de Prendas
            ExposedDropdownMenuBox(
                expanded = expandedPrendas,
                onExpandedChange = { expandedPrendas = !expandedPrendas },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedPrendas.isEmpty()) "Seleccionar Tipo de Ropa"
                    else selectedPrendas.joinToString(", "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de ropa") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrendas) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedPrendas,
                    onDismissRequest = { expandedPrendas = false }
                ) {
                    opcionesPrendas.forEach { prenda ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = selectedPrendas.contains(prenda), onCheckedChange = null)
                                    Text(prenda, modifier = Modifier.padding(start = 8.dp))
                                }
                            },
                            onClick = {
                                selectedPrendas = if (selectedPrendas.contains(prenda))
                                    selectedPrendas - prenda
                                else
                                    selectedPrendas + prenda
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // Estados
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedPago, onDismissRequest = { expandedPago = false }) {
                        opcionesPago.forEach { pago ->
                            DropdownMenuItem(text = { Text(pago) }, onClick = { estadoPagoSeleccionado = pago; expandedPago = false })
                        }
                    }
                }

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
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedEntrega, onDismissRequest = { expandedEntrega = false }) {
                        opcionesEntrega.forEach { entrega ->
                            DropdownMenuItem(text = { Text(entrega) }, onClick = { estadoEntregaSeleccionado = entrega; expandedEntrega = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val cantInt = cantidad.toIntOrNull()
                    val precDouble = precio.toDoubleOrNull()
                    val tipoP = selectedPrendas.joinToString(", ")

                    if (clienteNombre.isNotBlank() && tipoP.isNotBlank() && cantInt != null && precDouble != null) {
                        scope.launch {
                            if (lavadaAEditar == null) {
                                dao.insertarLavada(
                                    Lavada(
                                        cliente = clienteNombre,
                                        tipoPrenda = tipoP,
                                        cantidad = cantInt,
                                        precio = precDouble,
                                        estadoPago = estadoPagoSeleccionado,
                                        estadoEntrega = estadoEntregaSeleccionado
                                    )
                                )
                            } else {
                                val antesPagado = lavadaAEditar.estadoPago == "Pagado"
                                val ahoraPagado = estadoPagoSeleccionado == "Pagado"
                                
                                val fechaEntregaFinal = if (estadoEntregaSeleccionado == "Entregada" && lavadaAEditar.fechaEntrega == null) {
                                    System.currentTimeMillis()
                                } else if (estadoEntregaSeleccionado == "Pendiente") {
                                    null
                                } else {
                                    lavadaAEditar.fechaEntrega
                                }

                                dao.actualizarLavada(
                                    lavadaAEditar.copy(
                                        cliente = clienteNombre,
                                        tipoPrenda = tipoP,
                                        cantidad = cantInt,
                                        precio = precDouble,
                                        estadoPago = estadoPagoSeleccionado,
                                        estadoEntrega = estadoEntregaSeleccionado,
                                        fechaEntrega = fechaEntregaFinal
                                    )
                                )

                                // Lógica de movimientos (pago)
                                when {
                                    !antesPagado && ahoraPagado -> {
                                        dao.insertarMovimiento(Movimiento(concepto = "Pago lavada - $clienteNombre", monto = precDouble, tipo = "ingreso"))
                                    }
                                    antesPagado && !ahoraPagado -> {
                                        dao.insertarMovimiento(Movimiento(concepto = "Reversión pago - $clienteNombre", monto = precDouble, tipo = "egreso"))
                                    }
                                }
                            }
                            onGuardarExitoso()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (lavadaAEditar == null) "Guardar Lavada" else "Actualizar Lavada")
            }
        }
    }
}
