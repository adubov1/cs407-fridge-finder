package com.cs407.fridgefinder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import org.json.JSONException
import org.json.JSONObject

class IdentifyIngredientsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var addIngredientEdit: EditText
    private lateinit var addIngredientButton: Button
    private lateinit var findRecipesButton: Button
    private val ingredients = mutableListOf<String>()
    private lateinit var adapter: IngredientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredients)

        recyclerView = findViewById(R.id.ingredientsList)
        progressBar = findViewById(R.id.progressBar)
        addIngredientEdit = findViewById(R.id.addIngredientEdit)
        addIngredientButton = findViewById(R.id.addIngredientButton)
        findRecipesButton = findViewById(R.id.findRecipesButton)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            navigateToCamera()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = IngredientsAdapter(
            ingredients,
            onDelete = { position -> removeIngredient(position) },
            onEdit = { position, newText -> updateIngredient(position, newText) }
        )
        recyclerView.adapter = adapter

        if (intent.getBooleanExtra("fromRecipeList", false)) {
            val existingIngredients = intent.getStringArrayListExtra("ingredients")
            if (!existingIngredients.isNullOrEmpty()) {
                updateIngredients(existingIngredients)
            }
        } else {
            val photoPaths = intent.getStringArrayListExtra("photoPaths") ?: emptyList()
            if (photoPaths.isNotEmpty()) {
                analyzeImages(photoPaths)
            }
        }

        addIngredientButton.setOnClickListener {
            val newIngredient = addIngredientEdit.text.toString().trim()
            if (newIngredient.isNotEmpty()) {
                addIngredient(newIngredient)
                addIngredientEdit.text.clear()
            }
        }

        findRecipesButton.setOnClickListener {
            if (ingredients.isNotEmpty()) {
                navigateToRecipes()
            } else {
                Toast.makeText(this, "Please add some ingredients first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeImages(photoPaths: List<String>) {
        if (photoPaths.isEmpty()) {
            Toast.makeText(this, "No photos to analyze", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        val apiKey = "Wx4pjijk.QTdhAGw3E6z2HBzVNywMdnkGBno8zeBa"
        val client = OkHttpClient()
        val combinedResults = mutableListOf<String>()

        Thread {
            try {
                for (imagePath in photoPaths) {
                    val file = File(imagePath)
                    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", file.name, requestBody)
                        .build()

                    val request = Request.Builder()
                        .url("https://vision.foodvisor.io/api/1.0/en/analysis/")
                        .addHeader("Authorization", "Api-Key $apiKey")
                        .post(multipartBody)
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val result = parseAnalysisResponse(responseBody)
                        if (result != null) {
                            combinedResults.addAll(result)
                        }
                    } else {
                        println("Failed for image: $imagePath, Code: ${response.code}, Message: ${response.message}")
                    }
                }

                runOnUiThread {
                    if (combinedResults.isNotEmpty()) {
                        updateIngredients(combinedResults)
                    } else {
                        Toast.makeText(this, "No valid results from the images", Toast.LENGTH_SHORT).show()
                    }
                    progressBar.visibility = View.GONE
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }.start()
    }


    private fun parseAnalysisResponse(responseBody: String?): List<String>? {
        if (responseBody.isNullOrEmpty()) {
            println("Response is null or empty")
            return null
        }

        return try {
            val jsonObject = JSONObject(responseBody)
            val analysisResults = mutableListOf<String>()

            if (!jsonObject.has("items")) {
                println("Key 'items' is missing in the response")
                return null
            }

            val itemsArray = jsonObject.getJSONArray("items")
            if (itemsArray.length() == 0) {
                println("No items found in the response.")
                return null
            }

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val foodsArray = item.getJSONArray("food")

                for (j in 0 until foodsArray.length()) {
                    val food = foodsArray.getJSONObject(j)
                    val confidence = food.optDouble("confidence", 0.0)
                    if (confidence > 0.3) {
                        val foodInfo = food.getJSONObject("food_info")
                        val displayName = foodInfo.optString("display_name", "Unknown Item")
                        analysisResults.add(displayName)
                    }
                }
            }

            analysisResults
        } catch (e: JSONException) {
            e.printStackTrace()
            println("Failed to parse JSON: ${e.message}")
            null
        }
    }

    private fun navigateToCamera() {
        val intent = Intent(this, ScanCameraActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRecipes() {
        val intent = Intent(this, RecipeListActivity::class.java)
        intent.putStringArrayListExtra("ingredients", ArrayList(ingredients))
        startActivity(intent)
    }

    private fun addIngredient(ingredient: String) {
        ingredients.add(ingredient)
        adapter.notifyItemInserted(ingredients.size - 1)
    }

    private fun removeIngredient(position: Int) {
        ingredients.removeAt(position)
        adapter.notifyItemRemoved(position)
    }

    private fun updateIngredient(position: Int, newText: String) {
        ingredients[position] = newText
        adapter.notifyItemChanged(position)
    }

    private fun updateIngredients(newIngredients: List<String>) {
        val oldSize = ingredients.size
        ingredients.clear()
        adapter.notifyItemRangeRemoved(0, oldSize)
        ingredients.addAll(newIngredients)
        adapter.notifyItemRangeInserted(0, newIngredients.size)
        progressBar.visibility = View.GONE
    }

    class IngredientsAdapter(
        private val ingredients: List<String>,
        private val onDelete: (Int) -> Unit,
        private val onEdit: (Int, String) -> Unit
    ) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val editText: EditText = view.findViewById(R.id.ingredientEdit)
            val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ingredient, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.editText.setText(ingredients[position])

            holder.editText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newText = holder.editText.text.toString().trim()
                    if (newText.isNotEmpty() && newText != ingredients[position]) {
                        onEdit(position, newText)
                    }
                }
            }

            holder.deleteButton.setOnClickListener {
                onDelete(position)
            }
        }

        override fun getItemCount() = ingredients.size
    }
}
