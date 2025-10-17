package com.up.proyectop2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddProductActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        val productOptions = arrayOf("Pizza", "Burguer", "Hot Dog")
        val imageMap = mapOf(
            "Pizza" to R.drawable.pizza,
            "Burguer" to R.drawable.burguer,
            "Hot Dog" to R.drawable.hotdog
        )

        val autoCompleteName: AutoCompleteTextView = findViewById(R.id.autoCompleteName)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, productOptions)
        autoCompleteName.setAdapter(adapter)

        val etPrice: EditText = findViewById(R.id.etPrice)
        val etQuantity: EditText = findViewById(R.id.etQuantity)

        findViewById<Button>(R.id.btnSave).setOnClickListener{
            val name = autoCompleteName.text.toString().trim()

            if (name.isEmpty() || !productOptions.contains(name)) {
                autoCompleteName.error = "Product already in stock"
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

            val imagenResId = imageMap[name] ?: R.drawable.error

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