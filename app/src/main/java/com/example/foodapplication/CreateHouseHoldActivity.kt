package com.example.foodapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateHouseholdActivity : AppCompatActivity() {

    private lateinit var householdNameEditText: EditText
    private lateinit var createHouseholdButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_household)

        householdNameEditText = findViewById(R.id.householdNameEditText)
        createHouseholdButton = findViewById(R.id.createHouseholdButton)

        createHouseholdButton.setOnClickListener {
            createNewHousehold()
        }
    }

    private fun createNewHousehold() {
        val householdName = householdNameEditText.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (householdName.isEmpty()) {
            Toast.makeText(this, "Add meg a háztartás nevét!", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(this, "Felhasználó nincs bejelentkezve!", Toast.LENGTH_SHORT).show()
            return
        }

        val newHouseholdRef = firestore.collection("Households").document()
        val householdId = newHouseholdRef.id

        val householdData = mapOf(
            "household_id" to householdId,
            "name" to householdName,
            "created_by" to userId
        )

        newHouseholdRef.set(householdData)
            .addOnSuccessListener {
                createHouseholdSubCollections(householdId)
                addUserToHousehold(userId, householdId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hiba a háztartás létrehozásában: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addUserToHousehold(userId: String, householdId: String) {
        val userHouseholdData = mapOf(
            "user_id" to userId,
            "household_id" to householdId,
            "role" to "admin",
        )

        firestore.collection("User_Households").add(userHouseholdData)
            .addOnSuccessListener {
                updateUserHouseholds(userId, householdId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Nem sikerült a felhasználót hozzáadni a háztartáshoz: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createHouseholdSubCollections(householdId: String) {
        val householdRef = firestore.collection("Households").document(householdId)

        val emptyData = mapOf("placeholder" to true)

        householdRef.collection("Custom_Products").add(emptyData)
        householdRef.collection("Shopping_List").add(emptyData)
        householdRef.collection("Storage").add(emptyData)
    }


    private fun updateUserHouseholds(userId: String, householdId: String) {
        val userRef = firestore.collection("Users").document(userId)

        userRef.update("joined_households", FieldValue.arrayUnion(householdId))
            .addOnSuccessListener {
                Toast.makeText(this, "Háztartás sikeresen létrehozva!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Nem sikerült frissíteni a felhasználói adatokat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
