package com.example.foodapplication

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanContract
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference

class AdminActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var productNameEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var unitSpinner: Spinner
    private lateinit var barcodeEditText: EditText
    private lateinit var selectedUnitId: String
    private lateinit var selectedCategoryId : String

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Ha a barcode értéke megtalálható, akkor beírjuk a kódot a vonalkód mezőbe
            barcodeEditText.setText(result.contents)
            Toast.makeText(this, "Scanned: ${result.contents}", Toast.LENGTH_SHORT).show()
        } else {
            // Ha nincs eredmény (nem történt beolvasás), akkor üresen hagyjuk a mezőt
            barcodeEditText.setText("")
            Toast.makeText(this, "Scan was cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        firestore = FirebaseFirestore.getInstance()

        // Inicializáljuk a UI elemeket
        productNameEditText = findViewById(R.id.productNameEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        unitSpinner = findViewById(R.id.unitSpinner)
        barcodeEditText = findViewById(R.id.barcodeEditText)

        // Gomb beállítása, amely elindítja a barcode szkennelést
        val startScanButton: Button = findViewById(R.id.startScanButton)
        startScanButton.setOnClickListener {
            startBarcodeScan()
        }

        // Mentés gomb beállítása, amely elmenti az új terméket
        val saveButton: Button = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            saveProductToDatabase()
        }

        // Betöltjük a kategóriákat és mértékegységeket (Unit és Categories)
        loadCategories()
        loadUnits()
    }

    // Barcode szkenner elindítása
    private fun startBarcodeScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan a barcode")
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(true)

        // Barcode szkenner elindítása
        barcodeLauncher.launch(options)
    }

    // Kategóriák betöltése a Categories gyűjteményből
    // Kategóriák betöltése
    private fun loadCategories() {
        firestore.collection("Categories")
            .get()
            .addOnSuccessListener { result ->
                val categoryNames = mutableListOf<String>()
                val categoryIds = mutableListOf<String>()

                for (document in result) {
                    val categoryName = document.getString("name") ?: ""
                    val categoryId = document.id  // A dokumentum ID-t használjuk itt
                    categoryNames.add(categoryName)
                    categoryIds.add(categoryId)
                }

                val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = categoryAdapter

                // Tedd el a kiválasztott kategória ID-ját, hogy később használhasd
                categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedCategoryId = categoryIds[position] // Az ID-t használjuk

                    }

                    override fun onNothingSelected(parentView: AdapterView<*>) {
                        // Ha semmit nem választanak, akkor itt lekezelheted
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading categories: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Mértékegységek betöltése
    private fun loadUnits() {
        firestore.collection("Units")
            .get()
            .addOnSuccessListener { result ->
                val unitNames = mutableListOf<String>()
                val unitIds = mutableListOf<String>()

                for (document in result) {
                    val unitName = document.getString("name") ?: ""
                    val unitId = document.id  // A dokumentum ID-t használjuk itt
                    unitNames.add(unitName)
                    unitIds.add(unitId)
                }

                val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitNames)
                unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                unitSpinner.adapter = unitAdapter

                // Tedd el a kiválasztott mértékegység ID-ját, hogy később használhasd
                unitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedUnitId = unitIds[position] // Az ID-t használjuk
                    }

                    override fun onNothingSelected(parentView: AdapterView<*>) {
                        // Ha semmit nem választanak, akkor itt lekezelheted
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading units: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Az új termék mentése a Firestore-ban
    private fun saveProductToDatabase() {
        val productName = productNameEditText.text.toString()
        val categoryRef: DocumentReference = firestore.collection("Categories").document(selectedCategoryId)
        val unitRef: DocumentReference = firestore.collection("Units").document(selectedUnitId)
        val barcode = barcodeEditText.text.toString()

        if (productName.isNotEmpty()) {
            // Keresés a Firestore-ban a termék neve alapján
            firestore.collection("Products")
                .whereEqualTo("name", productName)  // Keresés termék név alapján
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        // Ha nem találunk találatot, akkor ellenőrizzük a vonalkódot, ha nem üres
                        if (barcode.isNotEmpty()) {
                            // Keresés a Firestore-ban a vonalkód alapján
                            firestore.collection("Products")
                                .whereEqualTo("barcode", barcode)  // Keresés vonalkód alapján
                                .get()
                                .addOnSuccessListener { barcodeResult ->
                                    if (barcodeResult.isEmpty) {
                                        // Ha nincs találat a vonalkód alapján, hozzáadhatjuk az új terméket
                                        val newProduct = hashMapOf(
                                            "name" to productName,
                                            "barcode" to barcode,
                                            "category_id" to categoryRef,   // Hivatkozás a kategóriára
                                            "default_unit" to unitRef,      // Hivatkozás az alapértelmezett mértékegységre
                                        )

                                        firestore.collection("Products")
                                            .add(newProduct)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Termék sikeresen hozzáadva", Toast.LENGTH_SHORT).show()

                                                // Ürítse ki a mezőket a sikeres hozzáadás után
                                                productNameEditText.text.clear()   // Ürítse a termék név mezőt
                                                barcodeEditText.text.clear()      // Ürítse a vonalkód mezőt
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(this, "Termék hozzáadása nem sikerült: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        // Ha már létezik termék ezzel a vonalkóddal
                                        Toast.makeText(this, "Ilyen vonalkóddal már létezik termék", Toast.LENGTH_SHORT).show()
                                        productNameEditText.text.clear()
                                        barcodeEditText.text.clear()
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(this, "Nem sikerült a vonalkód beolvasása: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // Ha nincs vonalkód, akkor csak a terméket adjuk hozzá
                            val newProduct = hashMapOf(
                                "name" to productName,
                                "category_id" to categoryRef,
                                "default_unit" to unitRef,
                            )

                            firestore.collection("Products")
                                .add(newProduct)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Termék sikeresen hozzáadva", Toast.LENGTH_SHORT).show()

                                    // Ürítse ki a mezőket a sikeres hozzáadás után
                                    productNameEditText.text.clear()   // Ürítse a termék név mezőt
                                    barcodeEditText.text.clear()      // Ürítse a vonalkód mezőt
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(this, "Termék hozzáadása nem sikerült: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Ha már létezik termék ugyanazzal a névvel
                        Toast.makeText(this, "Ilyen névvel már létezik termék", Toast.LENGTH_SHORT).show()
                        productNameEditText.text.clear()
                        barcodeEditText.text.clear()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Termék ellenőrzése nem sikerült: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Add meg a termék nevét", Toast.LENGTH_SHORT).show()
        }



    }

}
