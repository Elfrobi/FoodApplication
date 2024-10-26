package com.example.foodapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Button
import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var LoginEmail: EditText
    private lateinit var LoginPassword: EditText
    private lateinit var LoginButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_login)

        LoginEmail = findViewById(R.id.LoginEmail)
        LoginPassword = findViewById(R.id.LoginPassword)
        LoginButton = findViewById(R.id.LoginButton)
        auth = FirebaseAuth.getInstance()

        LoginButton.setOnClickListener {
            val email = LoginEmail.text.toString()
            val password = LoginPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and Password must not be empty", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.i("LoginActivity", "Login successful")
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        findViewById<TextView>(R.id.RegisterLink).setOnClickListener {
            goToRegister(it)

        }

    }

    fun goToRegister(view: View) {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}
