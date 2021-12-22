package com.nema.eduup.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.nema.eduup.R
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.gson.Gson
import com.nema.eduup.activities.MainActivity
import com.nema.eduup.adapters.DashboardRecyclerAdapter
import com.nema.eduup.databinding.FragmentDashboardBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.activities.NewDiscussionActivity
import com.nema.eduup.activities.NewNoteActivity
import com.nema.eduup.activities.PractiseQuestionsActivity
import com.nema.eduup.activities.viewmodels.BrowseFragmentViewModel
import com.nema.eduup.adapters.AllNotesRecyclerAdapter
import com.nema.eduup.adapters.AutoSliderImageAdapter
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import java.util.*
import kotlin.collections.ArrayList




class DashboardFragment : Fragment(), View.OnClickListener {

    private val TAG = DashboardFragment::class.qualifiedName
    private lateinit var binding: FragmentDashboardBinding
    private lateinit var adapter: DashboardRecyclerAdapter
    private lateinit var noteViewModel: DashboardViewModel
    private lateinit var listNotesRecyclerView: RecyclerView
    private lateinit var remindersCollection: CollectionReference
    private lateinit var firestoreNotesListener: ListenerRegistration
    private lateinit var fabNewNote: FloatingActionButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var remindersAdapter: AllNotesRecyclerAdapter
    private lateinit var sliderImageAdapter: AutoSliderImageAdapter
    private lateinit var tvQuestions: TextView
    private lateinit var tvJoinGroup: TextView
    private lateinit var tvNewContent: TextView
    private lateinit var tvShare: TextView
    private lateinit var startNewNoteActivityForResult: ActivityResultLauncher<Intent>
    private lateinit var listRemindersRecyclerView: RecyclerView
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private lateinit var imgSlider: SliderView
    private lateinit var scrollView: NestedScrollView
    private lateinit var notesProgressLayout: ConstraintLayout
    private lateinit var tvNotesProgressDialog: TextView
    private lateinit var clStaticLayout: ConstraintLayout
    private var sliderImageList = ArrayList<String>()
    private var remindersList = ArrayList<Note>()
    private var currentUser: User = User()
    private var userId = "-1"
    private var userLevel = "All Levels"

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(requireActivity())[BrowseFragmentViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentDashboardBinding.inflate(layoutInflater, container, false)
        noteViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
        }
        init()
        val sImgJson = sharedPreferences.getString(AppConstants.SLIDER_IMAGE_URLS, null)
        if (!sImgJson.isNullOrBlank()) {
            sliderImageList = Gson().fromJson(sImgJson, Array<String>::class.java).toMutableList() as ArrayList<String>
            Log.d("SliderImages", sliderImageList.toString())
        }else {
            getImageUrls()
        }

        sliderImageAdapter = AutoSliderImageAdapter(requireContext())
        imgSlider.setSliderAdapter(sliderImageAdapter)
        setImageInSlider()

        remindersAdapter = AllNotesRecyclerAdapter(requireContext(),null, null)
        listRemindersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listRemindersRecyclerView.adapter = remindersAdapter

        adapter = DashboardRecyclerAdapter(requireContext())
        listNotesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        listNotesRecyclerView.adapter = adapter
        //loadReminders()
        loadNotes()

        pullToRefresh.setOnRefreshListener {
            //loadReminders()
            loadNotes()
            pullToRefresh.isRefreshing = false
        }


        //sliderImageList.add("https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__340.jpg")
        //sliderImageList.add("https://images.ctfassets.net/hrltx12pl8hq/4plHDVeTkWuFMihxQnzBSb/aea2f06d675c3d710d095306e377382f/shutterstock_554314555_copy.jpg")
        //sliderImageList.add("https://media.istockphoto.com/photos/child-hands-formig-heart-shape-picture-id951945718?k=6&m=951945718&s=612x612&w=0&h=ih-N7RytxrTfhDyvyTQCA5q5xKoJToKSYgdsJ_mHrv0=")
        //sliderImageList.add("https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__340.jpg")


        startNewNoteActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                //  you will get result here in result.data
                val newNote = result.data?.getParcelableExtra<Note>(AppConstants.NEW_NOTE)
                Log.d("Note", newNote.toString())
                if (newNote != null) {
                    FirestoreUtil.addNoteToFirestore(newNote, firestoreInstance.collection(AppConstants.USERS).document(this.userId).collection(AppConstants.NOTES)){
                        Toast.makeText(requireContext(), "New note saved to firestore!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        fabNewNote.setOnClickListener(this)
        tvShare.setOnClickListener(this)
        tvQuestions.setOnClickListener(this)
        tvJoinGroup.setOnClickListener(this)
        tvNewContent.setOnClickListener(this)

        return binding.root
    }

    private fun init() {
        tvJoinGroup = binding.tvHomeGridJoinGroup
        tvNewContent = binding.tvHomeGridNewContent
        tvQuestions = binding.tvHomeGridMcq
        tvShare = binding.tvHomeGridShare
        listNotesRecyclerView = binding.listNotesRecyclerView
        listRemindersRecyclerView = binding.listEduUpRemindersRecyclerView
        //fabNewNote = binding.fabNewNote
        fabNewNote = this.requireActivity().findViewById<View>(R.id.fab_new) as FloatingActionButton
        imgSlider = binding.imgSlider
        pullToRefresh = binding.pullToRefresh
        scrollView = binding.scrollView
        notesProgressLayout = binding.clProgressLayout
        clStaticLayout = binding.clHomeStaticContainer
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when(view.id) {
                R.id.fab_new -> {
                    newNote()
                }
                R.id.tv_home_grid_mcq -> {
                    loadQuestions()
                }
                R.id.tv_home_grid_join_group -> {
                    loadGroups()
                }
                R.id.tv_home_grid_share -> {
                    share()
                }
                R.id.tv_home_grid_new_content -> {
                    browse()
                }
            }
        }
    }

    private fun loadGroups() {
        val intent = Intent(requireContext(), NewDiscussionActivity::class.java)
        startActivity(intent)
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_SUBJECT, "EduUp")
        intent.putExtra(Intent.EXTRA_TEXT, "Learn")
        startActivity(Intent.createChooser(intent, "Share Notes"))
    }


    private fun browse() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentBrowse)
    }

    private fun loadQuestions() {
        startActivity(Intent(requireContext(), PractiseQuestionsActivity::class.java))
    }

    fun SharedPreferences.Editor.putArrayList(key: String, list: ArrayList<String>?): SharedPreferences.Editor {
        putString(key, list?.joinToString(",") ?: "")
        return this
    }

    fun SharedPreferences.getArrayList(key: String, defValue: ArrayList<String>?): ArrayList<String>? {
        val value = getString(key, null)
        if (value.isNullOrBlank())
            return defValue
        return ArrayList (value.split(",").map { it })
    }

    private fun setImageUrls(imgUrls: ArrayList<String>) {

    }

    private fun getImageUrls () {
        val sliderImagePath = "sliderImages/"
        val gson = Gson()
        viewModel.getSliderImages(sliderImagePath) {
            sliderImageList.add(it)
            sliderImageAdapter.renewItems(sliderImageList)
            val json = gson.toJson(sliderImageList)
            sharedPreferences.edit().putString(AppConstants.SLIDER_IMAGE_URLS, json).apply()
        }
    }

    private fun setImageInSlider() {

        sliderImageAdapter.renewItems(sliderImageList)
        imgSlider.isAutoCycle = true
        imgSlider.setIndicatorAnimation(IndicatorAnimationType.WORM)
        imgSlider.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION)
        imgSlider.autoCycleDirection = SliderView.AUTO_CYCLE_DIRECTION_BACK_AND_FORTH;
        imgSlider.indicatorSelectedColor = Color.WHITE;
        imgSlider.indicatorUnselectedColor = Color.GRAY;
        imgSlider.scrollTimeInSec = 5

        imgSlider.startAutoCycle()
    }

    private fun loadData() {

        notesProgressLayout.visibility = View.VISIBLE
        FirestoreUtil.loadData { remindersList ->
            adapter.addNotes(remindersList)
            loadNotes()

        }
    }

    private fun loadReminders() {
        val collection = firestoreInstance.collection(AppConstants.REMINDERS)
        viewModel.getReminders(collection).observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            remindersAdapter.setNotes(it)
            Log.d("Note reminder", it.toString())
        })
    }

    private fun loadNotes() {
        notesProgressLayout.visibility = View.GONE
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.NOTES)
        viewModel.getNotes(collection).observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            adapter.setNotes(it)
            Log.d("Note notes", it.toString())
        })
    }

    private fun loadNotes2() {
        notesProgressLayout.visibility = View.GONE
        firestoreNotesListener = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.NOTES)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->

                if (e != null) {
                    Log.e(TAG, "Failed to listen for new notes", e)
                    return@EventListener
                }

                if (snapshots!!.isEmpty) {

                } else{
                    for (dc in snapshots.documentChanges) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            adapter.addNote(dc.document.toObject(Note::class.java))
                        }
                    }
                }
            })

    }

    private fun newNote() {
        startNewNoteActivityForResult.launch(Intent(activity, NewNoteActivity::class.java))
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

}