package com.nema.eduup.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import com.nema.eduup.R
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.nema.eduup.activities.MainActivity
import com.nema.eduup.activities.viewmodels.NewDiscussionActivityViewModel
import com.nema.eduup.adapters.PeopleRecyclerAdapter
import com.nema.eduup.databinding.FragmentPeopleBinding
import com.nema.eduup.firebase.FirebaseStorageUtil
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager
import com.nema.eduup.utils.GlideLoader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception


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
    private val viewModel by lazy { ViewModelProvider(requireActivity())[NewDiscussionActivityViewModel::class.java] }

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
        if (ConnectionManager().isNetworkAvailable(requireContext())) {
            peopleListenerRegistration = FirestoreUtil.addUsersListener {
                adapter.addPeople(it)
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



    private fun showErrorSnackBar(message: String, errorMessage: Boolean){
        (activity as MainActivity).showErrorSnackBar(message, errorMessage)
    }

    private fun showProgressDialog(text: String){
        (activity as MainActivity).showProgressDialog(text)
    }

    private fun hideProgressDialog() {
        (activity as MainActivity).hideProgressDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        FirestoreUtil.removeListener(peopleListenerRegistration)
    }

}