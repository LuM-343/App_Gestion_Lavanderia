package com.example.javeslaundry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarMovimiento(
    onVolver: () -> Unit,
    onAgregarIngreso: () -> Unit,
    onAgregarEgreso: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Agregar Movimiento", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onAgregarIngreso,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AGREGAR INGRESO")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAgregarEgreso,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AGREGAR EGRESO")
            }
        }
    }
}