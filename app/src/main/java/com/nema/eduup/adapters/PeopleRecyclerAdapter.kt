package com.nema.eduup.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.activities.ChatActivity
import com.nema.eduup.models.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.GlideLoader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class PeopleRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<PeopleRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private val people = ArrayList<User>()
    private val TAG = PeopleRecyclerAdapter::class.qualifiedName
    private var currentUserId = "-1"

    fun addPeople(newPeople: List<User>) {
        people.addAll(newPeople)
        notifyDataSetChanged()
    }

    fun clearPeople() {
        val newPeople = ArrayList<User>()
        for (person in people) {
            newPeople.add(person)

        }

        people.clear()
        people.addAll(newPeople)
    }

    fun addPerson(newPerson: User) {
        if (newPerson.id != currentUserId) {
            people.add(newPerson)
            notifyDataSetChanged()
        }
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
        println("person id is ${person.id} \n \n current user id $currentUserId \n \n \n")
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
        val txtName = itemView.findViewById<TextView>(R.id.txtName)
        val txtAbout = itemView.findViewById<TextView>(R.id.txtAbout)
        val imgProfilePhoto = itemView.findViewById<ImageView>(R.id.imgProfilePicture)
        val cardPeople = itemView.findViewById<CardView>(R.id.cardPerson)
    }
}