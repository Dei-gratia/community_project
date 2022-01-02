package com.nema.eduup.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.nema.eduup.R
import com.nema.eduup.utils.GlideLoader
import com.smarteist.autoimageslider.SliderViewAdapter
import java.net.URL


class AutoSliderImageAdapter(private val context: Context) :
    SliderViewAdapter<AutoSliderImageAdapter.VH>() {
    private var mSliderItems = ArrayList<String>()
    fun renewItems(sliderItems: ArrayList<String>) {
        mSliderItems = sliderItems
        notifyDataSetChanged()
    }

    private val sliderMessages = arrayListOf("Welcome to EduUp", "Your most reliable source of study material",
        "Manage all your study resources in one place", "Set a Todo and get a reminder", "Upload and share with others")


    override fun onCreateViewHolder(parent: ViewGroup): VH {
        val inflate: View = LayoutInflater.from(parent.context).inflate(R.layout.auto_slide_image_holder_with_text, null)
        return VH(inflate)
    }

    override fun onBindViewHolder(viewHolder: VH, position: Int) {
        GlideLoader(context).loadImage(URL(mSliderItems[position]), viewHolder.imageView)
        viewHolder.tvSliderText.text = sliderMessages[position % sliderMessages.size]

    }

    override fun getCount(): Int {
        return mSliderItems.size
    }

    inner class VH(itemView: View) : ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.img_slider)
        var tvSliderText: TextView = itemView.findViewById(R.id.tv_slider_text)
    }

}