package com.cs407.fridgefinder

import Recipe
import RecipeAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class SavedRecipesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saved_recipes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recipeSavedList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList())
        recyclerView.adapter = adapter

        fetchSavedRecipes()

        // Navigate to main activity when back button is clicked
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

    }

    private fun fetchSavedRecipes() {
        firestore.collection("recipe")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val recipes = result.documents.mapNotNull { it.toObject(Recipe::class.java)}
                adapter.updateRecipes(recipes)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching saved recipes", Toast.LENGTH_SHORT).show()
            }
    }
}