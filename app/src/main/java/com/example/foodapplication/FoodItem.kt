package com.example.foodapplication

import java.util.Date

data class FoodItem(
    var id: String,
    var name: String,
    var quantity: Int,
    var unitId: String,
    var comment: String,
    var expiration: Date
)
