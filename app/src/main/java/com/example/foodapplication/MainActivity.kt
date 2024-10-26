package com.example.foodapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    private lateinit var foodListView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val foodList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        foodListView = findViewById(R.id.foodListView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, foodList)
        foodListView.adapter = adapter

        findViewById<View>(R.id.addButton).setOnClickListener {
            showAddFoodItemDialog()
        }

        foodListView.setOnItemClickListener { parent, view, position, id ->
            showEditFoodItemDialog(position)
        }

        foodListView.setOnItemLongClickListener { parent, view, position, id ->
            showDeleteConfirmationDialog(position)
            true
        }
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
                foodList.add(foodName)
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a food name", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showEditFoodItemDialog(position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_food, null)
        val foodNameEditText = dialogView.findViewById<EditText>(R.id.foodNameEditText)
        val closeDialog = dialogView.findViewById<ImageView>(R.id.closeDialog)

        foodNameEditText.setText(foodList[position])

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        closeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val foodName = foodNameEditText.text.toString()
            if (foodName.isNotEmpty()) {
                foodList[position] = foodName
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a food name", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            foodList.removeAt(position)
            adapter.notifyDataSetChanged()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Food Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Yes") { dialog, which ->
                foodList.removeAt(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
