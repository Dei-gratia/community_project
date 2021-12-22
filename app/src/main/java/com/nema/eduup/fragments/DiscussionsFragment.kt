package com.nema.eduup.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.activities.NewDiscussionActivity
import com.nema.eduup.adapters.DiscussionsRecyclerAdapter
import com.nema.eduup.databinding.FragmentDiscussionsBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.ActiveChatChannel
import com.nema.eduup.models.TextMessage
import com.nema.eduup.models.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager


class DiscussionsFragment : Fragment() , View.OnClickListener{

    private lateinit var binding: FragmentDiscussionsBinding
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var discussionsListenerRegistration: ListenerRegistration
    private lateinit var adapter: DiscussionsRecyclerAdapter
    private var currentUser: User = User()
    private var userId = "-1"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentDiscussionsBinding.inflate(layoutInflater, container, false)
        init()
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
        }
        chatsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DiscussionsRecyclerAdapter(requireContext())
        chatsRecyclerView.adapter = adapter

        adapter.getUserId(userId)
        loadGroupDiscussions()
        loadDiscussions()


        fabNewChat.setOnClickListener(this)

        return binding.root
    }

    fun init () {
        chatsRecyclerView = binding.listChatsRecyclerView
        //fabNewChat = binding.fabNewChat
        fabNewChat = this.requireActivity().findViewById<View>(R.id.fab_new) as FloatingActionButton
        fabNewChat.setImageResource(R.drawable.ic_group_add_black_24)
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when(view.id) {
                R.id.fab_new -> {
                    loadNewDiscussionActivity()
                }
            }
        }
    }

    private fun loadPeopleFragment() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentPeople)
    }

    private fun loadNewDiscussionActivity() {
        startActivity(Intent(requireContext(), NewDiscussionActivity::class.java))
    }

    private fun loadDiscussions() {
        if (ConnectionManager().isNetworkAvailable(requireContext())) {
            FirestoreUtil.getUserActiveChatChannels { userActiveChatChannels ->
                val activeChatChannels = ArrayList<ActiveChatChannel>()
                userActiveChatChannels.forEach{(otherUserId, channelId)->

                    FirestoreUtil.getUser(otherUserId){user ->
                        discussionsListenerRegistration = FirestoreUtil.addLastMessageListener(channelId){ lastMessage ->
                            Log.d("Messages", lastMessage.toString())
                            var lastMessageText = ""
                            if (lastMessage is TextMessage) {
                                lastMessageText = lastMessage.text
                            }
                            else {
                                lastMessageText = "Media"
                            }
                            val activeChatChannel = ActiveChatChannel(
                                channelId,
                                 "${user.firstNames} ${user.familyName}",
                                user.imageUrl,
                                user.id,
                                lastMessageText,
                                lastMessage.time,
                                lastMessage.senderId,
                                0
                            )
                            activeChatChannels.add(activeChatChannel)
                            adapter.addDiscussion(activeChatChannel)
                        }
                    }
                    Log.d("Active for each", activeChatChannels.toString())
                }
                Log.d("Active chat", activeChatChannels.toString())
                adapter.addDiscussions(activeChatChannels)

            }

        }
        else {
            val dialog = android.app.AlertDialog.Builder(requireContext())
            dialog.setTitle("Error")
            dialog.setMessage("No Internet Connection")
            dialog.setPositiveButton("Open Settings") { _, _ ->
                val settingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                startActivity(settingsIntent)
            }
            dialog.setNegativeButton("Exit") { _, _ ->
                ActivityCompat.finishAffinity(requireActivity())
            }
            dialog.create()
            dialog.show()
        }
    }


    private fun loadGroupDiscussions() {
        if (ConnectionManager().isNetworkAvailable(requireContext())) {
            FirestoreUtil.getUserActiveGroupChannels { userActiveGroupChannels ->
                val activeChatChannels = ArrayList<ActiveChatChannel>()
                userActiveGroupChannels.forEach{ channelId ->
                    FirestoreUtil.getGroup(channelId) { groupChatChannel ->
                        discussionsListenerRegistration = FirestoreUtil.addGroupLastMessageListener(channelId){ lastMessage ->
                            Log.d("Messages", lastMessage.toString())
                            var lastMessageText = ""
                            if (lastMessage is TextMessage) {
                                lastMessageText = lastMessage.text
                            }
                            else {
                                lastMessageText = "Media"
                            }
                            val activeChatChannel = ActiveChatChannel(
                                channelId,
                                groupChatChannel.groupName,
                                groupChatChannel.groupImageUrl,
                                channelId,
                                lastMessageText,
                                lastMessage.time,
                                lastMessage.senderId,
                                0,
                                true

                            )
                            activeChatChannels.add(activeChatChannel)
                            adapter.addDiscussion(activeChatChannel)
                        }
                    }
                    Log.d("Active for each", activeChatChannels.toString())
                }
                Log.d("Active chat", activeChatChannels.toString())
                adapter.addDiscussions(activeChatChannels)

            }

        }
        else {
            val dialog = android.app.AlertDialog.Builder(requireContext())
            dialog.setTitle("Error")
            dialog.setMessage("No Internet Connection")
            dialog.setPositiveButton("Open Settings") { _, _ ->
                val settingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                startActivity(settingsIntent)
            }
            dialog.setNegativeButton("Exit") { _, _ ->
                ActivityCompat.finishAffinity(requireActivity())
            }
            dialog.create()
            dialog.show()
        }
    }


}