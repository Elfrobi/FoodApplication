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
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var RegisterName: EditText
    private lateinit var RegisterEmail: EditText
    private lateinit var RegisterPassword: EditText
    private lateinit var ConfirmPassword: EditText
    private lateinit var RegisterButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        RegisterName = findViewById(R.id.RegisterName)
        RegisterEmail = findViewById(R.id.RegisterEmail)
        RegisterPassword = findViewById(R.id.RegisterPassword)
        ConfirmPassword = findViewById(R.id.ConfirmPassword)
        RegisterButton = findViewById(R.id.RegisterButton)

        RegisterButton.setOnClickListener {
            val name = RegisterName.text.toString()
            val email = RegisterEmail.text.toString()
            val password = RegisterPassword.text.toString()
            val confirmPassword = ConfirmPassword.text.toString()

            auth = FirebaseAuth.getInstance()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "A jelszók nem egyeznek", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.i("RegisterActivity", "Registration successful")
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        findViewById<TextView>(R.id.LoginLink).setOnClickListener {
            goToLogin(it)

        }
    }

    fun goToLogin(view: View) {
        startActivity(Intent(this, LoginActivity::class.java))
    }
}

