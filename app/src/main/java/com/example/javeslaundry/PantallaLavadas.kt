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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Lavada
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLavadas(
    dao: LaundryDao,
    onVolver: () -> Unit,
    onAgregarLavada: () -> Unit,
    onEditarLavada: (Lavada) -> Unit
) {
    val lavadas by dao.obtenerLavadas().collectAsState(initial = emptyList())
    
    var busqueda by remember { mutableStateOf("") }

    // Filtros
    var filtroEstadoEntrega by remember { mutableStateOf("Todos") }
    var filtroEstadoPago by remember { mutableStateOf("Todos") }
    var mostrarFiltros by remember { mutableStateOf(false) }
    
    // Rango de fechas
    val dateRangePickerState = rememberDateRangePickerState()
    var mostrarCalendario by remember { mutableStateOf(false) }

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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAgregarLavada) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Lavada")
            }
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
                        onEdit = { onEditarLavada(lavada) }
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
