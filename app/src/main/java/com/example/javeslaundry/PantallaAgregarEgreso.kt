package com.example.javeslaundry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Movimiento
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class VistaEgreso {
    SELECCION_CATEGORIA,
    LISTA_CONCEPTOS,
    FORMULARIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarEgreso(
    dao: LaundryDao,
    onVolver: () -> Unit,
    onGuardarExitoso: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sdf   = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var vista                by remember { mutableStateOf(VistaEgreso.SELECCION_CATEGORIA) }
    var categoriaSeleccionada by remember { mutableStateOf("") }
    var conceptoSeleccionado  by remember { mutableStateOf("") }

    var monto               by remember { mutableStateOf("") }
    var descripcion         by remember { mutableStateOf("") }
    var nuevoConcepto       by remember { mutableStateOf("") }
    var fechaSeleccionada   by remember { mutableStateOf(System.currentTimeMillis()) }
    var mostrarCalendario   by remember { mutableStateOf(false) }
    var movimientoEnEdicion by remember { mutableStateOf<Movimiento?>(null) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var busquedaConcepto    by remember { mutableStateOf("") }
    var expandedConceptos   by remember { mutableStateOf(false) }

    val egresosCategoria by dao.obtenerMovimientosPorTipoYCategoria("egreso", categoriaSeleccionada)
        .collectAsState(initial = emptyList())
    val conceptosCategoria by dao.obtenerConceptosPorTipoYCategoria("egreso", categoriaSeleccionada)
        .collectAsState(initial = emptyList())

    val conceptosFiltrados = conceptosCategoria.filter {
        it.contains(busquedaConcepto, ignoreCase = true)
    }
    val registrosConcepto = egresosCategoria.filter {
        it.concepto.equals(conceptoSeleccionado, ignoreCase = true)
    }
    val sugerenciasNuevo = conceptosCategoria.filter {
        it.contains(nuevoConcepto, ignoreCase = true) && nuevoConcepto.isNotBlank()
    }

    fun limpiarFormulario() {
        monto               = ""
        descripcion         = ""
        nuevoConcepto       = ""
        fechaSeleccionada   = System.currentTimeMillis()
        movimientoEnEdicion = null
        busquedaConcepto    = ""
    }

    fun tituloTopBar() = when (vista) {
        VistaEgreso.SELECCION_CATEGORIA -> "Agregar Egreso"
        VistaEgreso.LISTA_CONCEPTOS     -> categoriaSeleccionada
        VistaEgreso.FORMULARIO          ->
            if (conceptoSeleccionado.isNotBlank()) conceptoSeleccionado else "Nuevo concepto"
    }

    fun onBack() = when (vista) {
        VistaEgreso.SELECCION_CATEGORIA -> onVolver()
        VistaEgreso.LISTA_CONCEPTOS     -> { limpiarFormulario(); vista = VistaEgreso.SELECCION_CATEGORIA }
        VistaEgreso.FORMULARIO          -> { limpiarFormulario(); conceptoSeleccionado = ""; vista = VistaEgreso.LISTA_CONCEPTOS }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = fechaSeleccionada)
    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { fechaSeleccionada = it }
                    mostrarCalendario = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (mostrarDialogoEliminar && movimientoEnEdicion != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar egreso") },
            text  = { Text("¿Eliminar este registro de \"${movimientoEnEdicion!!.concepto}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.eliminarMovimiento(movimientoEnEdicion!!) }
                    limpiarFormulario(); conceptoSeleccionado = ""
                    vista = VistaEgreso.LISTA_CONCEPTOS
                    mostrarDialogoEliminar = false
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoEliminar = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tituloTopBar(), fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (vista == VistaEgreso.LISTA_CONCEPTOS) {
                        IconButton(onClick = {
                            limpiarFormulario(); conceptoSeleccionado = ""; nuevoConcepto = ""
                            vista = VistaEgreso.FORMULARIO
                        }) { Icon(Icons.Default.Add, contentDescription = "Nuevo concepto") }
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
            when (vista) {

                // ── PANTALLA 1: CATEGORÍAS ───────────────────────────────────
                VistaEgreso.SELECCION_CATEGORIA -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("¿Qué tipo de egreso deseas registrar?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 24.dp))
                    listOf(
                        Triple("Gasto Interno",  "Gastos propios del negocio",      "Interno"),
                        Triple("Gasto Externo",  "Pagos a proveedores u otros",     "Externo"),
                        Triple("Otros",          "Egreso sin categoría específica", "Otros")
                    ).forEach { (titulo, desc, clave) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                .clickable {
                                    categoriaSeleccionada = clave
                                    limpiarFormulario()
                                    vista = VistaEgreso.LISTA_CONCEPTOS
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LaundryFondoRojo)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(titulo, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium, color = LaundryRojo)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // ── PANTALLA 2: LISTA DE CONCEPTOS ───────────────────────────
                VistaEgreso.LISTA_CONCEPTOS -> {
                    OutlinedTextField(
                        value = busquedaConcepto,
                        onValueChange = { busquedaConcepto = it },
                        label = { Text("Buscar concepto...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (conceptosFiltrados.isEmpty() && busquedaConcepto.isBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sin conceptos registrados",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Toca + para agregar uno nuevo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(conceptosFiltrados) { concepto ->
                                val nombre = if (concepto.length > 30) concepto.take(27) + "..." else concepto
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                                        .clickable {
                                            conceptoSeleccionado = concepto
                                            limpiarFormulario()
                                            vista = VistaEgreso.FORMULARIO
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = LaundryFondoRojo)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(nombre, fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyLarge, color = LaundryRojo)
                                        Text("›", style = MaterialTheme.typography.titleLarge,
                                            color = LaundryRojo, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── PANTALLA 3: FORMULARIO ───────────────────────────────────
                VistaEgreso.FORMULARIO -> {

                    // Chip del concepto seleccionado o campo nuevo concepto
                    if (conceptoSeleccionado.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LaundryFondoRojo),
                            shape = RoundedCornerShape(8.dp)) {
                            Text(conceptoSeleccionado,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium, color = LaundryRojo)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        // Nuevo concepto con autocompletado
                        ExposedDropdownMenuBox(
                            expanded = expandedConceptos && sugerenciasNuevo.isNotEmpty(),
                            onExpandedChange = { expandedConceptos = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = nuevoConcepto,
                                onValueChange = { nuevoConcepto = it; expandedConceptos = true },
                                label = { Text("Nombre del concepto") },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                                    .fillMaxWidth(),
                                trailingIcon = {
                                    if (nuevoConcepto.isNotBlank() && sugerenciasNuevo.isNotEmpty())
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConceptos)
                                }
                            )
                            if (sugerenciasNuevo.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = expandedConceptos && sugerenciasNuevo.isNotEmpty(),
                                    onDismissRequest = { expandedConceptos = false }
                                ) {
                                    sugerenciasNuevo.forEach { s ->
                                        DropdownMenuItem(text = { Text(s) },
                                            onClick = { nuevoConcepto = s; expandedConceptos = false },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ── Descripción ──────────────────────────────────────────
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Monto ────────────────────────────────────────────────
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { monto = it },
                        label = { Text("Monto (Q)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Fecha ────────────────────────────────────────────────
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { mostrarCalendario = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Fecha del egreso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(sdf.format(Date(fechaSeleccionada)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Cambiar fecha",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // ── Registros anteriores ─────────────────────────────────
                    if (conceptoSeleccionado.isNotBlank() && registrosConcepto.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Registros anteriores",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        registrosConcepto.take(3).forEach { reg ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .clickable {
                                        movimientoEnEdicion = reg
                                        monto             = reg.monto.toString()
                                        descripcion       = reg.descripcion
                                        fechaSeleccionada = reg.fecha
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (movimientoEnEdicion?.id == reg.id)
                                        LaundryFondoRojo else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(sdf.format(Date(reg.fecha)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("- Q ${"%.2f".format(reg.monto)}",
                                            color = LaundryRojo, fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium)
                                    }
                                    // Mostrar descripción si existe
                                    if (reg.descripcion.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            if (reg.descripcion.length > 40)
                                                reg.descripcion.take(37) + "..."
                                            else reg.descripcion,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val conceptoFinal = if (conceptoSeleccionado.isNotBlank())
                        conceptoSeleccionado else nuevoConcepto

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val montoDouble = monto.toDoubleOrNull()
                                if (montoDouble != null && conceptoFinal.isNotBlank()) {
                                    scope.launch {
                                        if (movimientoEnEdicion == null) {
                                            dao.insertarMovimiento(Movimiento(
                                                concepto    = conceptoFinal,
                                                descripcion = descripcion,
                                                monto       = montoDouble,
                                                tipo        = "egreso",
                                                categoria   = categoriaSeleccionada,
                                                fecha       = fechaSeleccionada
                                            ))
                                        } else {
                                            dao.actualizarMovimiento(movimientoEnEdicion!!.copy(
                                                monto       = montoDouble,
                                                descripcion = descripcion,
                                                fecha       = fechaSeleccionada
                                            ))
                                        }
                                        limpiarFormulario()
                                        conceptoSeleccionado = ""
                                        vista = VistaEgreso.LISTA_CONCEPTOS
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (movimientoEnEdicion == null) "GUARDAR" else "ACTUALIZAR") }

                        if (movimientoEnEdicion != null) {
                            Button(onClick = { mostrarDialogoEliminar = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("ELIMINAR") }
                        }

                        OutlinedButton(
                            onClick = { limpiarFormulario(); conceptoSeleccionado = ""; vista = VistaEgreso.LISTA_CONCEPTOS },
                            modifier = Modifier.weight(1f)
                        ) { Text("CANCELAR") }
                    }
                }
            }
        }
    }
}