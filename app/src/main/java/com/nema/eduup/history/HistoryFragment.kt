package com.nema.eduup.history

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.databinding.FragmentHistoryBinding
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import java.util.*
import kotlin.Comparator


class HistoryFragment : Fragment(), HistoryRecyclerAdapter.OnBookmarkListener,
    HistoryRecyclerAdapter.OnNoteSelectedListener {

    private val TAG = HistoryFragment::class.qualifiedName
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var listHistoryRecyclerView: RecyclerView
    private lateinit var clNoHistory: ConstraintLayout
    private lateinit var btnBrowse: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var radioButtonView: View
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbUploadDateNTO: RadioButton
    private lateinit var rbUploadDateOTN: RadioButton
    private lateinit var rbRating: RadioButton
    private var historyList = arrayListOf<Note>()
    private var currentUser: User = User()
    private var userId = "-1"
    private var userLevel = "All Levels"
    private var historyIdsList = arrayListOf<String>()

    private val viewModel by lazy { ViewModelProvider(requireActivity())[HistoryFragmentViewModel::class.java] }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val adapter by lazy { HistoryRecyclerAdapter(requireContext(), this, this) }

    var upLoadDateComparator = Comparator<Note> { note1, note2 ->
        note1.date.compareTo(note2.date)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        setHasOptionsMenu(true)
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
        getHistory()
        return binding.root
    }

    private fun init() {
        listHistoryRecyclerView = binding.historyRecyclerView
        clNoHistory = binding.clNoHistory
        btnBrowse = binding.btnBrowse
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val sortItem: MenuItem = menu.findItem(R.id.action_sort)
        sortItem.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort -> {
                sort()
                return true
            }
        }
        return false
    }


    private fun getHistory() {
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId)
            .collection(AppConstants.HISTORY)
        viewModel.getHistory(collection).observe(viewLifecycleOwner, {
            if (it.isEmpty()) {
                clNoHistory.visibility = View.VISIBLE
                btnBrowse.setOnClickListener{
                    browse()
                }
            }
            else {
                for (item in it){
                    historyIdsList.add(item[AppConstants.NOTE_ID].toString())
                }
                adapter.setHistory(it)
                getHistoryNotes()
            }
        })
    }

    private fun getHistoryNotes() {
        viewModel.getHistoryNotes(userLevel.lowercase(), historyIdsList) {
            if (it.isEmpty()) {
                clNoHistory.visibility = View.VISIBLE
                btnBrowse.setOnClickListener{
                    browse()
                }
            } else {
                clNoHistory.visibility = View.GONE
                adapter.setNotes(it)
            }
        }
    }

    override fun onBookmarkSelected(noteId: String) {

    }

    private fun browse() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentBrowse)
    }

    private fun sort() {
        radioButtonView = View.inflate(
            requireContext(),
            R.layout.sort_radio_button,
            null
        )

        radioGroup = radioButtonView.findViewById(R.id.groupradio)
        rbUploadDateNTO = radioButtonView.findViewById(R.id.radio_high_to_low)
        rbUploadDateOTN = radioButtonView.findViewById(R.id.radio_low_to_high)
        rbRating = radioButtonView.findViewById(R.id.radio_rating)
        rbRating.visibility = View.VISIBLE
        AlertDialog.Builder(requireContext())
            .setTitle("Sort By?")
            .setView(radioButtonView)
            .setPositiveButton("OK") { _, _ ->
                if (rbUploadDateNTO.isChecked) {
                    Collections.sort(historyList, upLoadDateComparator)
                    adapter.setNotes(historyList)
                }

                if (rbUploadDateOTN.isChecked) {
                    Collections.sort(historyList, upLoadDateComparator)
                    historyList.reverse()
                    adapter.setNotes(historyList)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }


    override fun onNoteSelected(noteId: String) {
        viewModel.addToHistory(noteId)
    }

}