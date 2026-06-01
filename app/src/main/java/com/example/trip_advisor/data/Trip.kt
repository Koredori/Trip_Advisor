package com.example.trip_advisor.data

data class Trip(
    val id: Int = 0,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val description: String = "",
    val rating: Float = 0f,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
