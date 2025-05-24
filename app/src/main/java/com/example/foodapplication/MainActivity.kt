package com.example.foodapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class MainActivity : AppCompatActivity(){
    private val db = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val storageList = mutableListOf<FoodItem>()
    private val shoppingList = mutableListOf<FoodItem>()
    private lateinit var storageListAdapter: FoodItemAdapter
    private lateinit var shoppingListAdapter: FoodItemAdapter
    private lateinit var unitsList: MutableList<String>
    private val unitsMap = mutableMapOf<String, String>()
    private var currentHouseholdId: String? = null

    private lateinit var foodListView: ListView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var headerLayout: LinearLayout
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSelectedHousehold()
        if (currentHouseholdId != null) {
            loadFoodList(currentHouseholdId!!)
        }

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
        setHeaderUserName()
        emptyTextView = findViewById(R.id.emptyTextView)

        shoppingListAdapter = FoodItemAdapter(this, shoppingList, unitsMap)
        storageListAdapter = FoodItemAdapter(this, storageList, unitsMap)
        foodListView.adapter = shoppingListAdapter

        findViewById<Button>(R.id.addButton).setOnClickListener {
            val intent = Intent(this, ProductSelectionActivity::class.java)
            intent.putExtra("HOUSEHOLD_ID", currentHouseholdId)
            startActivity(intent)
        }

        foodListView.setOnItemClickListener { _, _, position, _ ->
            if (foodListView.adapter == storageListAdapter) {
                showDeleteConfirmationDialog(position)
            } else {
                addFoodItemToStorageList(position)
            }
        }

        foodListView.setOnItemLongClickListener { _, _, position, _ ->
            if (foodListView.adapter == storageListAdapter) {
                showEditItemDialog(position, storageList, storageListAdapter, "Storage")
            } else {
                showEditItemDialog(position, shoppingList, shoppingListAdapter, "Shopping_List")
            }
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
        loadUserHouseholdsToMenu()

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_food_list -> {
                    foodListView.adapter = shoppingListAdapter
                    loadFoodList(currentHouseholdId!!)
                    true
                }
                R.id.nav_storage_list -> {
                    foodListView.adapter = storageListAdapter
                    loadStorageList()
                    true
                }
                else -> false
            }
        }
        updateEmptyView()
    }

    override fun onResume() {
        super.onResume()
        loadUserHouseholdsToMenu()
        setHeaderUserName()
        val menu = navigationView.menu
        val menuItem = menu.findItem(R.id.nav_settings)
        menuItem?.isChecked = false
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
                else -> false
            }.also {
                drawerLayout.closeDrawer(navigationView)
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
        db.collection("Households")
            .document(householdId)
            .collection("Shopping_List")
            .get()
            .addOnSuccessListener { querySnapshot ->
                shoppingList.clear() // Korábbi lista törlése
                // Dokumentumok átalakítása FoodItem objektumokká
                for (document in querySnapshot) {
                    val id = document.id
                    val name = document.getString("name") ?: "Unknown"
                    val quantity = (document.getLong("quantity") ?: 0L).toInt()
                    val unitId = document.getString("unit_id") ?: "Unknown"
                    val comment = document.getString("comment") ?: "Unknown"
                    val expiration = document.getDate("expirationDate") ?: Date()

                    shoppingList.add(FoodItem(id, name, quantity, unitId, comment, expiration))
                }
                shoppingListAdapter.notifyDataSetChanged()
                updateEmptyView()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Hiba történt: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadStorageList() {
        currentHouseholdId?.let { householdId ->
            db.collection("Households")
                .document(householdId)
                .collection("Storage")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    storageList.clear()

                    for (document in querySnapshot) {
                        val id = document.id
                        val name = document.getString("name") ?: "Unknown"
                        val quantity = (document.getLong("quantity") ?: 0L).toInt()
                        val unitId = document.getString("unit_id") ?: "Unknown"
                        val comment = document.getString("comment") ?: "Unknown"
                        val expiration = document.getDate("expirationDate") ?: Date()

                        storageList.add(FoodItem(id, name, quantity, unitId, comment, expiration))
                    }
                    storageListAdapter.notifyDataSetChanged()
                    updateEmptyView()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Hiba történt: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadUnits() {
        db.collection("Units")
            .get()
            .addOnSuccessListener { result ->
                unitsList.clear()
                unitsMap.clear()
                for (document in result) {
                    val unitId = document.id
                    val unitName = document.getString("name") ?: unitId
                    unitsMap[unitId] = unitName
                    unitsList.add(unitId)
                }

                shoppingListAdapter.notifyDataSetChanged()
                storageListAdapter.notifyDataSetChanged()
                updateEmptyView()
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

    private fun setHeaderUserName(){
        val headerView = navigationView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<TextView>(R.id.usernameTextView)

        val user = firebaseAuth.currentUser
        if (user != null) {
            val uid = user.uid
            val userRef = FirebaseFirestore.getInstance().collection("Users").document(uid)

            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("username")
                    userName?.let {
                        usernameTextView.text = it
                    }
                } else {
                    Toast.makeText(this, "Felhasználói adat nem található!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Hiba történt a felhasználói adatok lekérésekor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditItemDialog(
        position: Int,
        itemList: MutableList<FoodItem>,
        adapter: FoodItemAdapter,
        collectionName: String
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_food, null)
        val foodNameEditText = dialogView.findViewById<EditText>(R.id.foodNameEditText)
        val foodCommentEditText = dialogView.findViewById<EditText>(R.id.foodCommentEditText)
        val foodQuantityEditText = dialogView.findViewById<EditText>(R.id.foodQuantityEditText)
        val foodUnitSpinner = dialogView.findViewById<Spinner>(R.id.foodUnitSpinner)
        val closeDialog = dialogView.findViewById<ImageView>(R.id.closeDialog)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val foodItem = itemList[position]

        // Kitöltjük az adatokat
        foodNameEditText.setText(foodItem.name)
        foodCommentEditText.setText(foodItem.comment)
        foodQuantityEditText.setText(foodItem.quantity.toString())

        val unitNames = unitsList.map { unitsMap[it] ?: it }
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitNames)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        foodUnitSpinner.adapter = unitAdapter

        // Spinner kiválasztott elemének beállítása
        val unitName = unitsMap[foodItem.unitId]
        if (unitName != null) {
            val unitPosition = unitNames.indexOf(unitName)
            if (unitPosition != -1) {
                foodUnitSpinner.setSelection(unitPosition)
            }
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
                val updatedFoodItem = mutableMapOf<String, Any>()

                if (foodName != foodItem.name) updatedFoodItem["name"] = foodName
                if (foodComment != foodItem.comment) updatedFoodItem["comment"] = foodComment
                if (foodQuantity.toInt() != foodItem.quantity) updatedFoodItem["quantity"] = foodQuantity.toInt()
                if (foodUnit != foodItem.unitId) updatedFoodItem["unit_id"] =
                    unitsMap.filterValues { it == foodUnit }.keys.first()

                if (updatedFoodItem.isNotEmpty()) {
                    currentHouseholdId?.let { householdId ->
                        updateItemInHousehold(householdId, collectionName, foodItem.id, updatedFoodItem)
                    }
                    foodItem.name = foodName
                    foodItem.comment = foodComment
                    foodItem.quantity = foodQuantity.toInt()
                    foodItem.unitId = unitsMap.filterValues { it == foodUnit }.keys.first()
                    adapter.notifyDataSetChanged()
                    updateEmptyView()
                    Toast.makeText(this, "Termék frissítve", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Nem történt változás", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Tölts ki minden mezőt", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            currentHouseholdId?.let { householdId ->
                deleteItemFromHousehold(householdId, collectionName, foodItem.id)
            }
            itemList.removeAt(position)
            adapter.notifyDataSetChanged()
            updateEmptyView()
            Toast.makeText(this, "Termék törölve", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun updateItemInHousehold(householdId: String, collectionName: String, itemId: String, updatedData: Map<String, Any>) {
        db.collection("Households")
            .document(householdId)
            .collection(collectionName)
            .document(itemId)
            .update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Termék frissítve", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteItemFromHousehold(householdId: String, collectionName: String, itemId: String) {
        db.collection("Households")
            .document(householdId)
            .collection(collectionName)
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Termék törölve", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hiba: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun addFoodItemToStorageList(position: Int){
        val foodItem = shoppingList[position]

        currentHouseholdId?.let { householdId ->
            val storageRef = db.collection("Households")
                .document(householdId)
                .collection("Storage")

            val shoppingRef = db.collection("Households")
                .document(householdId)
                .collection("Shopping_List")
                .document(foodItem.id)

            val newItem = hashMapOf(
                "name" to foodItem.name,
                "quantity" to foodItem.quantity,
                "unit_id" to foodItem.unitId,
                "comment" to foodItem.comment,
                "expiration" to foodItem.expiration,
            )

            storageRef.add(newItem)
                .addOnSuccessListener {
                    shoppingRef.delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "${foodItem.name} áthelyezve a tárolásba", Toast.LENGTH_SHORT).show()
                            loadFoodList(householdId)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Nem sikerült törölni: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Nem sikerült áthelyezni: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val foodItem = storageList[position]
        AlertDialog.Builder(this)
            .setTitle("Termék törlése")
            .setMessage("Biztosan törlöd a(z) '${foodItem.name}' terméket a háztartásból?")
            .setPositiveButton("Törlés") { _, _ ->
                currentHouseholdId?.let { householdId ->
                    deleteItemFromHousehold(householdId, "Storage", foodItem.id)
                }
                storageList.removeAt(position)
                storageListAdapter.notifyDataSetChanged()
                updateEmptyView()
                Toast.makeText(this, "Termék törölve", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun loadUserHouseholdsToMenu() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("User_Households")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                val menu = navigationView.menu
                menu.removeGroup(11)

                val submenu = menu.addSubMenu(11, Menu.NONE, Menu.NONE, "Háztartások")
                val householdIds = mutableListOf<String>()
                var loadedCount = 0

                for (document in documents) {
                    val householdId = document.getString("household_id") ?: continue
                    householdIds.add(householdId)

                    db.collection("Households").document(householdId).get()
                        .addOnSuccessListener { household ->
                            val householdName = household.getString("name") ?: "Névtelen háztartás"
                            val menuItem = submenu.add(Menu.NONE, householdId.hashCode(), Menu.NONE, householdName)
                            menuItem.isCheckable = true

                            menuItem.setOnMenuItemClickListener {
                                saveSelectedHousehold(householdId)
                                currentHouseholdId = householdId
                                loadFoodList(householdId)
                                drawerLayout.closeDrawer(navigationView)
                                true
                            }

                            loadedCount++

                            if (loadedCount == householdIds.size) {
                                val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
                                val savedId = sharedPref.getString("SELECTED_HOUSEHOLD", null)

                                val selectedId = savedId ?: householdIds.firstOrNull()
                                currentHouseholdId = selectedId

                                for (i in 0 until submenu.size()) {
                                    val item = submenu.getItem(i)
                                    item.isChecked = item.itemId == selectedId.hashCode()
                                }
                                selectedId?.let { loadFoodList(it) }
                            }
                        }
                }
                if (documents.isEmpty) {
                    shoppingList.clear()
                    shoppingListAdapter.notifyDataSetChanged()
                    updateEmptyView()
                }
            }
    }


    private fun updateEmptyView() {
        if (foodListView.adapter == shoppingListAdapter) {
            if (shoppingList.isEmpty()) {
                emptyTextView.text = getString(R.string.empty_shopping_list)
                emptyTextView.visibility = View.VISIBLE
            } else {
                emptyTextView.visibility = View.GONE
            }
        } else if (foodListView.adapter == storageListAdapter) {
            if (storageList.isEmpty()) {
                emptyTextView.text = getString(R.string.empty_storage_list)
                emptyTextView.visibility = View.VISIBLE
            } else {
                emptyTextView.visibility = View.GONE
            }
        }
    }


    //kilépéskor maradjon megnyitva az utoljára kiválaszott háztartás
    private fun saveSelectedHousehold(householdId: String) {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit().putString("SELECTED_HOUSEHOLD", householdId).apply()
    }

    private fun loadSelectedHousehold() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        currentHouseholdId = sharedPref.getString("SELECTED_HOUSEHOLD", null)
    }
}
