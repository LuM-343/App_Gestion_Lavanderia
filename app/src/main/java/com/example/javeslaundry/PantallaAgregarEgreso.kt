package com.example.javeslaundry
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Movimiento
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarEgreso(
    dao: LaundryDao,
    onVolver: () -> Unit,
    onGuardarExitoso: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var monto by remember { mutableStateOf("") }
    var concepto by remember { mutableStateOf("") }
    var expandedConceptos by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var movimientoEnEdicion by remember { mutableStateOf<Movimiento?>(null) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    // Conceptos únicos previos para autocompletado
    val conceptosPrevios by dao.obtenerConceptosPorTipo("egreso")
        .collectAsState(initial = emptyList())

    // Todos los egresos registrados
    val egresosRegistrados by dao.obtenerMovimientosPorTipo("egreso")
        .collectAsState(initial = emptyList())

    val conceptosFiltrados = conceptosPrevios.filter {
        it.contains(concepto, ignoreCase = true) && concepto.isNotBlank()
    }

    val egresosFiltrados = egresosRegistrados.filter {
        it.concepto.contains(busqueda, ignoreCase = true) ||
                busqueda.isBlank()
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun limpiarCampos() {
        monto = ""
        concepto = ""
        busqueda = ""
        movimientoEnEdicion = null
    }

    // Diálogo confirmar eliminación
    if (mostrarDialogoEliminar && movimientoEnEdicion != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar egreso") },
            text = { Text("¿Seguro que deseas eliminar el egreso \"${movimientoEnEdicion!!.concepto}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.eliminarMovimiento(movimientoEnEdicion!!)
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
                title = { Text("Egreso", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {

            // ── Campo Concepto con autocompletado ────────────────────────────
            ExposedDropdownMenuBox(
                expanded = expandedConceptos && conceptosFiltrados.isNotEmpty(),
                onExpandedChange = { expandedConceptos = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = {
                        concepto = it
                        expandedConceptos = true
                        movimientoEnEdicion = null
                    },
                    label = { Text("Concepto") },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                        .fillMaxWidth(),
                    trailingIcon = {
                        if (concepto.isNotBlank()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedConceptos && conceptosFiltrados.isNotEmpty()
                            )
                        }
                    }
                )
                if (conceptosFiltrados.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedConceptos && conceptosFiltrados.isNotEmpty(),
                        onDismissRequest = { expandedConceptos = false }
                    ) {
                        conceptosFiltrados.forEach { conceptoPrevio ->
                            DropdownMenuItem(
                                text = { Text(conceptoPrevio) },
                                onClick = {
                                    concepto = conceptoPrevio
                                    expandedConceptos = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Campo Monto ──────────────────────────────────────────────────
            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Monto") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Botones ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull()
                        if (montoDouble != null && concepto.isNotBlank()) {
                            scope.launch {
                                if (movimientoEnEdicion == null) {
                                    dao.insertarMovimiento(
                                        Movimiento(
                                            concepto = concepto,
                                            monto = montoDouble,
                                            tipo = "egreso"
                                        )
                                    )
                                } else {
                                    dao.actualizarMovimiento(
                                        movimientoEnEdicion!!.copy(
                                            concepto = concepto,
                                            monto = montoDouble
                                        )
                                    )
                                }
                                limpiarCampos()
                                onGuardarExitoso()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (movimientoEnEdicion == null) "GUARDAR" else "ACTUALIZAR")
                }

                if (movimientoEnEdicion != null) {
                    Button(
                        onClick = { mostrarDialogoEliminar = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("ELIMINAR")
                    }
                }

                Button(
                    onClick = {
                        if (movimientoEnEdicion != null) limpiarCampos()
                        else onVolver()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCELAR")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Buscador de egresos registrados ──────────────────────────────
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                label = { Text("Buscar egreso registrado...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Egresos registrados", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(8.dp))

            // Encabezado tabla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(8.dp)
            ) {
                Text("Fecha",    modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                Text("Concepto", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold)
                Text("Monto",    modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(egresosFiltrados) { egreso ->
                    val esSeleccionado = movimientoEnEdicion?.id == egreso.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (esSeleccionado) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                            .clickable {
                                movimientoEnEdicion = egreso
                                concepto = egreso.concepto
                                monto = egreso.monto.toString()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            sdf.format(Date(egreso.fecha)),
                            modifier = Modifier.weight(1.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            egreso.concepto,
                            modifier = Modifier.weight(2.5f),
                            fontSize = 13.sp
                        )
                        Text(
                            "Q ${"%.2f".format(egreso.monto)}",
                            modifier = Modifier.weight(1.2f),
                            fontSize = 13.sp,
                            color = Color(red = 198, green = 40, blue = 40),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}