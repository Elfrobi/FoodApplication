package com.example.foodapplication

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.EmailAuthProvider

class SettingsActivity : AppCompatActivity() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var toolbar: Toolbar
    private lateinit var nameTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.back_icon)
            title = "Beállítások"
        }

        nameTextView = findViewById(R.id.nameTextView)
        val changeNameButton = findViewById<Button>(R.id.changeNameButton)
        val changePasswordButton = findViewById<Button>(R.id.changePasswordButton)

        fillUserName()

        changeNameButton.setOnClickListener {
            showChangeNameDialog()
        }

        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun showChangeNameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_name, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val saveButton = dialogView.findViewById<Button>(R.id.saveNameButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelNameButton)

        saveButton.setOnClickListener {
            val newName = editName.text.toString()
            if (newName.isNotEmpty()) {
                updateUserName(newName)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Kérjük, adjon meg egy nevet!", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val oldPasswordEditText = dialogView.findViewById<EditText>(R.id.editOldPassword)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.editNewPassword)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.editConfirmPassword)
        val saveButton = dialogView.findViewById<Button>(R.id.savePasswordButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelPasswordButton)

        saveButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (oldPassword.isNotEmpty() && newPassword == confirmPassword) {
                updatePassword(oldPassword, newPassword)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Hiba a jelszó megerősítésében!", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun fillUserName(){
        val user = firebaseAuth.currentUser
        if (user != null) {
            val uid = user.uid
            val userRef = FirebaseFirestore.getInstance().collection("Users").document(uid)

            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("username")
                    userName?.let {
                        nameTextView.text = it
                    }
                } else {
                    Toast.makeText(this, "Felhasználói adat nem található!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Hiba történt a felhasználói adatok lekérésekor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserName(newName: String) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val uid = user.uid
            val userRef = FirebaseFirestore.getInstance().collection("Users").document(uid)

            userRef.update("username", newName)
                .addOnSuccessListener {
                    Toast.makeText(this, "Név sikeresen frissítve!", Toast.LENGTH_SHORT).show()
                    fillUserName()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Hiba történt a Firestore adatainak frissítésekor: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePassword(oldPassword: String, newPassword: String) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            user.reauthenticate(EmailAuthProvider.getCredential(user.email!!, oldPassword))
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user.updatePassword(newPassword).addOnCompleteListener { passwordTask ->
                            if (passwordTask.isSuccessful) {
                                Toast.makeText(this, "Jelszó frissítve!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Hiba történt a jelszó frissítésekor!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Hibás régi jelszó!", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
