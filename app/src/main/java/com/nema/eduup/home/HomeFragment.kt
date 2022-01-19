package com.nema.eduup.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.browse.AllNotesRecyclerAdapter
import com.nema.eduup.databinding.FragmentHomeBinding
import com.nema.eduup.discussions.NewDiscussionActivity
import com.nema.eduup.newnote.NewNoteActivity
import com.nema.eduup.quiz.PractiseQuestionsActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView

class HomeFragment : Fragment(), View.OnClickListener {

    private val TAG = HomeFragment::class.qualifiedName
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: HomeRecyclerAdapter
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
    private lateinit var clNoNotes: ConstraintLayout
    private lateinit var btnAddNote: Button
    private var sliderImageList = ArrayList<String>()
    private var remindersList = ArrayList<Note>()
    private var currentUser: User = User()
    private var notes : MutableLiveData<List<Note>> = MutableLiveData()
    private var userId = "-1"
    private var userLevel = "All Levels"

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(requireActivity())[HomeFragmentViewModel::class.java] }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
        }
        init()
        val sImgJson = sharedPreferences.getString(AppConstants.SLIDER_IMAGE_URLS, null)
        if (!sImgJson.isNullOrBlank()) {
            sliderImageList = Gson().fromJson(sImgJson, Array<String>::class.java).toMutableList() as ArrayList<String>
        }else {
            getImageUrls()
        }

        sliderImageAdapter = AutoSliderImageAdapter(requireContext())
        imgSlider.setSliderAdapter(sliderImageAdapter)
        setImageInSlider()

        remindersAdapter = AllNotesRecyclerAdapter(requireContext(),null, null)
        listRemindersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listRemindersRecyclerView.adapter = remindersAdapter

        adapter = HomeRecyclerAdapter(requireContext())
        listNotesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        listNotesRecyclerView.adapter = adapter
        //loadReminders()
        loadNotes()

        pullToRefresh.setOnRefreshListener {
            //loadReminders()
            loadNotes()
            pullToRefresh.isRefreshing = false
        }

        startNewNoteActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val newNote = result.data?.getParcelableExtra<Note>(AppConstants.NEW_NOTE)
                if (newNote != null) {
                    viewModel.addNoteToFirestore(newNote, firestoreInstance.collection(AppConstants.USERS).document(this.userId).collection(AppConstants.NOTES)){
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
        btnAddNote.setOnClickListener(this)


        return binding.root
    }

    private fun init() {
        tvJoinGroup = binding.tvHomeGridJoinGroup
        tvNewContent = binding.tvHomeGridNewContent
        tvQuestions = binding.tvHomeGridMcq
        tvShare = binding.tvHomeGridShare
        listNotesRecyclerView = binding.listNotesRecyclerView
        listRemindersRecyclerView = binding.listEduUpRemindersRecyclerView
        fabNewNote = this.requireActivity().findViewById<View>(R.id.fab_new) as FloatingActionButton
        imgSlider = binding.imgSlider
        pullToRefresh = (activity as HomeActivity).pullToRefreshHome
        scrollView = binding.scrollView
        notesProgressLayout = binding.clProgressLayout
        clStaticLayout = binding.clHomeStaticContainer
        clNoNotes = binding.clNoNotes
        btnAddNote = binding.btnAddNote
        fabNewNote.setImageResource(R.drawable.ic_note_add_black_24)
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
                R.id.btn_add_note -> {
                    newNote()
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
        Navigation.findNavController(requireActivity(), R.id.home_nav_host_frag).navigate(R.id.fragmentBrowse)
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
    

    private fun loadReminders() {
        val collection = firestoreInstance.collection(AppConstants.REMINDERS)
        viewModel.getReminders(collection).observe(viewLifecycleOwner, {
            remindersAdapter.setNotes(it)
        })
    }

    private fun loadNotes() {
        notesProgressLayout.visibility = View.GONE
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.NOTES)
        viewModel.getNotes(collection).observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                notes.value = it
                adapter.setNotes(it)
                clNoNotes.visibility = View.GONE
            }
            else {
                clNoNotes.visibility = View.VISIBLE
            }


        })
    }


    private fun newNote() {
        startNewNoteActivityForResult.launch(Intent(activity, NewNoteActivity::class.java))
    }

}