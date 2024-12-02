package com.example.foodapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class ProductSelectionActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var productListView: ListView
    private lateinit var unitSpinner: Spinner
    private lateinit var quantityEditText: EditText
    private lateinit var commentEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var scanBarcodeButton: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var householdId: String

    private var productList = mutableListOf<Map<String, String>>() // Store product names and IDs
    private var filteredProductList: MutableList<Map<String, String>> = mutableListOf()
    private var selectedProductId: String? = null
    private lateinit var selectedUnitId: String

    private var unitsList: MutableList<String> = mutableListOf()

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            searchByBarcode(result.contents)
        } else {
            Toast.makeText(this, "Scan was cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_selection)

        firestore = FirebaseFirestore.getInstance()

        householdId = intent.getStringExtra("HOUSEHOLD_ID") ?: ""

        searchEditText = findViewById(R.id.searchEditText)
        productListView = findViewById(R.id.productListView)
        unitSpinner = findViewById(R.id.unitSpinner)
        quantityEditText = findViewById(R.id.quantityEditText)
        commentEditText = findViewById(R.id.commentEditText)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
        scanBarcodeButton = findViewById(R.id.scanBarcodeButton)

        loadProducts()
        loadUnits()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        productListView.setOnItemClickListener { _, _, position, _ ->
            val selectedProduct = filteredProductList[position]
            selectedProductId = selectedProduct["id"]
            searchEditText.setText(selectedProduct["name"])
            searchEditText.clearFocus()
            if (selectedProductId != null) {
                loadProductDetails(selectedProductId!!)
            }
        }

        scanBarcodeButton.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            options.setPrompt("Scan a barcode")
            options.setBeepEnabled(true)
            options.setBarcodeImageEnabled(true)
            options.setOrientationLocked(true)
            barcodeLauncher.launch(options)
        }

        saveButton.setOnClickListener {
            saveProductToShoppingList()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun loadProducts() {
        firestore.collection("Products")
            .get()
            .addOnSuccessListener { result ->
                productList.clear()
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    val id = document.id
                    productList.add(mapOf("name" to name, "id" to id))
                }
                updateProductListView(productList)
                filterProducts("")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading products", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterProducts(query: String) {
        filteredProductList = if (query.isEmpty()) {
            productList
        } else {
            productList.filter { it["name"]?.contains(query, ignoreCase = true) == true }.toMutableList()
        }

        val filteredNames = filteredProductList.map { it["name"] ?: "" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredNames)
        productListView.adapter = adapter
    }


    private fun updateProductListView(list: List<Map<String, String>>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            list.map { it["name"]!! }
        )
        productListView.adapter = adapter
    }

    private fun searchByBarcode(barcode: String) {
        firestore.collection("Products")
            .whereEqualTo("barcode", barcode)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(this, "No product found with this barcode", Toast.LENGTH_SHORT).show()
                } else {
                    val document = result.documents[0]
                    selectedProductId = document.id
                    searchEditText.setText(document.getString("name"))
                    loadProductDetails(selectedProductId!!)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error scanning barcode", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUnits() {
        firestore.collection("Units")
            .get()
            .addOnSuccessListener { result ->
                val unitNames = mutableListOf<String>()
                for (document in result) {
                    val unitName = document.getString("name") ?: ""
                    unitNames.add(unitName)
                    unitsList.add(document.id)
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                unitSpinner.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading units", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProductDetails(productId: String) {
        firestore.collection("Products")
            .document(productId)
            .get()
            .addOnSuccessListener { productDocument ->
                if (productDocument.exists()) {
                    val defaultUnitRef = productDocument.get("default_unit") as? DocumentReference
                    if (defaultUnitRef != null) {
                        defaultUnitRef.get()
                            .addOnSuccessListener { unitSnapshot ->
                                if (unitSnapshot.exists()) {
                                    selectedUnitId = unitSnapshot.id
                                    val unitPosition = unitsList.indexOfFirst { it == selectedUnitId }
                                    if (unitPosition != -1) {
                                        unitSpinner.setSelection(unitPosition)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error loading unit details", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching product details: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun saveProductToShoppingList() {
        val quantity = quantityEditText.text.toString()
        if (selectedProductId != null && quantity.isNotEmpty()) {
            val productData = hashMapOf(
                "product_id" to selectedProductId,
                "name" to searchEditText.text.toString(),
                "quantity" to quantity.toInt(),
                "unit_id" to selectedUnitId,
                "comment" to commentEditText.text.toString()
            )

            firestore.collection("Households")
                .document(householdId)
                .collection("Shopping_List")
                .add(productData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Product added to shopping list", Toast.LENGTH_SHORT).show()
                    searchEditText.text.clear()
                    unitSpinner.setSelection(8)
                    quantityEditText.text.clear()
                    commentEditText.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error adding product", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Please select a product and quantity", Toast.LENGTH_SHORT).show()
        }
    }


}
