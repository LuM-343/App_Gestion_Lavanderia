package com.example.javeslaundry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.javeslaundry.database.LaundryDao
import com.example.javeslaundry.database.Movimiento
import kotlinx.coroutines.launch

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Egreso",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Monto") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = concepto,
                onValueChange = { concepto = it },
                label = { Text("Concepto") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val montoDouble = monto.toDoubleOrNull()

                        if (montoDouble != null && concepto.isNotBlank()) {
                            scope.launch {
                                dao.insertarMovimiento(
                                    Movimiento(
                                        concepto = concepto,
                                        monto = montoDouble,
                                        tipo = "egreso"
                                    )
                                )
                                onGuardarExitoso()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("GUARDAR")
                }

                Button(
                    onClick = onVolver,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCELAR")
                }
            }
        }
    }
}