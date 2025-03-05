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

class JoinHouseholdActivity : AppCompatActivity() {

    private lateinit var householdCodeEditText: EditText
    private lateinit var joinHouseholdButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_household)

        householdCodeEditText = findViewById(R.id.householdCodeEditText)
        joinHouseholdButton = findViewById(R.id.joinHouseholdButton)

        joinHouseholdButton.setOnClickListener {
            joinHousehold()
        }
    }

    private fun joinHousehold() {
        val householdId = householdCodeEditText.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (householdId.isEmpty()) {
            Toast.makeText(this, "Írd be a háztartás azonosítóját!", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(this, "Felhasználó nincs bejelentkezve!", Toast.LENGTH_SHORT).show()
            return
        }

        val householdRef = firestore.collection("Households").document(householdId)
        householdRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userHouseholdData = mapOf(
                        "user_id" to userId,
                        "household_id" to householdId,
                        "role" to "member",
                    )

                    firestore.collection("User_Households").add(userHouseholdData)
                        .addOnSuccessListener {
                            updateUserHouseholds(userId, householdId)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Nem sikerült csatlakozni: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Háztartás nem található!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hiba történt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserHouseholds(userId: String, householdId: String) {
        val userRef = firestore.collection("Users").document(userId)

        userRef.update("joined_households", FieldValue.arrayUnion(householdId))
            .addOnSuccessListener {
                Toast.makeText(this, "Sikeresen csatlakoztál a háztartáshoz!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Nem sikerült frissíteni a felhasználói adatokat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
