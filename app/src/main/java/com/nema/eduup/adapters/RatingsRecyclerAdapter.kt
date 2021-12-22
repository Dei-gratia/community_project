package com.nema.eduup.adapters

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
import com.nema.eduup.activities.ViewNoteActivity
import com.nema.eduup.models.Rating
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

    fun clearComments(reminders: Boolean) {
        val newRatings = ArrayList<Rating>()

        ratings.clear()
        ratings.addAll(newRatings)
    }

    fun addRating(newRating: Rating) {
        ratings.add(newRating)
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
        //val dateFormat = SimpleDateFormat.getDateInstance().format(rating.date)
        //holder.txtRatingDate.text = dateFormat

        holder.cardComment.setOnClickListener {
            val intent = Intent(context, ViewNoteActivity::class.java)
            intent.putExtra(AppConstants.NOTES, rating)
            //context.startActivity(intent)
        }

    }

    override fun getItemCount() = ratings.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtRatingUserName = itemView.findViewById<TextView>(R.id.txtRatingUserName)
        val txtRatingComment = itemView.findViewById<TextView>(R.id.txtRatingComment)
        val txtRatingDate = itemView.findViewById<TextView>(R.id.txtRatingDate)
        val rbRateValue = itemView.findViewById<RatingBar>(R.id.rbUserRatingBar)
        val cardComment = itemView.findViewById<CardView>(R.id.cardComment)
    }
}