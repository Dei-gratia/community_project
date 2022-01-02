package com.nema.eduup.discussions.groups

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.discussions.chat.ChatActivity
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.GlideLoader
import java.net.URL
import kotlin.collections.ArrayList

class GroupsRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<GroupsRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private val groups = ArrayList<GroupChatChannel>()
    private val TAG = GroupsRecyclerAdapter::class.qualifiedName
    private var currentUserId = "-1"


    fun setGroups(newGroups: List<GroupChatChannel>) {
        val diffCallback = GroupsDiffCallback(groups, newGroups)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        groups.clear()
        groups.addAll(newGroups)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getUserId(userId: String) {
        currentUserId = userId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_group, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        holder.txtName.text = group.groupName
        holder.txtAbout.text = group.about
        val imgUserPhoto = holder.imgProfilePhoto
        val profileImageURL = group.groupImageUrl
        if (profileImageURL.isNotBlank()){
            GlideLoader(context).loadImage(URL(profileImageURL), imgUserPhoto)
        }

        if (group.userIds.contains(currentUserId)){
            holder.btnJoinGroup.visibility = View.INVISIBLE
            holder.cardGroup.setOnClickListener {
                loadGroup(group)
            }
        }
        else {
            holder.btnJoinGroup.visibility = View.VISIBLE
            holder.btnJoinGroup.setOnClickListener {

                val dialog = android.app.AlertDialog.Builder(context)
                dialog.setTitle("Join Group")
                dialog.setMessage("By joining this group you agree to follow and respect its rules")
                dialog.setPositiveButton("Join") { _, _ ->
                    loadGroup(group)
                }
                dialog.setNegativeButton("Exit") { _, _ ->

                }
                dialog.create()
                dialog.show()

            }
        }
    }

    private fun loadGroup (group: GroupChatChannel) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra(AppConstants.USER_NAME, group.groupName)
        intent.putExtra(AppConstants.USER_ID, group.channelId)
        intent.putExtra(AppConstants.USER_IDS, ArrayList(group.userIds))
        intent.putExtra(AppConstants.USER_IMAGE_URL, group.groupImageUrl)
        context.startActivity(intent)
    }

    override fun getItemCount() = groups.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtAbout: TextView = itemView.findViewById(R.id.txtAbout)
        val imgProfilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePicture)
        val btnJoinGroup: Button = itemView.findViewById(R.id.btn_join_group)
        val cardGroup: CardView = itemView.findViewById(R.id.cardPerson)
    }

    class GroupsDiffCallback(private val oldList: List<GroupChatChannel>, private val newList: List<GroupChatChannel>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].channelId === newList[newItemPosition].channelId
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val (_, value, name) = oldList[oldPosition]
            val (_, value1, name1) = newList[newPosition]

            return name == name1 && value == value1
        }

        @Nullable
        override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
            return super.getChangePayload(oldPosition, newPosition)
        }
    }
}