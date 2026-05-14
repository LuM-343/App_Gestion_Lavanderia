package com.example.javeslaundry

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javeslaundry.database.Cliente
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Lavada
import com.example.javeslaundry.database.Movimiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInformes(
    dao: LaundryDao,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val clientes by dao.obtenerClientes().collectAsState(initial = emptyList())
    val lavadas by dao.obtenerLavadas().collectAsState(initial = emptyList())
    val movimientos by dao.obtenerMovimientos().collectAsState(initial = emptyList())

    var periodoSeleccionado by remember { mutableStateOf("Todo") }
    val opcionesPeriodo = listOf("Último mes", "Últimos 6 meses", "Todo")

    // Cálculos para las gráficas del mes actual
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val inicioMes = calendar.timeInMillis

    val movsMes = movimientos.filter { it.fecha >= inicioMes }

    // Segmentar Ingresos por Cliente (Top 4 + Otros)
    val ingresosData = remember(movsMes) {
        val grouped = movsMes.filter { it.tipo == "ingreso" }
            .groupBy { 
                val c = it.concepto
                when {
                    c.contains("Pago lavada:", ignoreCase = true) -> c.substringAfter(":").trim().split(" (")[0]
                    c.contains("Pago lavada -", ignoreCase = true) -> c.substringAfter("-").trim().split(" (")[0]
                    else -> c
                }
            }
            .mapValues { it.value.sumOf { m -> m.monto } }
            .toList()
            .sortedByDescending { it.second }
        
        val top = grouped.take(4)
        val rest = grouped.drop(4).sumOf { it.second }
        val result = top.toMutableList()
        if (rest > 0) result.add("Otros" to rest)
        result
    }

    // Segmentar Egresos por Concepto (Top 4 + Otros)
    val egresosData = remember(movsMes) {
        val grouped = movsMes.filter { it.tipo == "egreso" }
            .groupBy { it.concepto }
            .mapValues { it.value.sumOf { m -> m.monto } }
            .toList()
            .sortedByDescending { it.second }
        
        val top = grouped.take(4)
        val rest = grouped.drop(4).sumOf { it.second }
        val result = top.toMutableList()
        if (rest > 0) result.add("Otros" to rest)
        result
    }

    val coloresIngresos = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF009688), 
        Color(0xFF00BCD4), Color(0xFF8BC34A)
    )
    val coloresEgresos = listOf(
        Color(0xFFF44336), Color(0xFFFF9800), Color(0xFFFFC107), 
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )

    val launcherGuardarArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val exito = exportarAExcel(context, uri, periodoSeleccionado, clientes, lavadas, movimientos)
                if (exito) {
                    Toast.makeText(context, "Informe exportado con éxito", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Error al exportar el informe", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Informes y Estadísticas", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Resumen Estadístico Mensual",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Fila de Gráficas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Gráfica de Ingresos
                ChartContainer(
                    title = "Ingresos",
                    data = ingresosData,
                    colors = coloresIngresos
                )

                // Gráfica de Egresos
                ChartContainer(
                    title = "Egresos",
                    data = egresosData,
                    colors = coloresEgresos
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Exportar a Excel",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Selecciona el periodo del informe:", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))

                    opcionesPeriodo.forEach { opcion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { periodoSeleccionado = opcion }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (periodoSeleccionado == opcion),
                                onClick = { periodoSeleccionado = opcion }
                            )
                            Text(text = opcion, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                    val nombreArchivo = "Informe_JavesLaundry_${sdf.format(Date())}.xlsx"
                    launcherGuardarArchivo.launch(nombreArchivo)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar y Descargar Excel")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ChartContainer(
    title: String,
    data: List<Pair<String, Double>>,
    colors: List<Color>
) {
    val total = data.sumOf { it.second }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(160.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        PieChart(
            data = data.mapIndexed { index, pair -> pair.second to colors[index % colors.size] }, 
            modifier = Modifier.size(110.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Total: Q ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.forEachIndexed { index, (label, value) ->
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        modifier = Modifier.size(8.dp).padding(top = 4.dp), 
                        color = colors[index % colors.size], 
                        shape = MaterialTheme.shapes.extraSmall
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(label, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2)
                        Text("Q ${"%.0f".format(value)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(data: List<Pair<Double, Color>>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.first }
    Canvas(modifier = modifier) {
        if (total == 0.0) {
            drawCircle(color = Color.LightGray, radius = size.minDimension / 2)
        } else {
            var startAngle = -90f
            data.forEach { (value, color) ->
                val sweepAngle = (value / total * 360f).toFloat()
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
    }
}

suspend fun exportarAExcel(
    context: Context,
    uri: Uri,
    periodo: String,
    clientes: List<Cliente>,
    lavadas: List<Lavada>,
    movimientos: List<Movimiento>
): Boolean = withContext(Dispatchers.IO) {
    try {
        val workbook = XSSFWorkbook()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        val fechaLimite = when (periodo) {
            "Último mes" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis
            }
            "Últimos 6 meses" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -6)
                cal.timeInMillis
            }
            else -> 0L
        }

        // 1. Hoja de Lavadas
        val sheetLavadas = workbook.createSheet("Lavadas")
        val headerLavadas = sheetLavadas.createRow(0)
        val headersL = listOf("ID", "Fecha Creación", "Cliente", "Prenda", "Cantidad", "Precio", "Estado Pago", "Estado Entrega", "Fecha Entrega")
        headersL.forEachIndexed { i, h -> headerLavadas.createCell(i).setCellValue(h) }

        var rowIdxL = 1
        lavadas.filter { it.fechaCreacion >= fechaLimite }.forEach { lavada ->
            val row = sheetLavadas.createRow(rowIdxL++)
            row.createCell(0).setCellValue(lavada.id.toDouble())
            row.createCell(1).setCellValue(sdf.format(Date(lavada.fechaCreacion)))
            row.createCell(2).setCellValue(lavada.cliente)
            row.createCell(3).setCellValue(lavada.tipoPrenda)
            row.createCell(4).setCellValue(lavada.cantidad.toDouble())
            row.createCell(5).setCellValue(lavada.precio)
            row.createCell(6).setCellValue(lavada.estadoPago)
            row.createCell(7).setCellValue(lavada.estadoEntrega)
            row.createCell(8).setCellValue(lavada.fechaEntrega?.let { sdf.format(Date(it)) } ?: "Pendiente")
        }

        // 2. Hoja de Clientes
        val sheetClientes = workbook.createSheet("Clientes")
        val headerClientes = sheetClientes.createRow(0)
        val headersC = listOf("ID", "Nombre", "Teléfono", "Dirección")
        headersC.forEachIndexed { i, h -> headerClientes.createCell(i).setCellValue(h) }

        var rowIdxC = 1
        clientes.forEach { cliente ->
            val row = sheetClientes.createRow(rowIdxC++)
            row.createCell(0).setCellValue(cliente.id.toDouble())
            row.createCell(1).setCellValue(cliente.nombre)
            row.createCell(2).setCellValue(cliente.telefono)
            row.createCell(3).setCellValue(cliente.direccion)
        }

        // 3. Hoja de Movimientos
        val sheetMovs = workbook.createSheet("Movimientos")
        val headerMovs = sheetMovs.createRow(0)
        val headersM = listOf("ID", "Fecha", "Concepto", "Monto", "Tipo")
        headersM.forEachIndexed { i, h -> headerMovs.createCell(i).setCellValue(h) }

        var rowIdxM = 1
        movimientos.filter { it.fecha >= fechaLimite }.forEach { mov ->
            val row = sheetMovs.createRow(rowIdxM++)
            row.createCell(0).setCellValue(mov.id.toDouble())
            row.createCell(1).setCellValue(sdf.format(Date(mov.fecha)))
            row.createCell(2).setCellValue(mov.concepto)
            row.createCell(3).setCellValue(mov.monto)
            row.createCell(4).setCellValue(mov.tipo)
        }

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
