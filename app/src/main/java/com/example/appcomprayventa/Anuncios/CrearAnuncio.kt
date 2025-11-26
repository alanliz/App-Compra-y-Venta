package com.example.appcomprayventa.Anuncios

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.appcomprayventa.Adaptadores.AdaptadorImagenSeleccionada
import com.example.appcomprayventa.Constantes
import com.example.appcomprayventa.MainActivity
import com.example.appcomprayventa.Modelo.ModeloImagenSeleccionada // Nota: Revisa si tu carpeta es 'modelo' o 'Modelo'
import com.example.appcomprayventa.R
import com.example.appcomprayventa.databinding.ActivityCrearAnuncioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class CrearAnuncio : AppCompatActivity() {

    private lateinit var binding: ActivityCrearAnuncioBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth
    private var imagenUri: Uri? = null
    private lateinit var imagenSelecArrayList: ArrayList<ModeloImagenSeleccionada>
    private lateinit var adaptadorImagenSel: AdaptadorImagenSeleccionada

    private var marca = ""
    private var categoria = ""
    private var condicion = ""
    private var direccion = ""
    private var precio = ""
    private var titulo = ""
    private var descripcion = ""


    private var latitud = 0.0
    private var longitud = 0.0
    private var Edicion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearAnuncioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this).apply {
            setTitle("Por favor espere")
            setCanceledOnTouchOutside(false)
        }

        // Configurar Listas Desplegables (Adapters)
        val adaptadorCat = ArrayAdapter(this, R.layout.item_categoria, Constantes.categorias)
        binding.Categoria.setAdapter(adaptadorCat)

        val adaptadorCon = ArrayAdapter(this, R.layout.item_condicion, Constantes.condiciones)
        binding.Condicion.setAdapter(adaptadorCon)

        // Inicializar RecyclerView de Imágenes
        imagenSelecArrayList = ArrayList()
        cargarImagenes()

        // --- Listeners de Botones ---
        binding.agregarImg.setOnClickListener {
            mostrarOpciones()
        }

        binding.BtnCrearAnuncio.setOnClickListener {
            validarDatos()
        }
    }

    // --------------------------------------------------------------------------
    // SECCIÓN: LAUNCHERS (Permisos y Resultados de Activity)
    // --------------------------------------------------------------------------

    // 1. Resultado Permiso Almacenamiento
    private val solicitarPermisoAlmacenamiento = registerForActivityResult(ActivityResultContracts.RequestPermission()) { esConcedido ->
        if (esConcedido) {
            imagenGaleria()
        } else {
            Toast.makeText(this, "El permiso de almacenamiento ha sido denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Resultado Permiso Cámara
    private val solicitarPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        var todosConcedidos = true
        for (esConcedido in resultado.values) {
            todosConcedidos = todosConcedidos && esConcedido
        }
        if (todosConcedidos) {
            imagenCamara()
        } else {
            Toast.makeText(this, "El permiso de la cámara o almacenamiento ha sido denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Resultado Selección Galería
    private val resultadoGaleriaARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val data = resultado.data
            imagenUri = data?.data

            if (imagenUri != null) {
                val tiempo = "${Constantes.obtenerTiempoDis()}"
                val modeloImagenSel = ModeloImagenSeleccionada(
                    tiempo, imagenUri, null, false
                )
                imagenSelecArrayList.add(modeloImagenSel)
                cargarImagenes()
            }
        } else {
            Toast.makeText(this, "La selección de imagen se canceló", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. Resultado Captura Cámara
    private val resultadoCamara_ARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val tiempo = "${Constantes.obtenerTiempoDis()}"
            val modeloImagenSel = ModeloImagenSeleccionada(
                tiempo, imagenUri, null, false
            )
            imagenSelecArrayList.add(modeloImagenSel)
            cargarImagenes()
        } else {
            Toast.makeText(this, "La captura de imagen se canceló", Toast.LENGTH_SHORT).show()
        }
    }

    // --------------------------------------------------------------------------
    // SECCIÓN: FUNCIONES DE SELECCIÓN DE IMAGEN
    // --------------------------------------------------------------------------

    private fun mostrarOpciones() {
        val popupMenu = PopupMenu(this, binding.agregarImg)
        popupMenu.menu.add(Menu.NONE, 1, 1, "Cámara")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Galería")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == 1) {
                // Opción Cámara
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    solicitarPermisoCamara.launch(arrayOf(Manifest.permission.CAMERA))
                } else {
                    solicitarPermisoCamara.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            } else if (itemId == 2) {
                // Opción Galería
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    imagenGaleria()
                } else {
                    solicitarPermisoAlmacenamiento.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            true
        }
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleriaARL.launch(intent)
    }

    private fun imagenCamara() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Titulo_imagen")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion_imagen")

        imagenUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri)
        resultadoCamara_ARL.launch(intent)
    }

    private fun cargarImagenes() {
        adaptadorImagenSel = AdaptadorImagenSeleccionada(this, imagenSelecArrayList)
        binding.RVImagenes.adapter = adaptadorImagenSel
    }

    // --------------------------------------------------------------------------
    // SECCIÓN: VALIDACIÓN Y SUBIDA DE DATOS
    // --------------------------------------------------------------------------

    private fun validarDatos() {
        marca = binding.EtMarca.text.toString().trim()
        categoria = binding.Categoria.text.toString().trim()
        condicion = binding.Condicion.text.toString().trim()
        direccion = binding.Locacion.text.toString().trim()
        precio = binding.EtPrecio.text.toString().trim()
        titulo = binding.EtTitulo.text.toString().trim()
        descripcion = binding.EtDescripcion.text.toString().trim()

        when {
            marca.isEmpty() -> {
                binding.EtMarca.error = "Ingrese una marca"
                binding.EtMarca.requestFocus()
            }
            categoria.isEmpty() -> {
                binding.Categoria.error = "Ingrese una categoria"
                binding.Categoria.requestFocus()
            }
            condicion.isEmpty() -> {
                binding.Condicion.error = "Ingrese una condición"
                binding.Condicion.requestFocus()
            }
            precio.isEmpty() -> {
                binding.EtPrecio.error = "Ingrese un precio"
                binding.EtPrecio.requestFocus()
            }
            titulo.isEmpty() -> {
                binding.EtTitulo.error = "Ingrese un título"
                binding.EtTitulo.requestFocus()
            }
            descripcion.isEmpty() -> {
                binding.EtDescripcion.error = "Ingrese una descripción"
                binding.EtDescripcion.requestFocus()
            }
            imagenSelecArrayList.isEmpty() -> {
                Toast.makeText(this, "Agregue al menos una imagen", Toast.LENGTH_SHORT).show()
            }
            else -> {
                agregarAnuncio()
            }
        }
    }

    private fun agregarAnuncio() {
        progressDialog.setMessage("Agregando anuncio...")
        progressDialog.show()

        val tiempo = Constantes.obtenerTiempoDis()
        val ref = FirebaseDatabase.getInstance().getReference("Anuncios")
        val keyId = ref.push().key

        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "${keyId}"
        hashMap["uid"] = "${firebaseAuth.uid}"
        hashMap["marca"] = marca
        hashMap["categoria"] = categoria
        hashMap["condicion"] = condicion
        hashMap["direccion"] = direccion
        hashMap["precio"] = precio
        hashMap["titulo"] = titulo
        hashMap["descripcion"] = descripcion
        hashMap["estado"] = "${Constantes.anuncio_disponible}"
        hashMap["tiempo"] = tiempo
        hashMap["latitud"] = latitud
        hashMap["longitud"] = longitud
        hashMap["contadorVistas"] = 0

        if (keyId != null) {
            ref.child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener {
                    cargarImagenesStorage(keyId)
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error al agregar anuncio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cargarImagenesStorage(keyId: String) {
        for (i in imagenSelecArrayList.indices) {
            val modeloImagenSel = imagenSelecArrayList[i]

            if (!modeloImagenSel.deInternet) {
                val nombreImagen = modeloImagenSel.id
                val rutaNombreImagen = "Anuncios/$nombreImagen"

                val storageReference = FirebaseStorage.getInstance().getReference(rutaNombreImagen)

                // Asegurarse de que la URI no sea nula antes de subir
                if (modeloImagenSel.imagenUri != null) {
                    storageReference.putFile(modeloImagenSel.imagenUri!!)
                        .addOnSuccessListener { taskSnaphot ->
                            val uriTask = taskSnaphot.storage.downloadUrl
                            // Nota: El while aquí bloquea el hilo principal, considera usar Tasks.whenAll en el futuro
                            while (!uriTask.isSuccessful);

                            val urlImgCargada = uriTask.result

                            if (uriTask.isSuccessful) {
                                val hashMap = HashMap<String, Any>()
                                hashMap["id"] = "${modeloImagenSel.id}"
                                hashMap["imagenUrl"] = "$urlImgCargada"

                                val ref = FirebaseDatabase.getInstance().getReference("Anuncios")
                                ref.child(keyId).child("Imagenes")
                                    .child(nombreImagen)
                                    .updateChildren(hashMap)
                            }

                            // Verificar si es la última imagen para cerrar el diálogo
                            if (i == imagenSelecArrayList.size - 1) {
                                finalizarProceso()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    private fun finalizarProceso() {
        progressDialog.dismiss()
        if (Edicion) {
            Toast.makeText(this, "Se actualizó la información del anuncio", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@CrearAnuncio, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        } else {
            Toast.makeText(this, "Se publicó su anuncio", Toast.LENGTH_SHORT).show()
            limpiarCampos()
        }
    }

    private fun limpiarCampos() {
        binding.EtMarca.setText("")
        binding.Categoria.setText("")
        binding.Condicion.setText("")
        binding.Locacion.setText("")
        binding.EtPrecio.setText("")
        binding.EtTitulo.setText("")
        binding.EtDescripcion.setText("")
        imagenSelecArrayList.clear()
        adaptadorImagenSel.notifyDataSetChanged()
        imagenUri = null
    }
}