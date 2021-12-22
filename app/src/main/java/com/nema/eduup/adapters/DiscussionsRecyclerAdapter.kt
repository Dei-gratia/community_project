package com.nema.eduup.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.activities.ChatActivity
import com.nema.eduup.models.ActiveChatChannel
import com.nema.eduup.models.Message
import com.nema.eduup.models.TextMessage
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.GlideLoader
import java.net.URL
import java.util.*
import android.text.Spannable

import android.text.style.ImageSpan

import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import com.nema.eduup.utils.AppConstants.getTimeAgo
import com.nema.eduup.utils.AppConstants.isYesterday


class DiscussionsRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<DiscussionsRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private val discussions = ArrayList<ActiveChatChannel>()
    private val TAG = DiscussionsRecyclerAdapter::class.qualifiedName
    private var currentUserId = "-1"

    fun addDiscussions(newDiscussions: List<ActiveChatChannel>) {
        val  list = newDiscussions.sortedByDescending { it.newestMessageDate }
        discussions.addAll(list)
        notifyDataSetChanged()
    }


    fun addDiscussion(newDiscussion: ActiveChatChannel) {
        var activeChatChannel = discussions.find { it.channelId == newDiscussion.channelId  }
        if (activeChatChannel != null){
            discussions[discussions.indexOf(activeChatChannel)] = newDiscussion
            discussions.sortedWith(compareBy { it.newestMessageDate })
            //activeChatChannel = newDiscussion
        } else {
            discussions.add(newDiscussion)
        }
        val list = discussions.sortedByDescending { it.newestMessageDate }
        discussions.clear()
        discussions.addAll(list)
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
        val discussion = discussions[position]

        holder.txtName.text = discussion.otherUserName
        val lastMessageTime = discussion.newestMessageDate
        val date = lastMessageTime.getTimeAgo()
        if (DateUtils.isToday(discussion.newestMessageDate.time)){
            holder.tvTime.text = DateFormat.format("hh:mm aa", lastMessageTime)
        }
        else if (isYesterday(lastMessageTime)){
            holder.tvTime.text = "Yesterday"
        }else {
            holder.tvTime.text = DateFormat.format("dd/mm/yyyy", lastMessageTime)
        }

        if (discussion.newestMessageSenderId == currentUserId) {
            if (discussion.newestMessage == "Media") {
                val ssb = SpannableStringBuilder("You:    Media")
                ssb.setSpan(ForegroundColorSpan(context.getColor(R.color.black)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
                val imgMedia: Drawable = context.resources.getDrawable(R.drawable.ic_image_black_24)
                val background = ShapeDrawable()
                background.paint.color = Color.GRAY
                val lineHeight = holder.txtAbout.lineHeight
                val layerDrawable = LayerDrawable(arrayOf(background, imgMedia))
                layerDrawable.setBounds(0, 0, lineHeight, lineHeight)
                val media = ImageSpan(layerDrawable, ImageSpan.ALIGN_BASELINE)
                ssb.setSpan(media, 5, 6, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)


                holder.txtAbout.setText(ssb, TextView.BufferType.SPANNABLE)
            }else{
                val ssb = SpannableStringBuilder("You:  ${discussion.newestMessage}")
                ssb.setSpan(ForegroundColorSpan(context.getColor(R.color.black)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
                holder.txtAbout.text = ssb


            }
        } else {
            if (discussion.newestMessage == "Media") {
                val ssb = SpannableStringBuilder("   Media")
                val imgMedia: Drawable = context.resources.getDrawable(R.drawable.ic_image_black_24)
                val background = ShapeDrawable()
                background.paint.color = Color.GRAY
                val lineHeight = holder.txtAbout.lineHeight
                val layerDrawable = LayerDrawable(arrayOf(background, imgMedia))
                layerDrawable.setBounds(0, 0, lineHeight, lineHeight)
                val media = ImageSpan(layerDrawable, ImageSpan.ALIGN_BASELINE)
                ssb.setSpan(media, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                /*ssb.setSpan(
                    ImageSpan(context, R.drawable.ic_image_black_24),
                    0,
                    1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )*/
                holder.txtAbout.setText(ssb, TextView.BufferType.SPANNABLE)
            }else{
                holder.txtAbout.text = discussion.newestMessage
            }
        }



        val imgUserPhoto = holder.imgProfilePhoto
        val profileImageURL = discussion.otherUserImageUrl
        if (!profileImageURL.isBlank()){
            GlideLoader(context).loadImage(URL(profileImageURL), imgUserPhoto)
        }

        holder.cardPeople.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra(AppConstants.USER_NAME, "${discussion.otherUserName} ")
            intent.putExtra(AppConstants.USER_ID, discussion.otherUserId)
            intent.putExtra(AppConstants.USER_IMAGE_URL, discussion.otherUserImageUrl)
            if (discussion.isGroup){
                intent.putExtra(AppConstants.USER_IDS, arrayListOf(currentUserId))
            }
            context.startActivity(intent)
        }



    }

    override fun getItemCount() = discussions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName = itemView.findViewById<TextView>(R.id.txtName)
        val txtAbout = itemView.findViewById<TextView>(R.id.txtAbout)
        val tvTime = itemView.findViewById<TextView>(R.id.tv_time)
        val imgProfilePhoto = itemView.findViewById<ImageView>(R.id.imgProfilePicture)
        val cardPeople = itemView.findViewById<CardView>(R.id.cardPerson)
    }
}