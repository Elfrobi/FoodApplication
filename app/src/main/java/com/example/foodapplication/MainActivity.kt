package com.example.foodapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(){

    private lateinit var foodListView: ListView
    private lateinit var adapter: FoodItemAdapter
    private val foodList = mutableListOf<FoodItem>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var headerLayout: LinearLayout
    private val db = FirebaseFirestore.getInstance()
    private lateinit var unitsList: MutableList<String>
    private val unitsMap = mutableMapOf<String, String>()
    private var currentHouseholdId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkUserHouseholds()

        unitsList = mutableListOf()
        loadUnits()

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        foodListView = findViewById(R.id.foodListView)
        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        setSupportActionBar(toolbar)

        setHeaderPadding()

        adapter = FoodItemAdapter(this, foodList, unitsMap)
        foodListView.adapter = adapter


        findViewById<Button>(R.id.addButton).setOnClickListener {
            val intent = Intent(this, ProductSelectionActivity::class.java)
            intent.putExtra("HOUSEHOLD_ID", currentHouseholdId)
            startActivity(intent)
        }

        foodListView.setOnItemClickListener { _, _, position, _ ->
            showEditFoodItemDialog(position)
        }

        foodListView.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteConfirmationDialog(position)
            true
        }

        // Navbar setup
        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()


        handleNavigationViewClick()



        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_food_list -> {
                    //loadFoodList()
                    true
                }
                R.id.nav_storage_list -> {
                    //loadStorageList()
                    true
                }
                else -> false
            }
        }


    }

    override fun onResume() {
        super.onResume()
        val householdId = "householdtest"
        loadFoodList(householdId)
    }

    private fun checkUserHouseholds() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("User_Households")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { userHousehold ->
                if (userHousehold.isEmpty) {
                    val intent = Intent(this, NoHouseholdActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val householdId = userHousehold.documents[0].getString("household_id")
                    if (householdId != null) {
                        currentHouseholdId = householdId
                        loadFoodList(householdId)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hiba történt a háztartások lekérésekor", Toast.LENGTH_SHORT).show()
            }
    }


    private fun handleNavigationViewClick(){
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.i("MainActivity", "HOME")
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Beállítások kiválasztva", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_logout -> {
                    logoutUser()
                    drawerLayout.closeDrawer(navigationView)
                    true
                }
                R.id.nav_new -> {
                    Toast.makeText(this, "Új háztartás hozzáadása", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, CreateHouseholdActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_join -> {
                    Toast.makeText(this, "Csatlakozás egy háztartáshoz", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, JoinHouseholdActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_houseHold -> {
                    Toast.makeText(this, "Háztartás", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(navigationView) // Bezárja a navigációs fiókot
            }
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

        Toast.makeText(this, "Kijelentkezés sikeres", Toast.LENGTH_SHORT).show()
    }

    private fun loadFoodList(householdId: String) {
        // Hivatkozás a Shopping_List kollekcióra
        db.collection("Households")
            .document(householdId)
            .collection("Shopping_List")
            .get()
            .addOnSuccessListener { querySnapshot ->
                foodList.clear() // Korábbi lista törlése

                // Dokumentumok átalakítása FoodItem objektumokká
                for (document in querySnapshot) {
                    val id = document.id
                    val name = document.getString("name") ?: "Unknown"
                    val quantity = (document.getLong("quantity") ?: 0L).toInt()
                    val unitId = document.getString("unit_id") ?: "Unknown"
                    val comment = document.getString("comment") ?: "Unknown"

                    foodList.add(FoodItem(id, name, quantity, unitId, comment))
                }
                adapter.notifyDataSetChanged() // Adapter frissítése
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Hiba történt: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadStorageList() {
        // Implement storage list loading from Firestore if needed
    }


    private fun loadUnits() {
        db.collection("Units")
            .get()
            .addOnSuccessListener { result ->
                unitsMap.clear()
                for (document in result) {
                    val unitId = document.id
                    val unitName = document.getString("name") ?: ""
                    unitsMap[unitId] = unitName
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading units", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setHeaderPadding(){
        val headerView = navigationView.getHeaderView(0)
        headerLayout = headerView.findViewById(R.id.header_layout)

        val statusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android").let {
            if (it > 0) resources.getDimensionPixelSize(it) else 0
        }

        headerLayout.setPadding(
            headerLayout.paddingLeft,
            statusBarHeight,
            headerLayout.paddingRight,
            headerLayout.paddingBottom
        )
    }

    private fun showEditFoodItemDialog(position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_food, null)
        val foodNameEditText = dialogView.findViewById<EditText>(R.id.foodNameEditText)
        val foodCommentEditText = dialogView.findViewById<EditText>(R.id.foodCommentEditText)
        val foodQuantityEditText = dialogView.findViewById<EditText>(R.id.foodQuantityEditText)
        val foodUnitSpinner = dialogView.findViewById<Spinner>(R.id.foodUnitSpinner)
        val closeDialog = dialogView.findViewById<ImageView>(R.id.closeDialog)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val foodItem = foodList[position]  // A foodList az adott háztartás Shopping_List-jét tartalmazza

        // Kitöltjük az adatokat a foodItem objektumból
        foodNameEditText.setText(foodItem.name)
        foodCommentEditText.setText(foodItem.comment)
        foodQuantityEditText.setText(foodItem.quantity.toString())

        // Feltételezve, hogy az egységek listája elérhető
        val unitAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, unitsList)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        foodUnitSpinner.adapter = unitAdapter

        // Az egység kiválasztása a foodItem alapján
        val unitPosition = unitsList.indexOf(foodItem.unitId) // Az egység alapján beállítjuk a spinner pozícióját
        if (unitPosition != -1) {
            foodUnitSpinner.setSelection(unitPosition)
        }

        closeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val foodName = foodNameEditText.text.toString()
            val foodComment = foodCommentEditText.text.toString()
            val foodQuantity = foodQuantityEditText.text.toString()
            val foodUnit = foodUnitSpinner.selectedItem.toString()

            if (foodName.isNotEmpty() && foodQuantity.isNotEmpty()) {
                // Csak akkor frissítjük az adatokat, ha történt változás
                val updatedFoodItem = mutableMapOf<String, Any>()

                if (foodName != foodItem.name) updatedFoodItem["name"] = foodName
                if (foodComment != foodItem.comment) updatedFoodItem["comment"] = foodComment
                if (foodQuantity.toInt() != foodItem.quantity) updatedFoodItem["quantity"] = foodQuantity.toInt()
                if (foodUnit != foodItem.unitId) updatedFoodItem["unit_id"] = foodUnit

                // Ha történt változás, frissítjük az adatbázisban
                if (updatedFoodItem.isNotEmpty()) {
                    currentHouseholdId?.let { householdId -> updateFoodItemInHousehold(householdId, foodItem.id, updatedFoodItem) }
                    foodItem.name = foodName
                    foodItem.comment = foodComment
                    foodItem.quantity = foodQuantity.toInt()
                    foodItem.unitId = foodUnit
                    adapter.notifyDataSetChanged()  // Adapter frissítése
                    Toast.makeText(this, "Food item updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            // Töröljük a terméket az adott háztartás Shopping_List kollekciójából
            currentHouseholdId?.let { householdId -> deleteFoodItemFromHousehold(householdId, foodItem.id) }
            foodList.removeAt(position)
            adapter.notifyDataSetChanged()  // Adapter frissítése
            Toast.makeText(this, "Food item deleted", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }



    private fun updateFoodItemInHousehold(householdId: String, foodItemId: String, updatedData: Map<String, Any>) {
        val foodRef = db.collection("Households").document(householdId)
            .collection("Shopping_List").document(foodItemId)
        foodRef.update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Food item updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update food item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteFoodItemFromHousehold(householdId: String, foodItemId: String) {
        val foodRef = db.collection("Households").document(householdId)
            .collection("Shopping_List").document(foodItemId)
        foodRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Food item deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete food item", Toast.LENGTH_SHORT).show()
            }
    }



    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Food Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Yes") { _, _ ->
                foodList.removeAt(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
