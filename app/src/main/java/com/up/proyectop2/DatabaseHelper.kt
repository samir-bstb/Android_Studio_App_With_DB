package com.up.proyectop2

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "BD_APP.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_PRODUCTS = "productos"

        // Columnas
        const val COLUMN_ID = "ID"
        const val COLUMN_NAME = "name"
        const val COLUMN_PRICE = "price"
        const val COLUMN_QUANTITY = "quantity"
        const val COLUMN_IMAGE_RES_ID = "imagenResID"
    }

    init {
        copyDatabaseFromAssets()
    }

    private fun copyDatabaseFromAssets() {
        val dbPath = context.getDatabasePath(DATABASE_NAME)

        // Si la base de datos ya existe, no la sobrescribimos
        if (dbPath.exists()) {
            return
        }

        // Crear el directorio si no existe
        dbPath.parentFile?.mkdirs()

        try {
            // Copiar la base de datos desde assets
            context.assets.open(DATABASE_NAME).use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            }
            android.util.Log.d("DatabaseHelper", "✅ Base de datos copiada desde assets exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "❌ Error copiando base de datos: ${e.message}", e)
            throw RuntimeException("Error al copiar la base de datos desde assets", e)
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // No necesitamos crear la tabla porque ya existe en la BD de assets
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Manejar actualizaciones de la base de datos si es necesario
    }

    // Data class interna para manejar productos con ID
    data class ProductWithId(
        val id: Int,
        val name: String,
        val price: Double,
        val quantity: Int,
        val imagenResId: Int
    )

    // Obtener todos los productos (retorna lista de ProductWithId para uso interno)
    fun getAllProductsWithId(): List<ProductWithId> {
        val productsList = mutableListOf<ProductWithId>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS", null)

        try {
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                    val price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE))
                    val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY))
                    val imagenResId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_RES_ID))

                    productsList.add(ProductWithId(id, name, price, quantity, imagenResId))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }

        return productsList
    }

    // Obtener todos los productos (sin ID para la UI)
    fun getAllProducts(): List<Products> {
        return getAllProductsWithId().map {
            Products(it.name, it.price, it.quantity, it.imagenResId)
        }
    }

    // Insertar un producto
    fun insertProduct(product: Products): Long {
        val db = writableDatabase
        db.execSQL(
            "INSERT INTO $TABLE_PRODUCTS ($COLUMN_NAME, $COLUMN_PRICE, $COLUMN_QUANTITY, $COLUMN_IMAGE_RES_ID) VALUES (?, ?, ?, ?)",
            arrayOf(product.name, product.price, product.quantity, product.imagenResId)
        )

        // Obtener el ID del último registro insertado
        val cursor = db.rawQuery("SELECT last_insert_rowid()", null)
        var id = -1L
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0)
        }
        cursor.close()
        return id
    }

    // Verificar si existe un producto por nombre
    fun productExists(name: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PRODUCTS WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        )

        var exists = false
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0
        }
        cursor.close()
        return exists
    }

    // Obtener ID de un producto por nombre
    fun getProductIdByName(name: String): Int? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_ID FROM $TABLE_PRODUCTS WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        )

        var productId: Int? = null
        if (cursor.moveToFirst()) {
            productId = cursor.getInt(0)
        }
        cursor.close()
        return productId
    }

    // Eliminar un producto por nombre
    fun deleteProductByName(name: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_PRODUCTS, "$COLUMN_NAME = ?", arrayOf(name))
    }

    // Eliminar múltiples productos por nombres
    fun deleteProductsByNames(names: List<String>): Int {
        val db = writableDatabase
        var deletedCount = 0

        db.beginTransaction()
        try {
            names.forEach { name ->
                deletedCount += db.delete(TABLE_PRODUCTS, "$COLUMN_NAME = ?", arrayOf(name))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return deletedCount
    }

    // Actualizar cantidad de un producto por nombre
    fun updateProductQuantityByName(name: String, newQuantity: Int): Int {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COLUMN_QUANTITY = ? WHERE $COLUMN_NAME = ?",
            arrayOf(newQuantity, name)
        )
        return 1 // Retorna 1 si se actualizó correctamente
    }

    // Actualizar precio de un producto por nombre
    fun updateProductPriceByName(name: String, newPrice: Double): Int {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COLUMN_PRICE = ? WHERE $COLUMN_NAME = ?",
            arrayOf(newPrice, name)
        )
        return 1 // Retorna 1 si se actualizó correctamente
    }

    // Actualizar precio y cantidad de un producto por nombre
    fun updateProductByName(name: String, newPrice: Double, newQuantity: Int): Int {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COLUMN_PRICE = ?, $COLUMN_QUANTITY = ? WHERE $COLUMN_NAME = ?",
            arrayOf(newPrice, newQuantity, name)
        )
        return 1 // Retorna 1 si se actualizó correctamente
    }

    // Obtener un producto por nombre
    fun getProductByName(name: String): Products? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_PRODUCTS WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        )

        var product: Products? = null
        try {
            if (cursor.moveToFirst()) {
                val productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                val price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE))
                val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY))
                val imagenResId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_RES_ID))

                product = Products(productName, price, quantity, imagenResId)
            }
        } finally {
            cursor.close()
        }

        return product
    }
}
