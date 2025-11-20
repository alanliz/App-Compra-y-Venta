package com.example.appcomprayventa

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale


object Constantes {
    fun obtenerTiempoDis():
            Long{ return System.currentTimeMillis()}

    fun obtenerFecha(tiempo: Long): String {
        val calendario = Calendar.getInstance(Locale.ENGLISH)
        calendario.timeInMillis = tiempo

        return DateFormat.format("dd/MM/yyyy", calendario).toString()
    }

    const val anuncio_disponible = "Disponible"
    const val anuncio_vendido = "Vendido"

    val categorias = arrayOf(
        "Celulares",
        "Computadoras",
        "Electronica y Electrodomesticos",
        "Automoviles",
        "Consolas y videojuegos",
        "Hogar y muebles",
        "Belleza y cuidado personal",
        "Libros",
        "Deportes"
    )

    val condiciones = arrayOf(
        "Nuevo",
        "Usado",
        "Reacondicionado"
    )
}