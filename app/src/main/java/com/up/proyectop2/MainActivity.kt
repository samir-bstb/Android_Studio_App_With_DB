package com.up.proyectop2

import android.content.Intent
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productsList: MutableList<Products> = mutableListOf()
    private lateinit var overlayView: View
    private var isDeleteMode = false
    private lateinit var databaseHelper: DatabaseHelper

    companion object {
        const val ADD_PRODUCT_REQUEST = 1
        private const val TAG = "MainActivity_DB"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar helper de base de datos
        databaseHelper = DatabaseHelper(this)

        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        recyclerViewProducts.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(productsList, isDeleteMode)
        recyclerViewProducts.adapter = productAdapter

        // Cargar productos desde la base de datos
        loadProductsFromDatabase()

        // Click normal en producto - mostrar opciones de edición
        productAdapter.onItemLongClick = { product ->
            if (!isDeleteMode) {
                showEditOptions(product)
            }
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

    private fun showEditOptions(product: Products) {
        val options = arrayOf("Edit Product", "Delete Product")
        
        AlertDialog.Builder(this)
            .setTitle(product.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditProductDialog(product)
                    1 -> showDeleteConfirmation(listOf(product.name))
                }
            }
            .show()
    }

    private fun showEditProductDialog(product: Products) {
        // Crear vista personalizada con dos campos
        val dialogView = layoutInflater.inflate(android.R.layout.select_dialog_item, null)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputPrice = EditText(this).apply {
            hint = "Price (current: ${"%.2f".format(product.price)})"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(product.price.toString())
        }

        val inputQuantity = EditText(this).apply {
            hint = "Quantity (current: ${product.quantity})"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(product.quantity.toString())
            setPadding(0, 30, 0, 0)
        }

        container.addView(inputPrice)
        container.addView(inputQuantity)

        AlertDialog.Builder(this)
            .setTitle("Edit Product - ${product.name}")
            .setMessage("Current: ${"$%.2f".format(product.price)} | Stock: ${product.quantity}")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newPrice = inputPrice.text.toString().toDoubleOrNull() ?: -1.0
                val newQuantity = inputQuantity.text.toString().toIntOrNull() ?: -1
                
                if (newPrice <= 0) {
                    Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newQuantity < 0) {
                    Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newQuantity == 0) {
                    // Preguntar si quiere eliminar el producto
                    AlertDialog.Builder(this)
                        .setTitle("Delete Product")
                        .setMessage("Quantity is 0. Do you want to delete '${product.name}'?")
                        .setPositiveButton("Delete") { _, _ ->
                            deleteProductFromDB(product.name)
                        }
                        .setNegativeButton("Keep with 0") { _, _ ->
                            updateProduct(product.name, newPrice, 0)
                        }
                        .show()
                } else {
                    updateProduct(product.name, newPrice, newQuantity)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProduct(productName: String, newPrice: Double, newQuantity: Int) {
        try {
            databaseHelper.updateProductByName(productName, newPrice, newQuantity)
            Log.d(TAG, "✅ Producto actualizado: $productName -> Price: $newPrice, Qty: $newQuantity")
            loadProductsFromDatabase()
            Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating product: ${e.message}", e)
            Toast.makeText(this, "Error updating product", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteProductFromDB(productName: String) {
        try {
            databaseHelper.deleteProductByName(productName)
            Log.d(TAG, "🗑️ Producto eliminado: $productName")
            loadProductsFromDatabase()
            Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting product: ${e.message}", e)
            Toast.makeText(this, "Error deleting product", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProductsFromDatabase() {
        try {
            val products = databaseHelper.getAllProducts()
            productsList.clear()
            productsList.addAll(products)
            productAdapter.notifyDataSetChanged()
            
            // LOG: Mostrar todos los productos en la base de datos
            val productsWithId = databaseHelper.getAllProductsWithId()
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "PRODUCTOS EN BASE DE DATOS: ${productsWithId.size}")
            productsWithId.forEach { product ->
                Log.d(TAG, "ID: ${product.id} | Name: ${product.name} | Price: $${product.price} | Qty: ${product.quantity}")
            }
            Log.d(TAG, "═══════════════════════════════════════")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading products: ${e.message}", e)
            Toast.makeText(this@MainActivity, "Error loading products", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_PRODUCT_REQUEST && resultCode == Activity.RESULT_OK) {
            val name = data?.getStringExtra("name") ?: ""
            val price = data?.getDoubleExtra("price", 0.0) ?: 0.0
            val quantity = data?.getIntExtra("quantity", 0) ?: 0
            val imagenResId = data?.getIntExtra("imagenResId", R.drawable.error) ?: R.drawable.error

            // Verificar si el producto ya existe en la base de datos
            try {
                if (databaseHelper.productExists(name)) {
                    Log.w(TAG, "⚠️ Producto duplicado: $name ya existe")
                    Toast.makeText(this@MainActivity, "Product already in stock", Toast.LENGTH_SHORT).show()
                } else {
                    // Insertar en la base de datos
                    val newProduct = Products(name, price, quantity, imagenResId)
                    val newId = databaseHelper.insertProduct(newProduct)
                    
                    Log.d(TAG, "✅ PRODUCTO AGREGADO:")
                    Log.d(TAG, "   ID generado: $newId | Name: $name | Price: $$price | Qty: $quantity")
                    
                    // Recargar productos
                    loadProductsFromDatabase()
                    
                    // Ensure delete mode is off after adding a product
                    isDeleteMode = false
                    productAdapter.isDeleteMode = false
                    overlayView.visibility = View.GONE
                    findViewById<Button>(R.id.btnAdd).isEnabled = true
                    
                    Toast.makeText(this@MainActivity, "Product added successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error adding product: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error adding product", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDeleteClick() {
        isDeleteMode = !isDeleteMode
        productAdapter.isDeleteMode = isDeleteMode
        overlayView.visibility = if (isDeleteMode) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnAdd).isEnabled = !isDeleteMode

        if (isDeleteMode) {
            productAdapter.onItemClick = { selectedPositions ->
                if (selectedPositions.isNotEmpty()) {
                    val productNames = selectedPositions.map { productsList[it].name }
                    showDeleteConfirmation(productNames)
                }
            }
            Toast.makeText(this, "Select products to delete", Toast.LENGTH_SHORT).show()
        } else {
            productAdapter.onItemClick = { _ -> }
            productAdapter.selectedPositions.clear()
            productAdapter.notifyDataSetChanged()
        }
    }

    private fun showDeleteConfirmation(productNames: List<String>) {
        if (productNames.isEmpty()) {
            Toast.makeText(this, "Select at least one product to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val namesDisplay = productNames.joinToString(", ")
        
        AlertDialog.Builder(this)
            .setTitle("Warning!")
            .setMessage("Are you sure you want to delete $namesDisplay?")
            .setPositiveButton("Yes") { _, _ ->
                try {
                    Log.d(TAG, "🗑️ ELIMINANDO ${productNames.size} PRODUCTOS:")
                    productNames.forEach { name ->
                        Log.d(TAG, "   Deleting: $name")
                    }
                    
                    val deletedCount = databaseHelper.deleteProductsByNames(productNames)
                    Log.d(TAG, "✅ $deletedCount productos eliminados exitosamente")
                    
                    loadProductsFromDatabase()
                    
                    productAdapter.selectedPositions.clear()
                    isDeleteMode = false
                    productAdapter.isDeleteMode = false
                    overlayView.visibility = View.GONE
                    findViewById<Button>(R.id.btnAdd).isEnabled = true
                    Toast.makeText(this@MainActivity, "Products deleted successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error deleting products: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error deleting products", Toast.LENGTH_SHORT).show()
                }
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
}
