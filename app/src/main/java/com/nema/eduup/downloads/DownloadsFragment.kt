package com.nema.eduup.downloads

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentDownloadsBinding
import com.nema.eduup.home.HomeActivity

class DownloadsFragment : Fragment() {

    private val TAG = DownloadsFragment::class.qualifiedName
    private lateinit var binding: FragmentDownloadsBinding
    private lateinit var listDownloadsRecyclerView: RecyclerView
    private lateinit var adapter: DownloadsRecyclerAdapter
    private lateinit var clNoDownloads: ConstraintLayout
    private lateinit var btnBrowse: Button

    private val viewModel by lazy { ViewModelProvider(requireActivity())[DownloadsViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDownloadsBinding.inflate(layoutInflater, container, false)
        init()

        adapter = DownloadsRecyclerAdapter(requireContext())
        listDownloadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listDownloadsRecyclerView.adapter = adapter
        loadDownloads()


        return binding.root
    }

    private fun init() {
        listDownloadsRecyclerView = binding.downloadsRecyclerView
        clNoDownloads = binding.clNoDownloads
        btnBrowse = binding.btnBrowse
    }

    private fun loadDownloads() {
        viewModel.allDownloads.observe(viewLifecycleOwner, Observer { downloads ->
            downloads.let {
                if (downloads.isEmpty()) {
                    clNoDownloads.visibility = View.VISIBLE
                    btnBrowse.setOnClickListener{
                        browse()
                    }
                }else {
                    clNoDownloads.visibility = View.GONE
                    adapter.clearDownloads()
                    adapter.addDownloads(downloads)
                }
            }
        })
    }

    private fun browse() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentBrowse)
    }

}