package com.example.foodapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class NoHouseholdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_household)

        findViewById<Button>(R.id.createHouseholdButton).setOnClickListener {
            val intent = Intent(this, CreateHouseholdActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.joinHouseholdButton).setOnClickListener {
            val intent = Intent(this, JoinHouseholdActivity::class.java)
            startActivity(intent)
        }
    }
}
