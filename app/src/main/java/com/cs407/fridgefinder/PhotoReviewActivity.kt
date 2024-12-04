package com.cs407.fridgefinder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class PhotoReviewActivity : AppCompatActivity() {
    private lateinit var adapter: PhotoReviewAdapter
    private val photos = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_review)

        intent.getStringArrayListExtra("photos")?.let {
            photos.addAll(it)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.photosGrid)
        adapter = PhotoReviewAdapter(photos) { position ->
            removePhoto(position)
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.finalizeButton).setOnClickListener {
            if (photos.isNotEmpty()) {
                println("Starting IdentifyIngredientsActivity with photos: $photos")
                val intent = Intent(this, IdentifyIngredientsActivity::class.java)
                intent.putStringArrayListExtra("photoPaths", ArrayList(photos))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No photos to process", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.takeMoreButton).setOnClickListener {
            returnToCamera()
        }
    }

    private fun removePhoto(position: Int) {
        photos.removeAt(position)
        if (photos.isEmpty()) {
            returnToCamera()
        } else {
            adapter.notifyItemRemoved(position)
        }
    }

    private fun returnToCamera() {
        val intent = Intent()
        intent.putStringArrayListExtra("remaining_photos", ArrayList(photos))
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }
}

private class PhotoReviewAdapter(
    private val photos: List<String>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.photoImage)
        val removeButton: ImageButton = view.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.imageView)
            .load(photos[position])
            .centerCrop()
            .into(holder.imageView)

        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount() = photos.size
}
