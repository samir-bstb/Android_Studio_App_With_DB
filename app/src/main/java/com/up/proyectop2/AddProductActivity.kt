package com.up.proyectop2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class AddProductActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // Categorías base (palabras clave)
        val categories = arrayOf("Pizza", "Burger", "Hot Dog")

        // Ejemplos de productos (opcionales, aparecen como sugerencias)
        val productExamples = arrayOf(
            "Pizza Hawaiana",
            "Pizza Pepperoni",
            "Pizza Vegetariana",
            "Pizza 4 Quesos",
            "Burger Sencilla",
            "Burger Doble",
            "Burger BBQ",
            "Hot Dog Clásico",
            "Hot Dog con Queso",
            "Hot Dog Especial"
        )

        val autoCompleteName: AutoCompleteTextView = findViewById(R.id.autoCompleteName)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, productExamples)
        autoCompleteName.setAdapter(adapter)
        autoCompleteName.threshold = 1 // Mostrar sugerencias después de 1 carácter

        val etPrice: EditText = findViewById(R.id.etPrice)
        val etQuantity: EditText = findViewById(R.id.etQuantity)

        findViewById<Button>(R.id.btnSave).setOnClickListener{
            val name = autoCompleteName.text.toString().trim()

            // Validar que el nombre no esté vacío y contenga una categoría válida
            if (name.isEmpty()) {
                autoCompleteName.error = "Enter a product name"
                return@setOnClickListener
            }

            // Verificar que empiece con una categoría válida
            val hasValidCategory = categories.any { category ->
                name.startsWith(category, ignoreCase = true)
            }

            if (!hasValidCategory) {
                autoCompleteName.error = "Product must start with: Pizza, Burger, or Hot Dog"
                return@setOnClickListener
            }

            val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
            if (price <= 0) {
                etPrice.error = "Invalid price"
                return@setOnClickListener
            }

            val quantity = etQuantity.text.toString().toIntOrNull() ?: 0
            if (quantity <= 0) {
                etQuantity.error = "Invalid quantity"
                return@setOnClickListener
            }

            // Determinar la imagen según la categoría
            val imagenResId = when {
                name.startsWith("Pizza", ignoreCase = true) -> R.drawable.pizza
                name.startsWith("Burger", ignoreCase = true) -> R.drawable.burguer
                name.startsWith("Hot Dog", ignoreCase = true) -> R.drawable.hotdog
                else -> R.drawable.error
            }

            // Enviar datos de vuelta a MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("name", name)
            resultIntent.putExtra("price", price)
            resultIntent.putExtra("quantity", quantity)
            resultIntent.putExtra("imagenResId", imagenResId)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
