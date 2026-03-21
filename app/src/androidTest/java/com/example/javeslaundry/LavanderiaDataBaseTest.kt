package com.example.javeslaundry
// Deja tu línea de package aquí arriba

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.javeslaundry.database.Cliente
import com.example.javeslaundry.database.LavanderiaDao
import com.example.javeslaundry.database.LavanderiaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LavanderiaDatabaseTest {

    private lateinit var db: LavanderiaDatabase
    private lateinit var dao: LavanderiaDao

    @Before
    fun setup() {
        // Creamos una base de datos temporal en la memoria RAM solo para esta prueba
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LavanderiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lavanderiaDao()
    }

    @After
    fun teardown() {
        db.close() // Cerramos la BD al terminar
    }

    @Test
    fun pruebaInsertarYLeerCliente() = runBlocking {
        val nuevoCliente = Cliente(
            nombre = "Juan Pérez",
            telefono = 55512345,
            direccion = "Calle Principal 123",
            extra = "Entregar ropa planchada"
        )

        // 2. Lo insertamos en la BD y guardamos el ID que Room le asigna
        val idGenerado = dao.insertarCliente(nuevoCliente)

        // 3. Leemos todos los clientes de la BD
        val clientesGuardados = dao.obtenerClientes()

        // 4. Imprimimos los resultados para tu captura de pantalla
        println("=========================================")
        println("   PRUEBA DE BASE DE DATOS LAVANDERÍA    ")
        println(" Ahora estoy HAHA ")
        println("=========================================")
        println("-> Cliente insertado exitosamente con ID: $idGenerado")
        println("-> Total de clientes en BD: ${clientesGuardados.size}")

        val clienteLeido = clientesGuardados[0]
        println("-> Datos leídos de la BD:")
        println("   Nombre: ${clienteLeido.nombre}")
        println("   Teléfono: ${clienteLeido.telefono}")
        println("   Dirección: ${clienteLeido.direccion}")
        println("=========================================")
    }
}