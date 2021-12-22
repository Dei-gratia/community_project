package com.nema.eduup.fragments

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.nema.eduup.activities.MainActivity
import com.nema.eduup.activities.viewmodels.BrowseFragmentViewModel
import com.nema.eduup.activities.viewmodels.HomeFragmentViewModel
import com.nema.eduup.adapters.AllNotesRecyclerAdapter
import com.nema.eduup.databinding.FragmentBookmarksBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager


class BookmarksFragment : Fragment(), AllNotesRecyclerAdapter.OnBookmarkListener,
AllNotesRecyclerAdapter.OnNoteSelectedListener {

    private val TAG = BookmarksFragment::class.qualifiedName
    private lateinit var binding: FragmentBookmarksBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bookmarksRecyclerView: RecyclerView
    private lateinit var adapter: AllNotesRecyclerAdapter
    private lateinit var firestoreNotesListener: ListenerRegistration
    private lateinit var clNoBookmarks: ConstraintLayout
    private var bookmarkList = arrayListOf<Note>()
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
        bookmarksRecyclerView = binding.bookmarksRecyclerView
        clNoBookmarks = binding.clNoBookmarks
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

        loadData()

        return binding.root
    }

    private fun loadData(){
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.BOOKMARKS)
        viewModel.getBookmarks(collection).observe(viewLifecycleOwner, Observer {
            bookmarkList = it as ArrayList<Note>
            adapter.setNotes(it)

            if (bookmarkList.isEmpty()) {
                clNoBookmarks.visibility = View.VISIBLE
            }
            else{
                clNoBookmarks.visibility = View.GONE
            }
        })
        /*FirestoreUtil.addBookmarksListener(collection) {bookmark, mode ->
            if (mode == "1") {
                bookmarkList.add(bookmark)
                adapter.addBookmark(bookmark)
                adapter.addNote(bookmark)
            }
            else if (mode == "2") {
                bookmarkList.remove(bookmark)
                adapter.removeBookmark(bookmark)
                adapter.removeNote(bookmark)
            }
        }*/


    }

    private fun parseDocument(document: DocumentSnapshot): Note {
        return Note(
            document.id,
            document.getString("subject")!!,
            document.getString("title")!!,
            document.getString("description")!!,
            document.getString("body")!!,
            document.getString("fileUrl")!!,
            document.getString("fileType")!!,
            document.getString("level")!!,
            document.getLong("date")!!,
            document.getDouble("avgRating")!!,
            document.getLong("numRating")!!,
            document.getBoolean("reminders")!!
        )
    }

    private fun showErrorSnackBar(message: String, errorMessage: Boolean){
        (activity as MainActivity).showErrorSnackBar(message, errorMessage)
    }

    private fun showProgressDialog(text: String){
        (activity as MainActivity).showProgressDialog(text)
    }

    fun hideProgressDialog() {
        (activity as MainActivity).hideProgressDialog()
    }

    override fun onBookmarkSelected(note: Note) {
        if (bookmarkList.contains(note)){
            bookmarkList.remove(note)
            viewModel.removeBookmark(note)
        }else{
            bookmarkList.add(note)
            viewModel.addBookmark(note)
        }
    }

    private fun loadLocalBookmarks() {

    }

    override fun onNoteSelected(note: Note) {
        viewModel.addToRecentlyViewedNotes(note)
    }


}