package com.nema.eduup.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.nema.eduup.R
import java.io.IOException
import java.net.URL
import com.bumptech.glide.request.RequestListener




class GlideLoader(private val context: Context) {

    fun loadImage(imageURI: Uri, imageView: ImageView){
        try {
            Glide
                .with(context)
                .load(Uri.parse(imageURI.toString()))
                .centerCrop()
                .placeholder(R.drawable.ic_image_black_24)
                .into(imageView)
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun loadImage(imageURL: URL, imageView: ImageView){
        try {
            Glide
                .with(context)
                .load(imageURL)
                .centerCrop()
                .placeholder(R.drawable.ic_image_black_24)
                .into(imageView)
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }
}