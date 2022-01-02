package com.nema.eduup.discussions

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
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.databinding.FragmentDiscussionsBinding
import com.nema.eduup.discussions.chat.ActiveChatChannel
import com.nema.eduup.discussions.chat.ChatActivityViewModel
import com.nema.eduup.discussions.chat.TextMessage
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.repository.DiscussionRepository
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager

class DiscussionsFragment : Fragment(), View.OnClickListener {

    private val TAG = DiscussionsFragment::class.qualifiedName
    private lateinit var binding: FragmentDiscussionsBinding
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var discussionsListenerRegistration: ListenerRegistration
    private lateinit var adapter: DiscussionsRecyclerAdapter
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private lateinit var clNoDiscussions: ConstraintLayout
    private lateinit var btnAddNewDiscussion: Button
    private val discussionRepository = DiscussionRepository
    private var currentUser: User = User()
    private var userId = "-1"

    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(this)[DiscussionsFragmentViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        clNoDiscussions.visibility = View.VISIBLE
        loadGroupDiscussions()
        loadDiscussions()

        pullToRefresh.setOnRefreshListener {
            clNoDiscussions.visibility = View.VISIBLE
            loadGroupDiscussions()
            loadDiscussions()
            pullToRefresh.isRefreshing = false
        }

        fabNewChat.setOnClickListener(this)
        btnAddNewDiscussion.setOnClickListener(this)

        return binding.root
    }

    fun init () {
        chatsRecyclerView = binding.listChatsRecyclerView
        clNoDiscussions = binding.clNoDiscussions
        btnAddNewDiscussion = binding.btnAddDiscussion
        fabNewChat = this.requireActivity().findViewById<View>(R.id.fab_new) as FloatingActionButton
        fabNewChat.setImageResource(R.drawable.ic_group_add_black_24)
        pullToRefresh = (activity as HomeActivity).pullToRefreshHome

    }

    override fun onClick(view: View?) {
        if (view != null) {
            when(view.id) {
                R.id.fab_new -> {
                    loadNewDiscussionActivity()
                }
                R.id.btn_add_discussion -> {
                    loadNewDiscussionActivity()
                }
            }
        }
    }

    private fun loadNewDiscussionActivity() {
        startActivity(Intent(requireContext(), NewDiscussionActivity::class.java))
    }

    private fun loadDiscussions() {
        val activeChatChannels = ArrayList<ActiveChatChannel>()
        viewModel.getUserActiveChatChannels {  userActiveChatChannels ->
            if (userActiveChatChannels.isNotEmpty()) {
                clNoDiscussions.visibility = View.GONE
                userActiveChatChannels.forEach{(otherUserId, channelId)->
                    viewModel.getUser(otherUserId) { user ->
                        Log.e(TAG, "user $user")
                        val collectionReference =
                            firestoreInstance.collection(AppConstants.CHAT_CHANNELS).document(channelId).collection(AppConstants.MESSAGES)
                        discussionsListenerRegistration = discussionRepository.addLastMessageListener(collectionReference){ lastMessage ->
                            var lastMessageText = ""
                            lastMessageText = if (lastMessage is TextMessage) {
                                lastMessage.text
                            } else {
                                "Media"
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
                }
            }

        }
    }

    private fun loadGroupDiscussions() {
        viewModel.getUserActiveGroupChannels { userActiveGroupChannels ->
            val activeChatChannels = ArrayList<ActiveChatChannel>()
            if (userActiveGroupChannels.isNotEmpty()) {
                clNoDiscussions.visibility = View.GONE
                userActiveGroupChannels.forEach{ channelId ->
                    viewModel.getGroup(channelId) { groupChatChannel ->
                        val collectionReference =
                            firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS).document(channelId).collection(AppConstants.MESSAGES)
                        discussionsListenerRegistration = discussionRepository.addLastMessageListener(collectionReference){ lastMessage ->
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
                }
            }
        }
    }
}