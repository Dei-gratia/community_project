package com.nema.eduup.discussions.people

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.discussions.chat.ChatActivity
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.GlideLoader
import java.net.URL
import java.util.*

class PeopleRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<PeopleRecyclerAdapter.ViewHolder>() {
    private val TAG = PeopleRecyclerAdapter::class.qualifiedName
    private val layoutInflater = LayoutInflater.from(context)
    private val people = ArrayList<User>()
    private var currentUserId = "-1"

    fun addPeople(newPeople: List<User>) {
        people.addAll(newPeople)
        notifyDataSetChanged()
    }

    fun getUserId(userId: String) {
        currentUserId = userId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_person, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val person = people[position]
        holder.txtName.text = "${person.firstNames} ${person.familyName}"
        holder.txtAbout.text = person.about
        val imgUserPhoto = holder.imgProfilePhoto
        val profileImageURL = person.imageUrl
        if (!profileImageURL.isBlank()){
            GlideLoader(context).loadImage(URL(profileImageURL), imgUserPhoto)
        }
        holder.cardPeople.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra(AppConstants.USER_NAME, "${person.firstNames} ${person.familyName}")
            intent.putExtra(AppConstants.USER_ID, person.id)
            intent.putExtra(AppConstants.USER_IMAGE_URL, person.imageUrl)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = people.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtAbout: TextView = itemView.findViewById(R.id.txtAbout)
        val imgProfilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePicture)
        val cardPeople: CardView = itemView.findViewById(R.id.cardPerson)
    }
}