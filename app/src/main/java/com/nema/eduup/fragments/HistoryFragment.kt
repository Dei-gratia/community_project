package com.nema.eduup.fragments

import android.os.Bundle
import android.util.Log
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
import com.nema.eduup.R
import com.nema.eduup.activities.viewmodels.HomeFragmentViewModel
import com.nema.eduup.adapters.AllNotesRecyclerAdapter
import com.nema.eduup.databinding.FragmentHistoryBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.roomDatabase.Note


class HistoryFragment : Fragment() , AllNotesRecyclerAdapter.OnBookmarkListener,
    AllNotesRecyclerAdapter.OnNoteSelectedListener{

    private lateinit var binding: FragmentHistoryBinding
    private lateinit var listHistoryRecyclerView: RecyclerView
    private lateinit var clNoHistory: ConstraintLayout
    private lateinit var btnBrowse: Button

    private val viewModel by lazy { ViewModelProvider(requireActivity())[HomeFragmentViewModel::class.java] }
    private val recentlyViewedNoteRecyclerAdapter by lazy { AllNotesRecyclerAdapter(requireContext(), this, this) }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHistoryBinding.inflate(layoutInflater, container, false)
        init()

        listHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listHistoryRecyclerView.adapter = recentlyViewedNoteRecyclerAdapter
        val historyList = viewModel.recentlyViewedNotes
        Log.d("History", historyList.toString())
        if (historyList.isEmpty()) {
            clNoHistory.visibility = View.VISIBLE
            btnBrowse.setOnClickListener{
                browse()
            }
        } else {
            clNoHistory.visibility = View.GONE
            recentlyViewedNoteRecyclerAdapter.addNotes(historyList)
        }


        return binding.root
    }

    private fun init() {
        listHistoryRecyclerView = binding.historyRecyclerView
        clNoHistory = binding.clNoHistory
        btnBrowse = binding.btnBrowse
    }

    override fun onBookmarkSelected(note: Note) {

    }

    private fun browse() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentBrowse)
    }


    override fun onNoteSelected(note: Note) {

    }


}