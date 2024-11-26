package com.example.foodapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class FoodItemAdapter(context: Context, private val foodItems: List<FoodItem>) :
    ArrayAdapter<FoodItem>(context, 0, foodItems) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

        val foodItem = foodItems[position]
        val foodIcon = itemView.findViewById<ImageView>(R.id.foodIcon)
        val foodNameTextView = itemView.findViewById<TextView>(R.id.foodNameTextView)
        val foodQuantityTextView = itemView.findViewById<TextView>(R.id.foodQuantityTextView)

        foodIcon.setImageResource(foodItem.iconResId)
        foodNameTextView.text = foodItem.name
        foodQuantityTextView.text = foodItem.quantity

        return itemView
    }
}
