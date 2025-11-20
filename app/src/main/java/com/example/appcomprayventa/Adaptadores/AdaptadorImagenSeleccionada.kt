package com.example.appcomprayventa.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appcomprayventa.Modelo.ModeloImagenSeleccionada
import com.example.appcomprayventa.R
import com.example.appcomprayventa.databinding.ItemImagenesSeleccionadasBinding


class AdaptadorImagenSeleccionada
    (private val context: Context,
     private val imagenSelecArrayList : ArrayList<ModeloImagenSeleccionada>
): RecyclerView.Adapter<AdaptadorImagenSeleccionada.HolderImagenSeleccionada>() {
    private lateinit var binding: ItemImagenesSeleccionadasBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImagenSeleccionada {
        binding =
            ItemImagenesSeleccionadasBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderImagenSeleccionada(binding.root)
    }



    override fun onBindViewHolder(holder: HolderImagenSeleccionada, position: Int) {
        val modelo = imagenSelecArrayList[position]
        val imagenUri = modelo.imagenUri

        try {
            Glide.with(context)
                .load(imagenUri)
                .placeholder(R.drawable.item_imagen)
                .into(holder.item_imagen)
        }catch (e: Exception){
        }

        holder.btn_cerrar.setOnClickListener{
            imagenSelecArrayList.remove(modelo)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return imagenSelecArrayList.size
    }

    inner class HolderImagenSeleccionada(itemView: View) : RecyclerView.ViewHolder(itemView){
        var item_imagen = binding.itemImagen
        var btn_cerrar = binding.cerrarItem
    }
}
