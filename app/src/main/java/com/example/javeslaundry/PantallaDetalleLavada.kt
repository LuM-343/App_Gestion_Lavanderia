package com.example.javeslaundry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Lavada
import com.example.javeslaundry.database.Movimiento
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleLavada(
    dao: LaundryDao,
    lavada: Lavada,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminarExitoso: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    
    val esEntregada = lavada.estadoEntrega == "Entregada"
    val esPagada = lavada.estadoPago == "Pagado"

    // Launcher para seleccionar dónde guardar el PDF del Ticket
    val launcherGuardarTicket = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val exito = generarPdfTicket(context, uri, lavada)
                if (exito) {
                    Toast.makeText(context, "Ticket PDF generado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar Lavada") },
            text = { Text("¿Estás seguro de que deseas eliminar esta lavada? Si ya fue pagada, se registrará una devolución automática en movimientos.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (lavada.estadoPago == "Pagado") {
                            dao.insertarMovimiento(
                                Movimiento(
                                    concepto = "Devolución por eliminación: ${lavada.cliente}",
                                    monto = lavada.precio,
                                    tipo = "egreso"
                                )
                            )
                        }
                        dao.eliminarLavada(lavada)
                        mostrarDialogoEliminar = false
                        onEliminarExitoso()
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
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
                title = { Text("Detalle de Lavada", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!esEntregada) {
                        IconButton(onClick = onEditar) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                    IconButton(onClick = { mostrarDialogoEliminar = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(label = "Folio:", value = "#${lavada.id}")
                    InfoRow(label = "Cliente:", value = lavada.cliente)
                    InfoRow(label = "Prendas:", value = lavada.tipoPrenda)
                    InfoRow(label = "Cantidad:", value = "${lavada.cantidad} uds.")
                    InfoRow(label = "Precio Total:", value = "Q ${"%.2f".format(lavada.precio)}")
                    InfoRow(label = "Fecha Recibido:", value = sdf.format(Date(lavada.fechaCreacion)))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    InfoRow(
                        label = "Estado Pago:", 
                        value = lavada.estadoPago,
                        valueColor = if (esPagada) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    InfoRow(
                        label = "Estado Entrega:", 
                        value = lavada.estadoEntrega,
                        valueColor = if (esEntregada) Color(0xFF1976D2) else Color(0xFFF57C00)
                    )
                    
                    lavada.fechaEntrega?.let {
                        InfoRow(label = "Fecha Entrega:", value = sdf.format(Date(it)))
                    }
                }
            }

            if (esEntregada) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Servicio Finalizado: Esta lavada ya fue entregada y sus datos base están protegidos.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("Gestión de Estado", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            dao.actualizarLavada(lavada.copy(estadoPago = "Pagado"))
                            dao.insertarMovimiento(Movimiento(concepto = "Pago lavada: ${lavada.cliente}", monto = lavada.precio, tipo = "ingreso"))
                            onVolver()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !esPagada,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(if (esPagada) "PAGADO" else "COBRAR", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        scope.launch {
                            dao.actualizarLavada(lavada.copy(estadoEntrega = "Entregada", fechaEntrega = System.currentTimeMillis()))
                            onVolver()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !esEntregada,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text(if (esEntregada) "ENTREGADO" else "ENTREGAR", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { 
                        val nombreSugerido = "Ticket_Laundry_${lavada.id}_${lavada.cliente.replace(" ", "_")}.pdf"
                        launcherGuardarTicket.launch(nombreSugerido)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERAR TICKET PDF")
                }

                OutlinedButton(
                    onClick = { 
                        Toast.makeText(context, "Función de contacto próximamente...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContactPhone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contactar Cliente")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(text = value, color = valueColor, fontWeight = if (valueColor != Color.Unspecified) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}

suspend fun generarPdfTicket(context: Context, uri: Uri, lavada: Lavada): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = PdfWriter(outputStream)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            // --- INTENTO DE AGREGAR LOGO ---
            try {
                // Se asume que guardaste la imagen como res/drawable/logo_laundry.png
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logotipo)
                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val imageData = ImageDataFactory.create(stream.toByteArray())
                    val logo = Image(imageData).apply {
                        setWidth(120f)
                        setHorizontalAlignment(HorizontalAlignment.CENTER)
                    }
                    document.add(logo)
                }
            } catch (e: Exception) {
                // Si la imagen no existe, simplemente continuamos con el texto
                e.printStackTrace()
            }

            // Encabezado
            document.add(Paragraph("JAVE'S LAUNDRY")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(22f)
                .setBold())
            document.add(Paragraph("Servicio de Lavandería")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f))
            
            document.add(Paragraph("\n"))
            document.add(LineSeparator(SolidLine()))
            document.add(Paragraph("\n"))

            // Cuerpo
            document.add(Paragraph("TICKET DE SERVICIO")
                .setBold()
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.CENTER))
            
            document.add(Paragraph("\n"))
            document.add(Paragraph("Orden No: ${lavada.id}").setBold())
            document.add(Paragraph("Fecha: ${sdf.format(Date(lavada.fechaCreacion))}"))
            document.add(Paragraph("Cliente: ${lavada.cliente.uppercase()}"))
            document.add(Paragraph("Prendas: ${lavada.tipoPrenda}"))
            document.add(Paragraph("Cantidad: ${lavada.cantidad} unidades"))
            
            document.add(Paragraph("\n"))
            document.add(Paragraph("TOTAL A PAGAR: Q ${"%.2f".format(lavada.precio)}")
                .setFontSize(18f)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT))
            
            document.add(Paragraph("\n"))
            document.add(Paragraph("Estado del Pago: ${lavada.estadoPago.uppercase()}"))
            document.add(Paragraph("Estado de Entrega: ${lavada.estadoEntrega.uppercase()}"))
            
            lavada.fechaEntrega?.let {
                document.add(Paragraph("Fecha de Entrega: ${sdf.format(Date(it))}"))
            }

            document.add(Paragraph("\n"))
            document.add(LineSeparator(SolidLine()))
            document.add(Paragraph("\n"))
            
            document.add(Paragraph("¡Gracias por su preferencia!")
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic())

            document.close()
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
