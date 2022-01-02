package com.nema.eduup.viewnote

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.utils.AppConstants
import java.util.*

class RatingsRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<RatingsRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private val ratings = ArrayList<Rating>()
    private val TAG = RatingsRecyclerAdapter::class.qualifiedName

    fun addRatings(newRating: List<Rating>) {
        ratings.addAll(newRating)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_rating, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rating = ratings[position]
        holder.txtRatingUserName.text = rating.userName
        holder.txtRatingComment.text = rating.comment
        holder.rbRateValue.rating = rating.rateValue.toFloat()


    }

    override fun getItemCount() = ratings.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtRatingUserName: TextView = itemView.findViewById(R.id.txtRatingUserName)
        val txtRatingComment: TextView = itemView.findViewById(R.id.txtRatingComment)
        val txtRatingDate: TextView = itemView.findViewById(R.id.txtRatingDate)
        val rbRateValue: RatingBar = itemView.findViewById(R.id.rbUserRatingBar)
        val cardComment: CardView = itemView.findViewById(R.id.cardComment)
    }
}