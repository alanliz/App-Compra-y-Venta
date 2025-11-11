package com.example.appcomprayventa

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.appcomprayventa.databinding.ActivityEditarPerfilBinding
import com.google.firebase.storage.FirebaseStorage

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var imageUri: Uri? = null

    private var nombres = ""
    private var f_nac = ""
    private var codigo = ""
    private var telefono = ""

    private fun validarInfo(){
        nombres = binding.EtNombres.text.toString().trim()
        f_nac = binding.EtFNac.text.toString().trim()
        codigo = binding.selectorCod.selectedCountryCodeWithPlus
        telefono = binding.EtTelefono.text.toString().trim()

        if(nombres.isEmpty()){
            Toast.makeText(this, "Ingrese sus nombres", Toast.LENGTH_SHORT).show()
        } else if(f_nac.isEmpty()) {
            Toast.makeText(this, "Ingrese su fecha de nacimiento", Toast.LENGTH_SHORT).show()
        }else if(codigo.isEmpty()) {
            Toast.makeText(this, "Ingrese su código de país", Toast.LENGTH_SHORT).show()
        }else if(telefono.isEmpty()) {
            Toast.makeText(this, "Ingrese su número de teléfono", Toast.LENGTH_SHORT).show()
        }else{
            actualizarInfo()
        }

    }

    private fun actualizarInfo() {
        progressDialog.setMessage("Actualizando información")
        progressDialog.show()

        val hashMap : HashMap<String, Any> = HashMap()
        hashMap["nombres"] = "${nombres}"
        hashMap["fecha_nac"] = "${f_nac}"
        hashMap["codigoTelefono"] = "${codigo}"
        hashMap["telefono"] = "${telefono}"

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Información actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e  ->
                progressDialog.dismiss()
                Toast.makeText(this, "ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        cargarInfo()

        binding.BtnActualizar.setOnClickListener {
            validarInfo()
        }


        binding.FABCambiarImg.setOnClickListener {
            selec_imagen_de()
        }
    }

    private fun cargarInfo() {

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener( object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    val f_nac = "${snapshot.child("fecha_nac").value}"
                    val telefono = "${snapshot.child("telefono").value}"
                    val codTelefono = "${snapshot.child("codigoTelefono").value}"

                    //Establecer los valores
                    binding.EtNombres.setText(nombres)
                    binding.EtFNac.setText(f_nac)
                    binding.EtTelefono.setText(telefono)

                    try{
                        Glide.with(applicationContext)
                            .load(imagen)
                            .placeholder(R.drawable.img_perfil)
                            .into(binding.imgPerfil)
                    }catch(e: Exception){
                        Toast.makeText(this@EditarPerfil,
                            "${e.message}",
                            Toast.LENGTH_SHORT).show()

                    }

                    try{
                        val codigo = codTelefono.replace("+", "").toInt()
                        binding.selectorCod.setCountryForPhoneCode(codigo)

                    }catch (e: Exception){
//                        Toast.makeText(this@EditarPerfil,
//                            "${e.message}",
//                            Toast.LENGTH_SHORT).show()
                    }

                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun subirImagenStorage(){
        progressDialog.setMessage("Subiendo imagen de Storage")
        progressDialog.show()

        val rutaImagen = "ImagenesPerfil/${firebaseAuth.uid}"
        val storageReference = FirebaseStorage.getInstance().getReference(rutaImagen)
        storageReference.putFile(imageUri!!)
            .addOnSuccessListener {takeSnapshot ->
                val uriTask = takeSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val urlImagenCargada = "${uriTask.result}"
                if(uriTask.isSuccessful) {
                    actualizarImagenBD(urlImagenCargada)
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()}
    }

    private fun actualizarImagenBD(urlImagenCargada: String) {
        progressDialog.setMessage("Actualizando imagen")
        progressDialog.show()

        val hashMap : HashMap<String, Any> = HashMap()
        if(imageUri != null){
            hashMap["urlImagenPerfil"] = urlImagenCargada
        }

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Su imagen de perfil se ha actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()}
    }

    private fun selec_imagen_de(){
        val popupMenu = PopupMenu(this, binding.FABCambiarImg)

        popupMenu.menu.add(Menu.NONE,1,1, "Camara")
        popupMenu.menu.add(Menu.NONE,2,2, "Galeria")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if(itemId == 1 ){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    concederPermisosCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                }else{
                    concederPermisosCamara.launch(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ))
                }
            }else if(itemId == 2){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    imagenGaleria()
                } else{
                    concederPermisosAlmacenamiento.launch(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }

            }
            return@setOnMenuItemClickListener true
        }
    }

    private val concederPermisosCamara =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {
                resultado -> var concedidoTodos = true
            for(seConcede in resultado.values) {
                concedidoTodos = concedidoTodos && seConcede
            }
            if (concedidoTodos){
                imagenCamara()
            }else {
                Toast.makeText(
                    this,
                    "El permiso de la camara o almacenamiento se denegaron",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val concederPermisosAlmacenamiento=
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()) {
                esConcedido ->
            if (esConcedido){
                imagenGaleria()
            }else {
                Toast.makeText(
                    this,
                    "El permiso de almacenamiento se denego",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val resultadoCamara_ARL =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ resultado ->
            if(resultado.resultCode == RESULT_OK){
                subirImagenStorage()
//                try{
//                    Glide.with(this)
//                        .load(imageUri)
//                        .placeholder(R.drawable.img_perfil)
//                        .into(binding.imgPerfil)
//                }catch(e: Exception){
//                }
            }else{
                Toast.makeText(
                    this,
                    "La captura de imagen se canceló",
                    Toast.LENGTH_SHORT
                ).show(
                )
            }
        }

    private val resultadoGaleria_ARL =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ resultado ->
            if(resultado.resultCode == RESULT_OK){
                val data = resultado.data
                imageUri = data!!.data
                subirImagenStorage()

//                try{
//                    Glide.with(this)
//                        .load(imageUri)
//                        .placeholder(R.drawable.img_perfil)
//                        .into(binding.imgPerfil)
//                }catch(e: Exception){
//                }
            }else{
                Toast.makeText(
                    this,
                    "La selecció de imagen se canceló",
                    Toast.LENGTH_SHORT
                ).show(
                )
            }
        }

    private fun imagenCamara() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Titulo_imagen")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion_imagen")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        resultadoCamara_ARL.launch(intent)
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria_ARL.launch(intent)
    }
}
