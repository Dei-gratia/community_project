package com.nema.eduup.browse

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.databinding.FragmentBrowseBinding
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList


class BrowseFragment : Fragment(), AllNotesRecyclerAdapter.OnBookmarkListener,
    AllNotesRecyclerAdapter.OnNoteSelectedListener {

    private val TAG = BrowseFragment::class.qualifiedName
    private lateinit var binding: FragmentBrowseBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var listNotesRecyclerView: RecyclerView
    private lateinit var listRemindersRecyclerView: RecyclerView
    private lateinit var adapter: AllNotesRecyclerAdapter
    private lateinit var remindersAdapter: AllNotesRecyclerAdapter
    private lateinit var etSearch: EditText
    private lateinit var pullToRefresh: SwipeRefreshLayout
    private lateinit var radioButtonView: View
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbUploadDateNTO: RadioButton
    private lateinit var rbUploadDateOTN: RadioButton
    private lateinit var rbRating: RadioButton
    private lateinit var filterNotesView: View
    private lateinit var spinnerNotesLevel: Spinner
    private lateinit var spinnerNotesSubject: Spinner
    private lateinit var notesLevel: String
    private lateinit var notesSubject: String
    private lateinit var acNotesSubject: AutoCompleteTextView
    private var userLevel: String = "College"
    private var notes : MutableLiveData<List<Note>> = MutableLiveData()
    private var reminders : MutableLiveData<List<Note>> = MutableLiveData()
    private var notesList = arrayListOf<Note>()
    private var bookmarkList = arrayListOf<String>()
    private var currentUser: User = User()
    private var userId = "-1"

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

    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(requireActivity())[BrowseFragmentViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentBrowseBinding.inflate(layoutInflater, container, false)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
            if (currentUser.schoolLevel.isNotBlank()) {
                userLevel = currentUser.schoolLevel
            }
        }
        notesLevel =
            sharedPreferences.getString(AppConstants.NOTES_LEVEL, userLevel).toString()
        val sub = sharedPreferences.getString(AppConstants.NOTES_SUBJECT, AppConstants.ALL_SUBJECTS).toString()
        notesSubject = if (sub.isNotBlank()) {
            sub
        } else {
            AppConstants.ALL_SUBJECTS
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
        Log.e(TAG, "level is $notesLevel subject is $notesSubject")
        loadNotes(notesLevel, notesSubject)
        pullToRefresh.setOnRefreshListener {
            loadReminders()
            loadBookmarks()
            loadNotes(userLevel, notesSubject)
            pullToRefresh.isRefreshing = false
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(strTyped: Editable?) {
                if (activity != null && isAdded){
                    filterFun(strTyped.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })


        etSearch.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                if (etSearch.text.isNotEmpty()) {
                    listRemindersRecyclerView.visibility = View.GONE
                }else {
                    listRemindersRecyclerView.visibility = View.VISIBLE
                    listNotesRecyclerView.requestFocus()
                }
            }else{
                listRemindersRecyclerView.visibility = View.GONE
            }
        }

        return binding.root
    }

    private fun init() {
        etSearch = (activity as HomeActivity).etSearch
        pullToRefresh = (activity as HomeActivity).pullToRefreshHome
        listRemindersRecyclerView = binding.listEduUpRemindersRecyclerView
        listNotesRecyclerView = binding.listEduUpNotesRecyclerView
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val sortItem: MenuItem = menu.findItem(R.id.action_sort)
        sortItem.isVisible = true
        val filterItem: MenuItem = menu.findItem(R.id.action_filter)
        filterItem.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort -> {
                sort()
                return true
            }
            R.id.action_filter -> {
                filterNotes()
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
                if (rbUploadDateNTO.isChecked) {
                    Collections.sort(notesList, upLoadDateComparator)
                    adapter.setNotes(notesList)
                }

                if (rbUploadDateOTN.isChecked) {
                    Collections.sort(notesList, upLoadDateComparator)
                    notesList.reverse()
                    adapter.setNotes(notesList)
                }

                if (rbRating.isChecked) {
                    Collections.sort(notesList, ratingComparator)
                    notesList.reverse()
                    adapter.setNotes(notesList)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

    private fun filterNotes() {
        filterNotesView = View.inflate(
            requireContext(),
            R.layout.filter_quizzes_layout,
            null
        )

        spinnerNotesLevel = filterNotesView.findViewById(R.id.spinner_quizzes_level)
        spinnerNotesSubject = filterNotesView.findViewById(R.id.spinner_quizzes_subject)
        acNotesSubject = filterNotesView.findViewById(R.id.et_quizzes_subject)
        val levels = arrayOf("All Levels","College", "A Level", "O Level", "Primary")
        val spinnerLevelsAdapter = ArrayAdapter(
            requireContext(), R.layout.spinner_item, levels)
        spinnerNotesLevel.adapter = spinnerLevelsAdapter
        val levelPosition = spinnerLevelsAdapter.getPosition(notesLevel)
        spinnerNotesLevel.setSelection(levelPosition)

        val subjectAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireContext(),
            android.R.layout.select_dialog_item, AppConstants.subjects.distinct().sorted())
        acNotesSubject.threshold = 1
        acNotesSubject.setAdapter(subjectAdapter)
        acNotesSubject.setText(notesSubject)

        AlertDialog.Builder(requireContext())
            .setTitle("Filter Notes")
            .setView(filterNotesView)
            .setPositiveButton("Filter") { _, _ ->
                notesLevel = spinnerNotesLevel.selectedItem.toString()
                //notesSubject = spinnerNotesSubject.selectedItem.toString()
                notesSubject = acNotesSubject.text.toString()
                loadNotes(notesLevel, notesSubject)
                sharedPreferences.edit().putString(AppConstants.NOTES_LEVEL, notesLevel).apply()
                sharedPreferences.edit().putString(AppConstants.NOTES_SUBJECT, notesSubject).apply()
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

    private fun loadReminders() {
        val collection = firestoreInstance.collection(AppConstants.REMINDERS)
        viewModel.getReminders(collection).observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            reminders.value = it
            remindersAdapter.setNotes(it)
        })
    }


    private fun loadNotes(level: String, subject: String) {
        val collection =
            firestoreInstance.collection(level.lowercase()).document(subject.lowercase())
                .collection("${level.lowercase()}${AppConstants.PUBLIC_NOTES}")
        if (subject == AppConstants.ALL_SUBJECTS || subject.isBlank()){
            viewModel.getLevelNotes(level.lowercase()).observe(viewLifecycleOwner, {
                notes.value = it
                notesList = it as ArrayList<Note>
                adapter.setNotes(it)
            })
        }
        else {
            viewModel.getNotes(collection).observe(viewLifecycleOwner, {
                notes.value = it
                notesList = it as ArrayList<Note>
                adapter.setNotes(it)
            })
        }
    }



    override fun onBookmarkSelected(noteId: String) {
        if (bookmarkList.contains(noteId)){
            viewModel.removeBookmark(noteId)
            bookmarkList.remove(noteId)
        }else{
            viewModel.addBookmark(noteId)
        }

    }

    private fun loadBookmarks() {
        val collection = firestoreInstance.collection(AppConstants.USERS).document(userId).collection(AppConstants.BOOKMARKS)
        viewModel.getBookmarks(collection).observe(viewLifecycleOwner, { bookmarks ->
            for (item in bookmarks){
                bookmarkList.add(item[AppConstants.NOTE_ID].toString())
            }
            adapter.addBookmarks(bookmarkList)

        })
    }

    override fun onNoteSelected(noteId: String) {
        viewModel.addToHistory(noteId)
    }

}