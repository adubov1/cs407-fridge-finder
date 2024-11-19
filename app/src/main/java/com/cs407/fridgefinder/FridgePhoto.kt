package com.cs407.fridgefinder

data class FridgePhoto (
    val uri: String,
    val timestamp: Long = System.currentTimeMillis()
)
