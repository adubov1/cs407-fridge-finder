package com.cs407.fridgefinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
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
    private val ingredients = mutableListOf<String>()
    private lateinit var adapter: IngredientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredients)

        recyclerView = findViewById(R.id.ingredientsList)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = IngredientsAdapter(ingredients)
        recyclerView.adapter = adapter

        val photoPaths = intent.getStringArrayListExtra("photoPaths") ?: emptyList()
        analyzeImages(photoPaths)
    }

    private fun analyzeImages(photoPaths: List<String>) {
        if (photoPaths.isEmpty()) {
            Toast.makeText(this, "No photos to analyze", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        val apiKey = "ao6MEaQ7.SWK7afeT6iMUITOP0QWR67TCfA9sxISy"
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
                        println("Response Body: $responseBody")
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



    private fun updateIngredients(newIngredients: List<String>) {
        val oldSize = ingredients.size
        ingredients.clear()
        adapter.notifyItemRangeRemoved(0, oldSize)
        ingredients.addAll(newIngredients)
        adapter.notifyItemRangeInserted(0, newIngredients.size)
        progressBar.visibility = View.GONE
    }

    class IngredientsAdapter(private val ingredients: List<String>) :
        RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = ingredients[position]
        }

        override fun getItemCount() = ingredients.size
    }
}
