package com.nema.eduup.history

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.browse.AllNotesRecyclerAdapter
import com.nema.eduup.databinding.FragmentHistoryBinding
import com.nema.eduup.utils.AppConstants


class HistoryFragment : Fragment(), AllNotesRecyclerAdapter.OnBookmarkListener,
    AllNotesRecyclerAdapter.OnNoteSelectedListener {

    private lateinit var binding: FragmentHistoryBinding
    private lateinit var listHistoryRecyclerView: RecyclerView
    private lateinit var clNoHistory: ConstraintLayout
    private lateinit var btnBrowse: Button
    private lateinit var sharedPreferences: SharedPreferences
    private var currentUser: User = User()
    private var userId = "-1"
    private var userLevel = "All Levels"
    private var historyIdsList = arrayListOf<String>()

    private val viewModel by lazy { ViewModelProvider(requireActivity())[HistoryFragmentViewModel::class.java] }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val adapter by lazy { AllNotesRecyclerAdapter(requireContext(), this, this) }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentHistoryBinding.inflate(layoutInflater, container, false)
        init()
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
            userLevel = currentUser.schoolLevel
        }

        listHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listHistoryRecyclerView.adapter = adapter
        getHistoryIds()
        return binding.root
    }

    private fun init() {
        listHistoryRecyclerView = binding.historyRecyclerView
        clNoHistory = binding.clNoHistory
        btnBrowse = binding.btnBrowse
    }

    private fun getHistoryIds() {
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(
            AppConstants.HISTORY)
        viewModel.getHistoryIds(collection).observe(viewLifecycleOwner, {
            historyIdsList = it as ArrayList<String>
            if (historyIdsList.isEmpty()) {
                clNoHistory.visibility = View.VISIBLE
                btnBrowse.setOnClickListener{
                    browse()
                }
            } else {
                clNoHistory.visibility = View.GONE
                getHistory()
            }


        })
    }

    private fun getHistory() {
        val collection =
            firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES)
                .collection(userLevel)

        viewModel.getNotesByIds(collection, historyIdsList) {
            adapter.setNotes(it)
        }
    }

    override fun onBookmarkSelected(noteId: String) {

    }

    private fun browse() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentBrowse)
    }


    override fun onNoteSelected(noteId: String) {

    }

}