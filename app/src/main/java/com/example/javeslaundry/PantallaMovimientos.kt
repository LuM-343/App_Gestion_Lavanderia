package com.example.javeslaundry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Movimiento
import java.text.SimpleDateFormat
import java.util.*

val LaundryVerde      = Color(red = 46,  green = 125, blue = 50)
val LaundryRojo       = Color(red = 198, green = 40,  blue = 40)
val LaundryFondoVerde = Color(red = 232, green = 245, blue = 233)
val LaundryFondoRojo  = Color(red = 255, green = 235, blue = 238)

// Niveles de navegación dentro de la pantalla
private enum class VistaMovimientos { GENERAL, INGRESOS, EGRESOS }
private enum class NivelDetalle    { CATEGORIAS, CONCEPTOS, REGISTROS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMovimientos(
    dao: LaundryDao,
    onVolver: () -> Unit,
    onAgregarMovimiento: () -> Unit
) {
    val todosMovimientos by dao.obtenerMovimientos().collectAsState(initial = emptyList())
    val movimientosAsc   = todosMovimientos.sortedBy { it.fecha }
    val formatoFecha     = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Navegación
    var vistaActual       by remember { mutableStateOf(VistaMovimientos.GENERAL) }
    var nivelDetalle      by remember { mutableStateOf(NivelDetalle.CATEGORIAS) }
    var categoriaActual   by remember { mutableStateOf("") }
    var conceptoActual    by remember { mutableStateOf("") }
    var busquedaConcepto  by remember { mutableStateOf("") }

    // Calendario
    val dateRangePickerState = rememberDateRangePickerState()
    var mostrarCalendario    by remember { mutableStateOf(false) }

    // Balance total siempre sobre todos
    val balances  = mutableListOf<Double>()
    var acumulado = 0.0
    for (mov in movimientosAsc) {
        acumulado += if (mov.tipo == "ingreso") mov.monto else -mov.monto
        balances.add(acumulado)
    }
    val balanceFinal  = balances.lastOrNull() ?: 0.0
    val todasLasFilas = movimientosAsc.zip(balances).reversed()

    fun filtrarPorFecha(lista: List<Pair<Movimiento, Double>>): List<Pair<Movimiento, Double>> {
        val inicio = dateRangePickerState.selectedStartDateMillis
        val fin    = dateRangePickerState.selectedEndDateMillis
        return lista.filter { (mov, _) ->
            when {
                inicio != null && fin != null -> mov.fecha in inicio..fin + 86399999L
                inicio != null               -> mov.fecha >= inicio
                else                         -> true
            }
        }
    }

    // Título dinámico
    val tituloTopBar = when (vistaActual) {
        VistaMovimientos.GENERAL  -> "Movimientos"
        VistaMovimientos.INGRESOS -> when (nivelDetalle) {
            NivelDetalle.CATEGORIAS -> "Ingresos"
            NivelDetalle.CONCEPTOS  -> categoriaActual
            NivelDetalle.REGISTROS  -> conceptoActual
        }
        VistaMovimientos.EGRESOS  -> when (nivelDetalle) {
            NivelDetalle.CATEGORIAS -> "Egresos"
            NivelDetalle.CONCEPTOS  -> categoriaActual
            NivelDetalle.REGISTROS  -> conceptoActual
        }
    }

    // Navegación interna atrás
    fun onBackInterno() {
        when (vistaActual) {
            VistaMovimientos.GENERAL  -> onVolver()
            VistaMovimientos.INGRESOS -> when (nivelDetalle) {
                NivelDetalle.CATEGORIAS -> { vistaActual = VistaMovimientos.GENERAL }
                NivelDetalle.CONCEPTOS  -> {
                    categoriaActual  = ""
                    busquedaConcepto = ""
                    nivelDetalle     = NivelDetalle.CATEGORIAS
                }
                NivelDetalle.REGISTROS  -> {
                    conceptoActual   = ""
                    busquedaConcepto = ""
                    nivelDetalle     = NivelDetalle.CONCEPTOS
                }
            }
            VistaMovimientos.EGRESOS  -> when (nivelDetalle) {
                NivelDetalle.CATEGORIAS -> { vistaActual = VistaMovimientos.GENERAL }
                NivelDetalle.CONCEPTOS  -> {
                    categoriaActual  = ""
                    busquedaConcepto = ""
                    nivelDetalle     = NivelDetalle.CATEGORIAS
                }
                NivelDetalle.REGISTROS  -> {
                    conceptoActual   = ""
                    busquedaConcepto = ""
                    nivelDetalle     = NivelDetalle.CONCEPTOS
                }
            }
        }
    }

    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dateRangePickerState.setSelection(null, null)
                    mostrarCalendario = false
                }) { Text("Limpiar") }
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
                title = {
                    Text(tituloTopBar, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = { onBackInterno() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarCalendario = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Filtrar por fecha",
                            tint = if (dateRangePickerState.selectedStartDateMillis != null)
                                MaterialTheme.colorScheme.primary
                            else LocalContentColor.current
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Botón agregar solo en vista principal
            if (vistaActual == VistaMovimientos.GENERAL) {
                Button(
                    onClick = onAgregarMovimiento,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("AGREGAR MOVIMIENTO") }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tarjeta balance siempre visible
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (balanceFinal >= 0) LaundryFondoVerde else LaundryFondoRojo
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Balance actual",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Q ${"%.2f".format(balanceFinal)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (balanceFinal >= 0) LaundryVerde else LaundryRojo
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chips General/Ingresos/Egresos solo en nivel raíz
            if (nivelDetalle == NivelDetalle.CATEGORIAS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        VistaMovimientos.GENERAL  to "General",
                        VistaMovimientos.INGRESOS to "Ingresos",
                        VistaMovimientos.EGRESOS  to "Egresos"
                    ).forEach { (v, label) ->
                        FilterChip(
                            selected = vistaActual == v,
                            onClick  = {
                                vistaActual      = v
                                categoriaActual  = ""
                                conceptoActual   = ""
                                busquedaConcepto = ""
                                nivelDetalle     = NivelDetalle.CATEGORIAS
                            },
                            label    = { Text(label, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Contenido ────────────────────────────────────────────────────
            when (vistaActual) {

                // GENERAL ────────────────────────────────────────────────────
                VistaMovimientos.GENERAL -> {
                    val filas = filtrarPorFecha(todasLasFilas)
                    if (filas.isEmpty()) {
                        MensajeVacio("Sin movimientos registrados")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filas) { (mov, bal) ->
                                TarjetaFilaMovimiento(
                                    fecha    = formatoFecha.format(Date(mov.fecha)),
                                    concepto = mov.concepto,
                                    ingreso  = if (mov.tipo == "ingreso") mov.monto else null,
                                    egreso   = if (mov.tipo == "egreso")  mov.monto else null,
                                    balance  = bal
                                )
                            }
                        }
                    }
                }

                // INGRESOS ───────────────────────────────────────────────────
                VistaMovimientos.INGRESOS -> {
                    val tipo = "ingreso"
                    val colorFondo  = LaundryFondoVerde
                    val colorTexto  = LaundryVerde

                    when (nivelDetalle) {

                        // Nivel 1: categorías fijas
                        NivelDetalle.CATEGORIAS -> {
                            listOf(
                                "Interno" to "Ingresos propios del negocio",
                                "Externo" to "Ingresos de clientes u otros",
                                "Otros"   to "Ingresos sin categoría específica"
                            ).forEach { (cat, desc) ->
                                TarjetaCategoriaMov(
                                    nombre     = cat,
                                    descripcion = desc,
                                    colorFondo = colorFondo,
                                    colorTexto = colorTexto,
                                    onClick    = {
                                        categoriaActual  = cat
                                        busquedaConcepto = ""
                                        nivelDetalle     = NivelDetalle.CONCEPTOS
                                    }
                                )
                            }
                        }

                        // Nivel 2: conceptos de esa categoría
                        NivelDetalle.CONCEPTOS -> {
                            val conceptos = todosMovimientos
                                .filter { it.tipo == tipo && it.categoria == categoriaActual }
                                .map { it.concepto }
                                .distinct()
                                .filter { it.contains(busquedaConcepto, ignoreCase = true) }

                            OutlinedTextField(
                                value = busquedaConcepto,
                                onValueChange = { busquedaConcepto = it },
                                label = { Text("Buscar concepto...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Opción General siempre visible
                            TarjetaCategoriaMov(
                                nombre      = "General",
                                descripcion = "Ver todos los ingresos de $categoriaActual",
                                colorFondo  = colorFondo,
                                colorTexto  = colorTexto,
                                onClick     = {
                                    conceptoActual = ""
                                    nivelDetalle   = NivelDetalle.REGISTROS
                                }
                            )

                            if (conceptos.isEmpty()) {
                                MensajeVacio("Sin conceptos en $categoriaActual")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(conceptos) { concepto ->
                                        val nombreMostrar = if (concepto.length > 30)
                                            concepto.take(27) + "..." else concepto
                                        TarjetaCategoriaMov(
                                            nombre      = nombreMostrar,
                                            descripcion = null,
                                            colorFondo  = colorFondo,
                                            colorTexto  = colorTexto,
                                            onClick     = {
                                                conceptoActual   = concepto
                                                busquedaConcepto = ""
                                                nivelDetalle     = NivelDetalle.REGISTROS
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Nivel 3: registros del concepto
                        NivelDetalle.REGISTROS -> {
                            val movsFiltrados = todosMovimientos
                                .filter {
                                    it.tipo == tipo &&
                                            it.categoria == categoriaActual &&
                                            (conceptoActual.isBlank() || it.concepto == conceptoActual)
                                }
                                .sortedBy { it.fecha }

                            // Balance parcial de este subconjunto
                            val pares = movsFiltrados
                                .runningFold(0.0) { acc, mov -> acc + mov.monto }
                                .drop(1)
                                .zip(movsFiltrados)
                                .map { (bal, mov) -> mov to bal }
                                .reversed()

                            val paresFiltrados = filtrarPorFecha(pares)

                            if (paresFiltrados.isEmpty()) {
                                MensajeVacio("Sin registros")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(paresFiltrados) { (mov, bal) ->
                                        TarjetaFilaMovimiento(
                                            fecha    = formatoFecha.format(Date(mov.fecha)),
                                            concepto = mov.concepto,
                                            ingreso  = mov.monto,
                                            egreso   = null,
                                            balance  = bal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // EGRESOS ────────────────────────────────────────────────────
                VistaMovimientos.EGRESOS -> {
                    val tipo       = "egreso"
                    val colorFondo = LaundryFondoRojo
                    val colorTexto = LaundryRojo

                    when (nivelDetalle) {

                        NivelDetalle.CATEGORIAS -> {
                            listOf(
                                "Interno" to "Gastos propios del negocio",
                                "Externo" to "Pagos a proveedores u otros",
                                "Otros"   to "Egresos sin categoría específica"
                            ).forEach { (cat, desc) ->
                                TarjetaCategoriaMov(
                                    nombre      = cat,
                                    descripcion = desc,
                                    colorFondo  = colorFondo,
                                    colorTexto  = colorTexto,
                                    onClick     = {
                                        categoriaActual  = cat
                                        busquedaConcepto = ""
                                        nivelDetalle     = NivelDetalle.CONCEPTOS
                                    }
                                )
                            }
                        }

                        NivelDetalle.CONCEPTOS -> {
                            val conceptos = todosMovimientos
                                .filter { it.tipo == tipo && it.categoria == categoriaActual }
                                .map { it.concepto }
                                .distinct()
                                .filter { it.contains(busquedaConcepto, ignoreCase = true) }

                            OutlinedTextField(
                                value = busquedaConcepto,
                                onValueChange = { busquedaConcepto = it },
                                label = { Text("Buscar concepto...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Opción General
                            TarjetaCategoriaMov(
                                nombre      = "General",
                                descripcion = "Ver todos los egresos de $categoriaActual",
                                colorFondo  = colorFondo,
                                colorTexto  = colorTexto,
                                onClick     = {
                                    conceptoActual = ""
                                    nivelDetalle   = NivelDetalle.REGISTROS
                                }
                            )

                            if (conceptos.isEmpty()) {
                                MensajeVacio("Sin conceptos en $categoriaActual")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(conceptos) { concepto ->
                                        val nombreMostrar = if (concepto.length > 30)
                                            concepto.take(27) + "..." else concepto
                                        TarjetaCategoriaMov(
                                            nombre      = nombreMostrar,
                                            descripcion = null,
                                            colorFondo  = colorFondo,
                                            colorTexto  = colorTexto,
                                            onClick     = {
                                                conceptoActual   = concepto
                                                busquedaConcepto = ""
                                                nivelDetalle     = NivelDetalle.REGISTROS
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        NivelDetalle.REGISTROS -> {
                            val movsFiltrados = todosMovimientos
                                .filter {
                                    it.tipo == tipo &&
                                            it.categoria == categoriaActual &&
                                            (conceptoActual.isBlank() || it.concepto == conceptoActual)
                                }
                                .sortedBy { it.fecha }

                            val pares = movsFiltrados
                                .runningFold(0.0) { acc, mov -> acc + mov.monto }
                                .drop(1)
                                .zip(movsFiltrados)
                                .map { (bal, mov) -> mov to bal }
                                .reversed()

                            val paresFiltrados = filtrarPorFecha(pares)

                            if (paresFiltrados.isEmpty()) {
                                MensajeVacio("Sin registros")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(paresFiltrados) { (mov, bal) ->
                                        TarjetaFilaMovimiento(
                                            fecha    = formatoFecha.format(Date(mov.fecha)),
                                            concepto = mov.concepto,
                                            ingreso  = null,
                                            egreso   = mov.monto,
                                            balance  = bal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tarjeta de categoría/concepto navegable ──────────────────────────────────
@Composable
fun TarjetaCategoriaMov(
    nombre: String,
    descripcion: String?,
    colorFondo: Color,
    colorTexto: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (descripcion != null) 14.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nombre,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorTexto)
                if (descripcion != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("›",
                style = MaterialTheme.typography.titleLarge,
                color = colorTexto,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ── Tarjeta de fila de movimiento ────────────────────────────────────────────
@Composable
fun TarjetaFilaMovimiento(
    fecha: String,
    concepto: String,
    ingreso: Double?,
    egreso: Double?,
    balance: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(concepto,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (ingreso != null) "+ Q ${"%.2f".format(ingreso)}"
                    else "- Q ${"%.2f".format(egreso)}",
                    color    = if (ingreso != null) LaundryVerde else LaundryRojo,
                    fontWeight = FontWeight.Bold,
                    style    = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(fecha,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Balance: Q ${"%.2f".format(balance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (balance >= 0) LaundryVerde else LaundryRojo,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Mensaje vacío ────────────────────────────────────────────────────────────
@Composable
fun MensajeVacio(texto: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}