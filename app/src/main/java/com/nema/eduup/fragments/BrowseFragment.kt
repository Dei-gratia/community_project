package com.nema.eduup.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.activities.MainActivity
import com.nema.eduup.activities.viewmodels.BrowseFragmentViewModel
import com.nema.eduup.activities.viewmodels.HomeFragmentViewModel
import com.nema.eduup.adapters.AllNotesRecyclerAdapter
import com.nema.eduup.adapters.AutoSliderImageAdapter
import com.nema.eduup.databinding.FragmentBrowseBinding
import com.nema.eduup.databinding.FragmentHomeBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import java.util.*
import java.util.EventListener
import kotlin.Comparator
import kotlin.collections.ArrayList


class BrowseFragment : Fragment() , AllNotesRecyclerAdapter.OnBookmarkListener,
    AllNotesRecyclerAdapter.OnNoteSelectedListener{

    private val TAG = BrowseFragment::class.qualifiedName
    private lateinit var binding: FragmentBrowseBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var listNotesRecyclerView: RecyclerView
    private lateinit var listRemindersRecyclerView: RecyclerView
    private lateinit var adapter: AllNotesRecyclerAdapter
    private lateinit var remindersAdapter: AllNotesRecyclerAdapter
    private lateinit var remindersCollection: CollectionReference
    private lateinit var notesListenerRegistration: ListenerRegistration
    private lateinit var etSearch: EditText
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private lateinit var radioButtonView: View
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbUploadDateNTO: RadioButton
    private lateinit var rbUploadDateOTN: RadioButton
    private lateinit var rbRating: RadioButton
    private lateinit var userLevel: String
    private lateinit var imgSlider: SliderView
    private lateinit var scrollView: NestedScrollView
    private lateinit var notesProgressDialog: Dialog
    private lateinit var notesProgressLayout: ConstraintLayout
    private lateinit var tvNotesProgressDialog: TextView
    private var sliderImageList = ArrayList<String>()
    private var notes : MutableLiveData<List<Note>> = MutableLiveData()
    private var reminders : MutableLiveData<List<Note>> = MutableLiveData()
    private var notesList = arrayListOf<Note>()
    private var bookmarkList = arrayListOf<Note>()
    private var currentUser: User = User()
    private var userId = "-1"

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    var ratingComparator = Comparator<Note> { note1, note2 ->
        if (note1.avgRating.compareTo(note2.avgRating) == 0) {
            note1.title.compareTo(note2.title, true)
        } else {
            note1.avgRating.compareTo(note2.avgRating)
        }
    }

    var upLoadDateComparator = Comparator<Note> { note1, note2 ->
        note1.date.compareTo(note2.date)
    }

    private val viewModel by lazy { ViewModelProvider(requireActivity())[BrowseFragmentViewModel::class.java] }

    private val recentlyViewedNoteRecyclerAdapter by lazy { AllNotesRecyclerAdapter(requireContext(), this, this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentBrowseBinding.inflate(layoutInflater, container, false)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
            userLevel = currentUser.schoolLevel
        }
        init()
        adapter = AllNotesRecyclerAdapter(requireContext(),this, this)
        listNotesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listNotesRecyclerView.adapter = adapter

        remindersAdapter = AllNotesRecyclerAdapter(requireContext(),this, this)
        listRemindersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listRemindersRecyclerView.adapter = remindersAdapter


        loadReminders()
        loadBookmarks()
        loadNotes()
        pullToRefresh.setOnRefreshListener {
            loadReminders()
            loadBookmarks()
            loadNotes()
            pullToRefresh.isRefreshing = false
        }


        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(strTyped: Editable?) {
                Toast.makeText(requireContext(), "${etSearch.text}", Toast.LENGTH_SHORT).show()
                if (activity != null && isAdded){
                    filterFun(strTyped.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })


        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                //etSearch.clearFocus()
            }
        }

        etSearch.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                //hideKeyboard()
                if (etSearch.text.isNotEmpty()) {
                    listRemindersRecyclerView.visibility = View.GONE
                }else {
                    listRemindersRecyclerView.visibility = View.VISIBLE
                    listNotesRecyclerView.requestFocus()
                }
            }else{
                listRemindersRecyclerView.visibility = View.GONE
                //(listNotesRecyclerView.layoutManager as LinearLayoutManager).scrollToPosition(5)
            }
            //backPressedCallback.isEnabled = hasFocus
        }

        //activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)

        return binding.root
    }

    private fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun init() {
        etSearch = (activity as MainActivity).etSearch
        pullToRefresh = binding.pullToRefresh
        listRemindersRecyclerView = binding.listEduUpRemindersRecyclerView
        listNotesRecyclerView = binding.listEduUpNotesRecyclerView
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item: MenuItem = menu.findItem(R.id.action_sort)
        item.isVisible = true
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
        AlertDialog.Builder(requireContext())
            .setTitle("Sort By?")
            .setView(radioButtonView)
            .setPositiveButton("OK") { _, _ ->
                Log.d("Sort before", notesList.toString())
                if (rbUploadDateNTO.isChecked) {
                    Collections.sort(notesList, upLoadDateComparator)
                    notesList.reverse()
                    adapter.addNotes(notesList)
                    Log.d("Sort nTo", notesList.toString())
                    Toast.makeText(requireContext(), "new to old", Toast.LENGTH_SHORT).show()
                }

                if (rbUploadDateOTN.isChecked) {
                    Toast.makeText(requireContext(), "old to new", Toast.LENGTH_SHORT).show()
                    Collections.sort(notesList, upLoadDateComparator)
                    adapter.addNotes(notesList)
                    Log.d("Sort oTn", notesList.toString())
                }

                if (rbRating.isChecked) {
                    Toast.makeText(requireContext(), "rating", Toast.LENGTH_SHORT).show()
                    Collections.sort(notesList, ratingComparator)
                    notesList.reverse()
                    adapter.addNotes(notesList)
                    Log.d("Sort rating", notesList.toString())
                }
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

    private fun filterFun(strTyped: String) {
        val filteredList = arrayListOf<Note>()
        for (item in notesList) {
            if (item.title.lowercase(Locale.ROOT)
                    .contains(strTyped.lowercase(Locale.ROOT))
            ) {
                filteredList.add(item)
            }
        }

        if (filteredList.size == 0) {
            Toast.makeText(requireContext(), "No items found", Toast.LENGTH_LONG).show()
        }
        adapter.filterList(filteredList)

    }

    private fun displayRecentlyViewedNotes() {
        listRemindersRecyclerView.visibility = View.GONE
        listNotesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listNotesRecyclerView.adapter = recentlyViewedNoteRecyclerAdapter
        adapter.addNotes(viewModel.recentlyViewedNotes)
    }

    private fun loadReminders() {
        val collection = firestoreInstance.collection(AppConstants.REMINDERS)
        viewModel.getReminders(collection).observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            reminders.value = it
            remindersAdapter.setNotes(it)
            Log.d("Note reminder", it.toString())
        })
    }


    private fun loadData2() {
        if (ConnectionManager().isNetworkAvailable(requireContext())){
            loadBookmarks()
            FirestoreUtil.loadData { remindersList ->
                for (reminder in remindersList){
                    if (!notesList.contains(reminder)){
                            notesList.add(reminder)
                        }
                }
                adapter.addNotes(remindersList)
                loadNotes()

            }
        }
        else {
            Toast.makeText(requireContext(),"No Network", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadNotes() {
        val collection =
            firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES)
                .collection(userLevel)

        viewModel.getNotes(collection).observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            notes.value = it
            notesList = it as ArrayList<Note>
            adapter.setNotes(it)
            Log.d("Note notes", it.toString())
        })
    }

    private fun loadNotes2() {
        notesProgressLayout.visibility = View.GONE
        val collection =
            firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES)
                .collection(userLevel)
        FirestoreUtil.addNotesListener(collection) {note, mode ->
            if (mode == "1") {
                if (!notesList.contains(note)){
                    notesList.add(note)
                }
                adapter.addNote(note)
            }
            else if (mode == "2") {
                notesList.remove(note)
                adapter.removeNote(note)
            }
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

    override fun onBookmarkSelected(note: Note) {
        if (bookmarkList.contains(note)){
            viewModel.removeBookmark(note)
            bookmarkList.remove(note)
        }else{
            viewModel.addBookmark(note)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        if (::notesListenerRegistration.isInitialized) {
            FirestoreUtil.removeListener(notesListenerRegistration)
        }

    }

    fun View.setFocusAndKeyboard(){
        this.requestFocus()
        this.showKeyboard()
    }

    fun View.showKeyboard() {
        this.requestFocus()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun loadBookmarks() {
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.BOOKMARKS)
        viewModel.getBookmarks(collection).observe(viewLifecycleOwner, androidx.lifecycle.Observer { bookmarks ->
            Log.d("Note bookmarks", bookmarks.toString())
            bookmarkList.addAll(bookmarks)
            adapter.addBookmarks(bookmarks as ArrayList<Note>)

        })
    }


    private fun loadBookmarks2() {
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.BOOKMARKS)
        FirestoreUtil.addBookmarksListener(collection) {bookmark, mode ->
            if (mode == "1") {
                bookmarkList.add(bookmark)
                adapter.addBookmark(bookmark)
            }
            else if (mode == "2") {
                bookmarkList.remove(bookmark)
                adapter.removeBookmark(bookmark)
            }
        }
    }

    override fun onNoteSelected(note: Note) {
        viewModel.addToRecentlyViewedNotes(note)
    }
}