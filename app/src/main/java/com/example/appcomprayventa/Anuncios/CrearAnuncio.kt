package com.example.appcomprayventa.Anuncios

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.appcomprayventa.Adaptadores.AdaptadorImagenSeleccionada
import com.example.appcomprayventa.Constantes
import com.example.appcomprayventa.Modelo.ModeloImagenSeleccionada
import com.example.appcomprayventa.R
import com.example.appcomprayventa.databinding.ActivityCrearAnuncioBinding
import com.google.firebase.auth.FirebaseAuth

class CrearAnuncio : AppCompatActivity() {

    private lateinit var binding: ActivityCrearAnuncioBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var imagenUri : Uri? = null
     private lateinit var imagenSelecArrayList: ArrayList<ModeloImagenSeleccionada>
    private lateinit var adaptadorImagenSeleccionada: AdaptadorImagenSeleccionada



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearAnuncioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        val adaptadorCat = ArrayAdapter(this, R.layout.item_categoria, Constantes.categorias)
        binding.Categoria.setAdapter(adaptadorCat)

        val adaptadorCon = ArrayAdapter(this, R.layout.item_condicion, Constantes.condiciones)
        binding.Condicion.setAdapter(adaptadorCon)





    }
}