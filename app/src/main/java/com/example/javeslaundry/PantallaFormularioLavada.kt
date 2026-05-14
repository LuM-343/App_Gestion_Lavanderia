package com.example.javeslaundry

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clientesRegistrados by dao.obtenerClientes().collectAsState(initial = emptyList())

    val esNueva = lavadaAEditar == null
    val yaPagado = lavadaAEditar?.estadoPago == "Pagado"

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

    var estadoEntregaSeleccionado by remember { mutableStateOf(lavadaAEditar?.estadoEntrega ?: "Pendiente") }

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
                        if (esNueva) "Nueva Lavada" else "Editar Lavada", 
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
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            cantidad = input
                        } else {
                            Toast.makeText(context, "La cantidad solo acepta números", Toast.LENGTH_SHORT).show()
                        }
                    },
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = precio,
                    onValueChange = { input ->
                        // Permite números y un solo punto decimal
                        if (input.isEmpty() || input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' }) {
                            precio = input
                        } else {
                            Toast.makeText(context, "El precio debe ser un número válido", Toast.LENGTH_SHORT).show()
                        }
                    },
                    label = { Text("Precio (Q)") },
                    modifier = Modifier.weight(1f),
                    enabled = !yaPagado,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Estados
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedPago && !yaPagado,
                    onExpandedChange = { if (!yaPagado) expandedPago = !expandedPago },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = estadoPagoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pago") },
                        enabled = !yaPagado,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    if (!yaPagado) {
                        ExposedDropdownMenu(expanded = expandedPago, onDismissRequest = { expandedPago = false }) {
                            opcionesPago.forEach { pago ->
                                DropdownMenuItem(text = { Text(pago) }, onClick = { estadoPagoSeleccionado = pago; expandedPago = false })
                            }
                        }
                    }
                }

                if (!esNueva) {
                    OutlinedTextField(
                        value = estadoEntregaSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Entrega") },
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            if (yaPagado) {
                Text(
                    "Esta lavada ya fue pagada. Para modificar el precio o el estado de pago, debe eliminarla y crear una nueva.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val cantInt = cantidad.toIntOrNull()
                    val precDouble = precio.toDoubleOrNull()
                    val tipoP = selectedPrendas.joinToString(", ")

                    if (clienteNombre.isBlank()) {
                        Toast.makeText(context, "Selecciona un cliente", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (tipoP.isBlank()) {
                        Toast.makeText(context, "Selecciona al menos un tipo de prenda", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cantInt == null) {
                        Toast.makeText(context, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (precDouble == null) {
                        Toast.makeText(context, "Ingresa un precio válido", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        if (esNueva) {
                            dao.insertarLavada(
                                Lavada(
                                    cliente = clienteNombre,
                                    tipoPrenda = tipoP,
                                    cantidad = cantInt,
                                    precio = precDouble,
                                    estadoPago = estadoPagoSeleccionado,
                                    estadoEntrega = "Pendiente"
                                )
                            )
                            if (estadoPagoSeleccionado == "Pagado") {
                                dao.insertarMovimiento(Movimiento(concepto = "Pago lavada (Nueva): $clienteNombre", monto = precDouble, tipo = "ingreso"))
                            }
                        } else {
                            val antesPagado = lavadaAEditar!!.estadoPago == "Pagado"
                            val ahoraPagado = estadoPagoSeleccionado == "Pagado"

                            dao.actualizarLavada(
                                lavadaAEditar.copy(
                                    cliente = clienteNombre,
                                    tipoPrenda = tipoP,
                                    cantidad = cantInt,
                                    precio = precDouble,
                                    estadoPago = estadoPagoSeleccionado
                                )
                            )

                            if (!antesPagado && ahoraPagado) {
                                dao.insertarMovimiento(Movimiento(concepto = "Pago lavada: $clienteNombre", monto = precDouble, tipo = "ingreso"))
                            }
                        }
                        onGuardarExitoso()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (esNueva) "Guardar Lavada" else "Actualizar Lavada")
            }
        }
    }
}
