package com.nema.eduup.bookmarks

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.nema.eduup.databinding.FragmentBookmarksBinding
import com.nema.eduup.auth.User
import com.nema.eduup.browse.AllNotesRecyclerAdapter
import com.nema.eduup.browse.BrowseFragmentViewModel
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants


class BookmarksFragment : Fragment(), AllNotesRecyclerAdapter.OnBookmarkListener,
AllNotesRecyclerAdapter.OnNoteSelectedListener {

    private val TAG = BookmarksFragment::class.qualifiedName
    private lateinit var binding: FragmentBookmarksBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bookmarksRecyclerView: RecyclerView
    private lateinit var adapter: AllNotesRecyclerAdapter
    private lateinit var firestoreNotesListener: ListenerRegistration
    private lateinit var clNoBookmarks: ConstraintLayout
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private var bookmarkList = arrayListOf<String>()
    private var currentUser = User()
    private var userId = "-1"
    private var userLevel = "All Levels"


    private val viewModel by lazy { ViewModelProvider(requireActivity())[BrowseFragmentViewModel::class.java] }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentBookmarksBinding.inflate(layoutInflater, container, false)
        init()
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
            userLevel = currentUser.schoolLevel
        }

        adapter = AllNotesRecyclerAdapter(requireContext(), this, this)
        bookmarksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        bookmarksRecyclerView.adapter = adapter
        adapter.isBookmarksFrag(true)

        getBookmarkIds()

        pullToRefresh.setOnRefreshListener {
            getBookmarkIds()
            pullToRefresh.isRefreshing = false
        }


        return binding.root
    }

    private fun init() {
        bookmarksRecyclerView = binding.bookmarksRecyclerView
        clNoBookmarks = binding.clNoBookmarks
        pullToRefresh = (activity as HomeActivity).pullToRefreshHome
    }

    private fun getBookmarkIds(){
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.BOOKMARKS)
        viewModel.getBookmarks(collection).observe(viewLifecycleOwner, { bookmarks ->

            if (bookmarks.isEmpty()) {
                clNoBookmarks.visibility = View.VISIBLE
                Log.e(TAG, "bookmark is empty")
            }
            else{
                for (item in bookmarks){
                    Log.e(TAG, "bookmark $item")
                    bookmarkList.add(item[AppConstants.NOTE_ID].toString())
                }
                getBookmarks()
            }

        })


    }

    private fun getBookmarks() {
        viewModel.getNotesByIds(userLevel.lowercase(), bookmarkList) {
            if (it.isEmpty()) {
                clNoBookmarks.visibility = View.VISIBLE
            }
            else{
                clNoBookmarks.visibility = View.GONE
                adapter.setNotes(it)
            }
        }
    }



    override fun onBookmarkSelected(noteId: String) {
        bookmarkList.remove(noteId)
        viewModel.removeBookmark(noteId)

    }


    override fun onNoteSelected(noteId: String) {
        viewModel.addToHistory(noteId)
    }


}