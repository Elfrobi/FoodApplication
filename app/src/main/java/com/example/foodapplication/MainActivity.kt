package com.example.foodapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var foodListView: ListView
    private lateinit var adapter: FoodItemAdapter
    private val foodList = mutableListOf<FoodItem>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var headerLayout: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        foodListView = findViewById(R.id.foodListView)
        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        setSupportActionBar(toolbar)

        setHeaderPadding()

        adapter = FoodItemAdapter(this, foodList)
        foodListView.adapter = adapter

        // Load initial data from Firestore
        //loadFoodList()

        findViewById<Button>(R.id.addButton).setOnClickListener {
            val intent = Intent(this, ProductSelectionActivity::class.java)
            //TODO: lekérni a jelenlegi household ID-t
            intent.putExtra("HOUSEHOLD_ID", "householdtest")
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
                    true
                }
                R.id.nav_join -> {
                    Toast.makeText(this, "Csatlakozás egy háztartáshoz", Toast.LENGTH_SHORT).show()
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

    private fun loadFoodList() {
//        db.collection("foodItems")
//            .get()
//            .addOnSuccessListener { result ->
//                foodList.clear()
//                for (document in result) {
//                    val foodName = document.getString("name")
//                    foodName?.let { foodList.add(it) }
//                }
//                adapter.notifyDataSetChanged()
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show()
//            }

        // Notify adapter of data change
        adapter.notifyDataSetChanged()
    }

    private fun loadStorageList() {
        // Implement storage list loading from Firestore if needed
    }

    private fun showAddFoodItemDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null)
        val foodNameEditText = dialogView.findViewById<EditText>(R.id.foodNameEditText)
        val closeDialog = dialogView.findViewById<ImageView>(R.id.closeDialog)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        closeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val foodName = foodNameEditText.text.toString()
            if (foodName.isNotEmpty()) {
                val newFood = hashMapOf("name" to foodName)
//                db.collection("foodItems")
//                    .add(newFood)
//                    .addOnSuccessListener {
//                        foodList.add(foodName)
//                        adapter.notifyDataSetChanged()
//                        dialog.dismiss()
//                    }
//                    .addOnFailureListener {
//                        Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show()
//                    }
            } else {
                Toast.makeText(this, "Please enter a food name", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
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
        val closeDialog = dialogView.findViewById<ImageView>(R.id.closeDialog)

        //foodNameEditText.setText(foodList[position])

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        closeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val foodName = foodNameEditText.text.toString()
            if (foodName.isNotEmpty()) {
                //foodList[position] = foodName
                    adapter.notifyDataSetChanged()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a food name", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
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
