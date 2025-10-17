package com.up.proyectop2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private val products: MutableList<Products>,
    var isDeleteMode: Boolean = false
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    var onItemClick: ((List<Int>) -> Unit)? = null
    var selectedPositions: MutableList<Int> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.tvName.text = product.name
        holder.tvPrice.text = "$${"%.2f".format(product.price)}"
        holder.tvQuantity.text = "In Stock: ${product.quantity}"
        holder.imgProduct.setImageResource(product.imagenResId)

        // Forzar recorte de la imagen según el fondo redondeado
        holder.imgProduct.clipToOutline = true

        // Highlight selected item only in delete mode
        val isSelected = selectedPositions.contains(position) && isDeleteMode
        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) android.R.color.holo_blue_light else android.R.color.transparent
            )
        )

        // Handle click on card
        holder.itemView.setOnClickListener {
            if (isDeleteMode) {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position)
                } else {
                    selectedPositions.add(position)
                }
                notifyDataSetChanged()
                onItemClick?.invoke(selectedPositions)
            } else {
                onItemClick?.invoke(emptyList())
            }
        }
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val cardView: CardView = itemView.findViewById(R.id.cardView)
    }
}