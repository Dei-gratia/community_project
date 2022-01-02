package com.nema.eduup.discussions.people

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.nema.eduup.databinding.FragmentPeopleBinding
import com.nema.eduup.auth.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager


class PeopleFragment : Fragment(){

    private val TAG = PeopleFragment::class.qualifiedName
    private lateinit var binding: FragmentPeopleBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var peopleListenerRegistration: ListenerRegistration
    private lateinit var peopleRecyclerView: RecyclerView
    private lateinit var adapter: PeopleRecyclerAdapter
    private var currentUser: User = User()
    private var groupProfileImageURL: String = ""
    private var userId = "-1"

    private val firestoreInstance by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(requireActivity())[PeopleFragmentViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentPeopleBinding.inflate(layoutInflater, container, false)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
        }
        init()

        peopleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PeopleRecyclerAdapter(requireContext())
        peopleRecyclerView.adapter = adapter

        adapter.getUserId(userId)
        loadPeople()



        return binding.root
    }

    private fun init() {
        peopleRecyclerView = binding.peopleRecyclerView
    }

    private fun loadPeople() {
        viewModel.users().observe(viewLifecycleOwner, {
            adapter.addPeople(it)
        })
    }

}