package com.example.foodapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var RegisterName: EditText
    private lateinit var RegisterEmail: EditText
    private lateinit var RegisterPassword: EditText
    private lateinit var ConfirmPassword: EditText
    private lateinit var RegisterButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        RegisterName = findViewById(R.id.RegisterName)
        RegisterEmail = findViewById(R.id.RegisterEmail)
        RegisterPassword = findViewById(R.id.RegisterPassword)
        ConfirmPassword = findViewById(R.id.ConfirmPassword)
        RegisterButton = findViewById(R.id.RegisterButton)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        RegisterButton.setOnClickListener {
            val name = RegisterName.text.toString()
            val email = RegisterEmail.text.toString()
            val password = RegisterPassword.text.toString()
            val confirmPassword = ConfirmPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Nincs kitöltve minden", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "A jelszók nem egyeznek", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.i("RegisterActivity", "Regisztráció sikeres")
                            uploadUsersDataToDatabase(name);
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Hiba a regisztrációban", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        findViewById<TextView>(R.id.LoginLink).setOnClickListener {
            goToLogin(it)
        }
    }

    private fun goToLogin(view: View) {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun uploadUsersDataToDatabase(registerName: String){
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = database.collection("Users").document(uid)
        ref.set(
            User(uid, registerName, "")
        )
   }
}

class User(val uid: String, val username: String, val household: String)

