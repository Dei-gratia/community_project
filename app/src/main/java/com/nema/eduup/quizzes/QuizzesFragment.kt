package com.nema.eduup.quizzes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.databinding.FragmentQuizzesBinding
import com.nema.eduup.uploadquiz.UploadQuizActivity
import com.nema.eduup.utils.AppConstants
import java.util.*

class QuizzesFragment : Fragment() {

    private val TAG = QuizzesFragment::class.qualifiedName
    private lateinit var binding: FragmentQuizzesBinding
    private lateinit var toolbar: Toolbar
    private lateinit var tvToolbarTitle: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var listQuizzesRecyclerView: RecyclerView
    private lateinit var clNoQuizzes: ConstraintLayout
    private lateinit var btnBrowse: Button
    private lateinit var adapter: QuizzesRecyclerAdapter
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbUploadDateNTO: RadioButton
    private lateinit var rbUploadDateOTN: RadioButton
    private lateinit var rbRating: RadioButton
    private lateinit var radioButtonView: View
    private lateinit var loadQuizzesView: View
    private lateinit var spinnerQuizzesLevel: Spinner
    private lateinit var spinnerQuizzesSubject: Spinner
    private lateinit var quizzesLevel: String
    private lateinit var quizzesSubject: String
    private lateinit var acQuizzesSubject: AutoCompleteTextView
    private var currentUser: User = User()
    private var quizList = arrayListOf<Quiz>()
    private var userId = "-1"
    private var userLevel: String = "College"

    private val viewModel by lazy { ViewModelProvider(requireActivity())[QuizzesFragmentViewModel::class.java] }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var upLoadDateComparator = Comparator<Quiz> { quiz1, quiz2 ->
        quiz1.date.compareTo(quiz2.date)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = FragmentQuizzesBinding.inflate(layoutInflater, container, false)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
            if (currentUser.schoolLevel.isNotBlank()) {
                userLevel = currentUser.schoolLevel
            }
        }
        quizzesLevel =
            sharedPreferences.getString(AppConstants.QUIZZES_LEVEL, userLevel).toString()

        val sub =
            sharedPreferences.getString(AppConstants.QUIZZES_SUBJECT, AppConstants.ALL_SUBJECTS).toString()
        quizzesSubject = if (sub.isNotBlank()) {
            sub
        } else {
            AppConstants.ALL_SUBJECTS
        }
        setHasOptionsMenu(true)
        init()
        tvToolbarTitle.text = "Exam Practise Quizzes"

        adapter = QuizzesRecyclerAdapter(requireContext())
        listQuizzesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        listQuizzesRecyclerView.adapter = adapter
        loadQuizzes(quizzesLevel.lowercase(), quizzesSubject.lowercase())

        return binding.root
    }

    private fun init() {
        btnBrowse = binding.btnBrowse
        clNoQuizzes = binding.clNoQuizzes
        listQuizzesRecyclerView = binding.quizzesRecyclerView
        tvToolbarTitle = (activity as PractiseQuestionsActivity).tvToolbarTitle
        //toolbar = (activity as PractiseQuestionsActivity).toolbar
    }

    private fun loadQuizzes(level: String, subject: String) {
        val collection = firestoreInstance.collection(level.lowercase()).document(
            subject.lowercase()).collection("${level.lowercase()}${AppConstants.PUBLIC_QUIZZES}")
        if (subject.lowercase() == AppConstants.ALL_SUBJECTS.lowercase() || subject.isBlank()) {
            viewModel.getLevelQuizzes(level.lowercase()).observe(viewLifecycleOwner, {quizzes ->
                quizzes.let {
                    if (quizzes.isEmpty()) {
                        clNoQuizzes.visibility = View.VISIBLE
                        btnBrowse.setOnClickListener {
                            browse()
                        }
                    } else {
                        clNoQuizzes.visibility = View.GONE
                        adapter.clearQuizzes()
                        adapter.addQuizzes(quizzes)
                        quizList.clear()
                        quizList.addAll(quizzes)
                    }
                }
            })
        } else {
            viewModel.getQuizzes(collection).observe(viewLifecycleOwner, { quizzes ->
                quizzes.let {
                    if (quizzes.isEmpty()) {
                        clNoQuizzes.visibility = View.VISIBLE
                        btnBrowse.setOnClickListener {
                            browse()
                        }
                    } else {
                        clNoQuizzes.visibility = View.GONE
                        adapter.clearQuizzes()
                        adapter.addQuizzes(quizzes)
                        quizList.clear()
                        quizList.addAll(quizzes)
                    }
                }
            })
        }
    }

    private fun browse() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_frag).navigate(R.id.fragmentBrowse)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val sortItem: MenuItem = menu.findItem(R.id.action_sort)
        val filterItem: MenuItem = menu.findItem(R.id.action_filter)
        val uploadItem: MenuItem = menu.findItem(R.id.action_upload_quiz)
        sortItem.isVisible = true
        filterItem.isVisible = true
        uploadItem.isVisible = true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort -> {
                sort()
                return true
            }
            R.id.action_filter -> {
                filterQuizzes()
                return true
            }
            R.id.action_upload_quiz -> {
                uploadQuiz()
            }
        }

        return false
    }

    private fun uploadQuiz() {
        val uploadIntent = Intent(requireContext(), UploadQuizActivity::class.java)
        uploadIntent.putExtra(AppConstants.USER_LEVEL, currentUser.schoolLevel)
        startActivity(uploadIntent)
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
        rbRating.visibility = View.GONE
        AlertDialog.Builder(requireContext())
            .setTitle("Sort By?")
            .setView(radioButtonView)
            .setPositiveButton("OK") { _, _ ->
                if (rbUploadDateNTO.isChecked) {
                    Collections.sort(quizList, upLoadDateComparator)
                    adapter.clearQuizzes()
                    adapter.addQuizzes(quizList)
                }

                if (rbUploadDateOTN.isChecked) {
                    Collections.sort(quizList, upLoadDateComparator)
                    quizList.reverse()
                    adapter.clearQuizzes()
                    adapter.addQuizzes(quizList)
                }
                
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

    private fun filterQuizzes() {
        loadQuizzesView = View.inflate(
            requireContext(),
            R.layout.filter_quizzes_layout,
            null
        )

        spinnerQuizzesLevel = loadQuizzesView.findViewById(R.id.spinner_quizzes_level)
        spinnerQuizzesSubject = loadQuizzesView.findViewById(R.id.spinner_quizzes_subject)
        acQuizzesSubject = loadQuizzesView.findViewById(R.id.et_quizzes_subject)
        val spinnerLevelsAdapter = ArrayAdapter(
            requireContext(), R.layout.spinner_item, AppConstants.levels)
        spinnerQuizzesLevel.adapter = spinnerLevelsAdapter
        var levelPosition = spinnerLevelsAdapter.getPosition(quizzesLevel)
        spinnerQuizzesLevel.setSelection(levelPosition)


        val subjectAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireContext(),
            android.R.layout.select_dialog_item, AppConstants.subjects.distinct().sorted())
        acQuizzesSubject.threshold = 1
        acQuizzesSubject.setAdapter(subjectAdapter)
        acQuizzesSubject.setText(quizzesSubject)

        AlertDialog.Builder(requireContext())
            .setTitle("Filter Quizzes")
            .setView(loadQuizzesView)
            .setPositiveButton("Filter") { _, _ ->
                quizzesLevel = spinnerQuizzesLevel.selectedItem.toString()
                quizzesSubject = acQuizzesSubject.text.toString()
                loadQuizzes(quizzesLevel.lowercase(), quizzesSubject.lowercase())
                Log.e(TAG, "level is $quizzesLevel subject is $quizzesSubject")
                sharedPreferences.edit().putString(AppConstants.QUIZZES_LEVEL, quizzesLevel).apply()
                sharedPreferences.edit().putString(AppConstants.QUIZZES_SUBJECT, quizzesSubject).apply()
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

}