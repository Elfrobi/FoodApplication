package com.example.foodapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class FoodItemAdapter(
    private val context: Context,
    private val foodItems: List<FoodItem>,
    private val unitsMap: Map<String, String>
) : ArrayAdapter<FoodItem>(context, R.layout.product_item_row, foodItems) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.product_item_row, parent, false)

        val productNameTextView = view.findViewById<TextView>(R.id.productNameTextView)
        val quantityTextView = view.findViewById<TextView>(R.id.quantityTextView)
        val commentTextView = view.findViewById<TextView>(R.id.commentTextView)

        val foodItem = foodItems[position]
        productNameTextView.text = foodItem.name
        val unitName = unitsMap[foodItem.unitId] ?: "Unknown"
        quantityTextView.text = "Mennyiség: ${foodItem.quantity} $unitName"
        commentTextView.text = "Megjegyzés: ${foodItem.comment}"

        return view
    }
}


