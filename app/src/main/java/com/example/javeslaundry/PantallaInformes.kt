package com.example.javeslaundry

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text("Informes y Exportación", fontWeight = FontWeight.Bold) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Exportar datos a Excel",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "El archivo contendrá 3 hojas separadas:\n• Lavadas\n• Clientes\n• Movimientos",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
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
