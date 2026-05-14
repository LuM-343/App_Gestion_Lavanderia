package com.example.javeslaundry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Lavada
import com.example.javeslaundry.database.Movimiento
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    
    val esEntregada = lavada.estadoEntrega == "Entregada"
    val esPagada = lavada.estadoPago == "Pagado"

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar Lavada") },
            text = { Text("¿Estás seguro de que deseas eliminar esta lavada? Se registrará una devolución en movimientos si ya estaba pagada.") },
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
                        onEliminarExitoso()
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoEliminar = false }) { Text("Cancelar") } }
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
                    // Una vez entregada, no se puede editar el resto de datos
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "Cliente:", value = lavada.cliente)
                    InfoRow(label = "Tipo de Ropa:", value = lavada.tipoPrenda)
                    InfoRow(label = "Cantidad:", value = lavada.cantidad.toString())
                    InfoRow(label = "Precio:", value = "Q ${"%.2f".format(lavada.precio)}")
                    InfoRow(label = "Fecha Ingreso:", value = sdf.format(Date(lavada.fechaCreacion)))
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
                    if (lavada.fechaEntrega != null) {
                        InfoRow(label = "Fecha Entrega:", value = sdf.format(Date(lavada.fechaEntrega!!)))
                    }
                }
            }

            if (esEntregada) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Esta lavada ya fue entregada. Solo se permite actualizar el pago si está pendiente.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Acciones Rápidas
            Text("Acciones", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón de Pago: Habilitado si no está pagado (aunque esté entregada)
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(if (esPagada) "Pagado" else "Marcar Pagado", fontSize = 12.sp)
                }

                // Botón de Entrega: Deshabilitado si ya se entregó
                Button(
                    onClick = {
                        scope.launch {
                            dao.actualizarLavada(lavada.copy(estadoEntrega = "Entregada", fechaEntrega = System.currentTimeMillis()))
                            onVolver()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !esEntregada,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(if (esEntregada) "Entregado" else "Marcar Entregado", fontSize = 12.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* TODO: Generar Ticket */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar Ticket")
                }

                OutlinedButton(
                    onClick = { /* TODO: Contactar Cliente */ },
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
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = valueColor, fontWeight = if (valueColor != Color.Unspecified) FontWeight.Bold else FontWeight.Normal)
    }
}
