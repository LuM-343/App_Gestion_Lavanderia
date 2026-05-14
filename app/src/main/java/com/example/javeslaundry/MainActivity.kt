package com.example.javeslaundry
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javeslaundry.ui.theme.JavesLaundryTheme
import androidx.compose.runtime.*
import com.example.javeslaundry.database.AppDatabase
import com.example.javeslaundry.database.Lavada

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.laundryDao()

        setContent {
            JavesLaundryTheme {
                var pantalla by remember { mutableStateOf("principal") }
                var lavadaSeleccionada by remember { mutableStateOf<Lavada?>(null) }

                when (pantalla) {
                    "principal" -> PantallaPrincipal(
                        onClientesClick = { pantalla = "clientes" },
                        onLavadasClick = { pantalla = "lavadas" },
                        onMovimientosClick = { pantalla = "movimientos" },
                        onInformesClick = { pantalla = "informes" }
                    )
                    "clientes" -> PantallaClientes(
                        dao = dao,
                        onVolver = { pantalla = "principal" }
                    )
                    "lavadas" -> PantallaLavadas(
                        dao = dao,
                        onVolver = { pantalla = "principal" },
                        onAgregarLavada = { 
                            lavadaSeleccionada = null
                            pantalla = "formularioLavada" 
                        },
                        onVerDetalle = { lavada ->
                            lavadaSeleccionada = lavada
                            pantalla = "detalleLavada"
                        }
                    )
                    "detalleLavada" -> PantallaDetalleLavada(
                        dao = dao,
                        lavada = lavadaSeleccionada!!,
                        onVolver = { pantalla = "lavadas" },
                        onEditar = { pantalla = "formularioLavada" },
                        onEliminarExitoso = { pantalla = "lavadas" }
                    )
                    "formularioLavada" -> PantallaFormularioLavada(
                        dao = dao,
                        lavadaAEditar = lavadaSeleccionada,
                        onVolver = { 
                            if (lavadaSeleccionada == null) pantalla = "lavadas" 
                            else pantalla = "detalleLavada" 
                        },
                        onGuardarExitoso = { pantalla = "lavadas" }
                    )
                    "movimientos" -> PantallaMovimientos(
                        dao = dao,
                        onVolver = { pantalla = "principal" },
                        onAgregarMovimiento = { pantalla = "agregarMovimiento" }
                    )
                    "informes" -> PantallaInformes(
                        dao = dao,
                        onVolver = { pantalla = "principal" }
                    )
                    "agregarMovimiento" -> PantallaAgregarMovimiento(
                        onVolver = { pantalla = "movimientos" },
                        onAgregarIngreso = { pantalla = "agregarIngreso" },
                        onAgregarEgreso = { pantalla = "agregarEgreso" }
                    )
                    "agregarIngreso" -> PantallaAgregarIngreso(
                        dao = dao,
                        onVolver = { pantalla = "agregarMovimiento" },
                        onGuardarExitoso = { pantalla = "movimientos" }
                    )
                    "agregarEgreso" -> PantallaAgregarEgreso(
                        dao = dao,
                        onVolver = { pantalla = "agregarMovimiento" },
                        onGuardarExitoso = { pantalla = "movimientos" }
                    )
                }
            }
        }
    }
}


@Composable
fun PantallaPrincipal(
    onClientesClick: () -> Unit,
    onLavadasClick: () -> Unit,
    onMovimientosClick: () -> Unit,
    onInformesClick: () -> Unit,
)
{

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenido a",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Jave's Laundry",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        Button(
            onClick = onClientesClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Clientes", fontSize = 18.sp)
        }
        Button(
            onClick = onLavadasClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Lavadas", fontSize = 18.sp)
        }
        Button(
            onClick = onMovimientosClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Movimientos", fontSize = 18.sp)
        }
        Button(
            onClick = onInformesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Informes", fontSize = 18.sp)
        }
    }
}
