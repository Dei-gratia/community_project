package com.nema.eduup.discussions

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.nema.eduup.discussions.chat.Message
import com.nema.eduup.auth.User
import com.nema.eduup.discussions.groups.GroupChatChannel
import com.nema.eduup.repository.DiscussionRepository
import com.nema.eduup.repository.UserRepository

class DiscussionsFragmentViewModel(application: Application): AndroidViewModel(application), LifecycleObserver {
    private var discussionRepository = DiscussionRepository
    private var userRepository = UserRepository
    var users : MutableLiveData<List<User>> = MutableLiveData()
    val groups: MutableLiveData<List<GroupChatChannel>> = MutableLiveData()


    fun getUserActiveChatChannels(onComplete: (HashMap<String, String>) -> Unit) {
        discussionRepository.getUserActiveChatChannels {
            onComplete(it)
        }
    }

    fun getUserActiveGroupChannels(onComplete: (ArrayList<String>) -> Unit) {
        discussionRepository.getUserActiveGroupChannels {
            onComplete(it)
        }
    }

    fun getGroup(channelId: String, onComplete: (GroupChatChannel) -> Unit) {
        discussionRepository.getGroup(channelId) {
            onComplete(it)
        }
    }


    fun users(onListen: (LiveData<List<User>>) -> Unit){
        discussionRepository.addUsersListener {
            users.value = it
            onListen(users)
        }
    }

    fun getUser(userId: String, onComplete: (User) -> Unit) {
        userRepository.getUser(userId) {
            onComplete(it)
        }
    }

    fun groups(onListen: (LiveData<List<GroupChatChannel>>) -> Unit) {
        discussionRepository.addGroupsListener {
            groups.value = it
            onListen(groups)
        }
    }

    fun removeListener(registration: ListenerRegistration) {
        discussionRepository.removeListener(registration)
    }


}