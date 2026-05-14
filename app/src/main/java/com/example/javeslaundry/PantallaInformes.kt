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

    // Estados para filtros
    var periodoSeleccionado by remember { mutableStateOf("Todo el tiempo") }
    val opcionesPeriodo = listOf("Un día", "1 semana", "Un mes", "6 meses atrás", "Todo el tiempo")

    var incluirLavadas by remember { mutableStateOf(true) }
    var incluirMovimientos by remember { mutableStateOf(true) }
    var incluirClientes by remember { mutableStateOf(true) }

    // Cálculos para las gráficas del mes actual
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val inicioMes = calendar.timeInMillis

    val movsMes = movimientos.filter { it.fecha >= inicioMes }

    // Segmentar Ingresos por Cliente
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

    // Segmentar Egresos por Concepto
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

    val coloresIngresos = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF009688), Color(0xFF00BCD4), Color(0xFF8BC34A))
    val coloresEgresos = listOf(Color(0xFFF44336), Color(0xFFFF9800), Color(0xFFFFC107), Color(0xFFE91E63), Color(0xFF9C27B0))

    val launcherGuardarArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val exito = exportarAExcel(
                    context, uri, periodoSeleccionado, 
                    clientes, lavadas, movimientos,
                    incluirLavadas, incluirMovimientos, incluirClientes
                )
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
            Text("Resumen Estadístico Mensual", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ChartContainer(title = "Ingresos", data = ingresosData, colors = coloresIngresos)
                ChartContainer(title = "Egresos", data = egresosData, colors = coloresEgresos)
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Configuración de Informe", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp))

            // Selector de Periodo
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Periodo del informe:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    opcionesPeriodo.forEach { opcion ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { periodoSeleccionado = opcion }.padding(vertical = 4.dp)) {
                            RadioButton(selected = (periodoSeleccionado == opcion), onClick = { periodoSeleccionado = opcion })
                            Text(text = opcion, modifier = Modifier.padding(start = 8.dp), fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Datos
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Datos a incluir:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DataSelectionRow("Lavadas", incluirLavadas) { incluirLavadas = it }
                    DataSelectionRow("Movimientos (Ingresos/Egresos)", incluirMovimientos) { incluirMovimientos = it }
                    DataSelectionRow("Lista de Clientes", incluirClientes) { incluirClientes = it }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!incluirLavadas && !incluirMovimientos && !incluirClientes) {
                        Toast.makeText(context, "Selecciona al menos un tipo de dato", Toast.LENGTH_SHORT).show()
                    } else {
                        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        val nombreArchivo = "Reporte_${periodoSeleccionado.replace(" ", "_")}_${sdf.format(Date())}.xlsx"
                        launcherGuardarArchivo.launch(nombreArchivo)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GENERAR INFORME EXCEL")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DataSelectionRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!isChecked) }.padding(vertical = 4.dp)) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Text(text = label, modifier = Modifier.padding(start = 8.dp), fontSize = 14.sp)
    }
}

@Composable
fun ChartContainer(title: String, data: List<Pair<String, Double>>, colors: List<Color>) {
    val total = data.sumOf { it.second }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(160.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        PieChart(data = data.mapIndexed { index, pair -> pair.second to colors[index % colors.size] }, modifier = Modifier.size(110.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Total: Q ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, size = Size(size.width, size.height))
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
    movimientos: List<Movimiento>,
    incluirLavadas: Boolean,
    incluirMovimientos: Boolean,
    incluirClientes: Boolean
): Boolean = withContext(Dispatchers.IO) {
    try {
        val workbook = XSSFWorkbook()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        val cal = Calendar.getInstance()
        val fechaLimite = when (periodo) {
            "Un día" -> { cal.add(Calendar.DAY_OF_YEAR, -1); cal.timeInMillis }
            "1 semana" -> { cal.add(Calendar.WEEK_OF_YEAR, -1); cal.timeInMillis }
            "Un mes" -> { cal.add(Calendar.MONTH, -1); cal.timeInMillis }
            "6 meses atrás" -> { cal.add(Calendar.MONTH, -6); cal.timeInMillis }
            else -> 0L
        }

        // 1. Hoja de Lavadas
        if (incluirLavadas) {
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
        }

        // 2. Hoja de Movimientos
        if (incluirMovimientos) {
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
        }

        // 3. Hoja de Clientes
        if (incluirClientes) {
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
