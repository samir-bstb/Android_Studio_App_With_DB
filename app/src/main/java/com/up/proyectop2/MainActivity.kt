package com.up.proyectop2

import android.content.Intent
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productsList: MutableList<Products> = mutableListOf()
    private lateinit var overlayView: View
    private var isDeleteMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        productsList.add(Products("Pizza", 10.99, 5, R.drawable.pizza))

        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        recyclerViewProducts.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(productsList, isDeleteMode)
        recyclerViewProducts.adapter = productAdapter

        // Set initial click listener
        productAdapter.onItemClick = { _ ->
            /*
            if (!isDeleteMode) {
                Toast.makeText(this, "Click disabled outside delete mode", Toast.LENGTH_SHORT).show()
            }*/
        }

        overlayView = View(this).apply {
            setBackgroundColor(Color.argb(100, 255, 255, 255))
            visibility = View.GONE
        }
        addContentView(overlayView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            startActivityForResult(intent, ADD_PRODUCT_REQUEST)
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            handleDeleteClick()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_PRODUCT_REQUEST && resultCode == Activity.RESULT_OK) {
            val name = data?.getStringExtra("name") ?: ""
            val price = data?.getDoubleExtra("price", 0.0) ?: 0.0
            val quantity = data?.getIntExtra("quantity", 0) ?: 0
            val imagenResId = data?.getIntExtra("imagenResId", R.drawable.error) ?: R.drawable.error

            val exists = productsList.any { it.name == name }
            if (exists) {
                Toast.makeText(this, "Product already in stock", Toast.LENGTH_SHORT).show()
            } else {
                productsList.add(Products(name, price, quantity, imagenResId))
                productAdapter.notifyDataSetChanged()
                // Ensure delete mode is off after adding a product
                isDeleteMode = false
                productAdapter.isDeleteMode = false
                overlayView.visibility = View.GONE
                findViewById<Button>(R.id.btnAdd).isEnabled = true
            }
        }
    }

    private fun handleDeleteClick() {
        isDeleteMode = !isDeleteMode
        productAdapter.isDeleteMode = isDeleteMode // Update adapter's delete mode
        overlayView.visibility = if (isDeleteMode) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnAdd).isEnabled = !isDeleteMode

        if (isDeleteMode) {
            productAdapter.onItemClick = { selectedPositions ->
                if (selectedPositions.isNotEmpty()) {
                    showDeleteConfirmation(selectedPositions)
                }
            }
            Toast.makeText(this, "Select products to delete", Toast.LENGTH_SHORT).show()
        } else {
            productAdapter.onItemClick = { _ ->
                Toast.makeText(this, "Click disabled outside delete mode", Toast.LENGTH_SHORT).show()
            }
            productAdapter.selectedPositions.clear()
            productAdapter.notifyDataSetChanged()
        }
    }

    private fun showDeleteConfirmation(selectedPositions: List<Int>) {
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, "Select at least one product to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val productsToDelete = selectedPositions.map { productsList[it] }.joinToString { it.name }
        AlertDialog.Builder(this)
            .setTitle("Warning!")
            .setMessage("Are you sure you want to delete $productsToDelete?")
            .setPositiveButton("Yes") { _, _ ->
                selectedPositions.sortedDescending().forEach { position ->
                    productsList.removeAt(position)
                }
                productAdapter.selectedPositions.clear()
                productAdapter.notifyDataSetChanged()
                isDeleteMode = false
                productAdapter.isDeleteMode = false
                overlayView.visibility = View.GONE
                findViewById<Button>(R.id.btnAdd).isEnabled = true
                Toast.makeText(this, "Products deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { _, _ ->
                productAdapter.selectedPositions.clear()
                productAdapter.notifyDataSetChanged()
                isDeleteMode = false
                productAdapter.isDeleteMode = false
                overlayView.visibility = View.GONE
                findViewById<Button>(R.id.btnAdd).isEnabled = true
            }
            .setOnCancelListener {
                productAdapter.selectedPositions.clear()
                productAdapter.notifyDataSetChanged()
                isDeleteMode = false
                productAdapter.isDeleteMode = false
                overlayView.visibility = View.GONE
                findViewById<Button>(R.id.btnAdd).isEnabled = true
            }
            .show()
    }

    companion object {
        const val ADD_PRODUCT_REQUEST = 1
    }
}