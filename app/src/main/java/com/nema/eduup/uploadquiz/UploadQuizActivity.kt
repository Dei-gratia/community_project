package com.nema.eduup.uploadquiz

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.BaseActivity
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.databinding.ActivityUploadQuizBinding
import com.nema.eduup.quizzes.Question
import com.nema.eduup.quizzes.Quiz
import com.nema.eduup.quizzes.QuizzesFragmentViewModel
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.hideKeyboard
import com.nema.eduup.utils.AppConstants.setFocusAndKeyboard
import java.util.*

class UploadQuizActivity : BaseActivity(), View.OnClickListener, QuestionsRecyclerAdapter.OnQuestionSelectedListener {

    private val TAG = UploadQuizActivity::class.qualifiedName
    private lateinit var binding: ActivityUploadQuizBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var listQuestionsRecyclerView: RecyclerView
    private lateinit var spinnerQuizLevel: Spinner
    private lateinit var acQuizSubject: AutoCompleteTextView
    private lateinit var etQuizTitle: TextView
    private lateinit var etQuizDescription: TextView
    private lateinit var etQuizDuration: TextView
    private lateinit var adapter: QuestionsRecyclerAdapter
    private lateinit var btnAddQuestion: Button
    private lateinit var btnUploadQuiz: Button
    private lateinit var questionView: View
    private var questionsList = arrayListOf<Question>()
    private var currentUser: User = User()
    private var userLevel: String = "College"
    val Activity.rootView get() = window.decorView.rootView

    private val viewModel by lazy { ViewModelProvider(this)[QuizzesFragmentViewModel::class.java] }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = ActivityUploadQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setupActionBar()
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            if (currentUser.schoolLevel.isNotBlank()) {
                userLevel = currentUser.schoolLevel
            }
        }
        val quizLevels = arrayOf("All levels", "College","A Level", "O Level", "Primary")
        val spinnerAdapter = ArrayAdapter(
            this, R.layout.spinner_item, AppConstants.levels)
        spinnerQuizLevel.adapter = spinnerAdapter
        var levelPosition = spinnerAdapter.getPosition(userLevel)
        spinnerQuizLevel.setSelection(levelPosition)

        val subjectAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this,
            android.R.layout.select_dialog_item, AppConstants.subjects.distinct().sorted())
        acQuizSubject.threshold = 1
        acQuizSubject.setAdapter(subjectAdapter)

        adapter = QuestionsRecyclerAdapter(this, this)
        listQuestionsRecyclerView.layoutManager = LinearLayoutManager(this)
        listQuestionsRecyclerView.adapter = adapter

        listQuestionsRecyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                listQuestionsRecyclerView.postDelayed({
                    listQuestionsRecyclerView.adapter?.itemCount?.minus(1)?.let {
                        if (it > 0) {
                            listQuestionsRecyclerView.smoothScrollToPosition(
                                it
                            )
                        }
                    }
                }, 100)
            }
        }

        btnAddQuestion.setOnClickListener(this)
        btnUploadQuiz.setOnClickListener(this)

    }

    private fun init() {
        toolbar = binding.toolbarUploadQuizActivity
        spinnerQuizLevel = binding.spinnerQuizLevel
        acQuizSubject = binding.etQuizSubject
        etQuizTitle = binding.etTitle
        etQuizDescription = binding.etQuizDescription
        etQuizDuration = binding.etQuizDuration
        btnAddQuestion = binding.btnAddQuestion
        btnUploadQuiz = binding.btnUploadQuiz
        listQuestionsRecyclerView = binding.questionsRecyclerView
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.btn_add_question -> {
                    this.hideKeyboard(rootView)
                    removeEditTextFocus()
                    addOrUpdateQuestion(Question())
                }
                R.id.btn_upload_quiz -> {
                    this.hideKeyboard(rootView)
                    removeEditTextFocus()
                    uploadQuiz()
                }
            }
        }
    }

    private fun removeEditTextFocus() {
        acQuizSubject.clearFocus()
        etQuizDescription.clearFocus()
        etQuizDuration.clearFocus()
        etQuizTitle.clearFocus()
    }


    private fun uploadQuiz() {
        if (validateQuizDetails()){
            showProgressDialog("Uploading Quiz")
            val quizTitle = etQuizTitle.text.toString()
            val quizDescription = etQuizDescription.text.toString()
            val quizLevel = spinnerQuizLevel.selectedItem.toString()
            val quizSubject = acQuizSubject.text.toString()
            val quizTotalQuestions = questionsList.size
            var quizDuration = 0
            if (etQuizDuration.text.isNotBlank()){
                quizDuration = etQuizDuration.text.toString().toInt()
            }
            val collection = firestoreInstance.collection(quizLevel.lowercase()).document(
                quizSubject.lowercase()).collection("${quizLevel.lowercase()}${AppConstants.PUBLIC_QUIZZES}")
            val quiz = Quiz(
                UUID.randomUUID().toString(),
                quizTitle,
                quizDescription,
                quizSubject,
                quizLevel,
                quizTotalQuestions,
                quizDuration,
                Calendar.getInstance().timeInMillis
            )
            viewModel.addQuizToFirestore(quiz, collection){
                uploadQuestions(it, quizLevel, quizSubject){
                    hideProgressDialog()
                    val toast = Toast.makeText(this, resources.getString(R.string.quiz_saved), Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                }
            }
        }
    }

    private fun validateQuizDetails(): Boolean {
        return when {
            etQuizTitle.text.isBlank() -> {
                etQuizTitle.setFocusAndKeyboard()
                etQuizTitle.error = "Quiz title can not be blank"
                false
            }
            questionsList.isEmpty() -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_quiz_no_qns), true)
                addOrUpdateQuestion(Question())
                false
            }
            else -> true
        }
    }

    private fun uploadQuestions(quizId: String, level: String, subject: String, onComplete: () -> Unit) {
        val collection = firestoreInstance.collection(level.lowercase()).document(
            subject.lowercase()).collection("${level.lowercase()}${AppConstants.PUBLIC_QUIZZES}").document(quizId).collection(AppConstants.QUESTIONS)
        for (qn in questionsList){
            viewModel.addQuestionToFirestore(qn, collection){

            }
        }
        onComplete()
    }

    private fun addOrUpdateQuestion(question: Question) {
        questionView = View.inflate(
            this,
            R.layout.add_question_layout,
            null
        )
        val etQuestion = questionView.findViewById<EditText>(R.id.et_question)
        val etOption1 = questionView.findViewById<EditText>(R.id.et_option_1)
        val etOption2 = questionView.findViewById<EditText>(R.id.et_option_2)
        val etOption3 = questionView.findViewById<EditText>(R.id.et_option_3)
        val etOption4 = questionView.findViewById<EditText>(R.id.et_option_4)
        val spinnerCorrectOption = questionView.findViewById<Spinner>(R.id.spinner_correct_option)
        val options = arrayOf("1","2", "3", "4")
        val spinnerAdapter = ArrayAdapter(
            this, R.layout.spinner_item, options)
        spinnerCorrectOption.adapter = spinnerAdapter
        etQuestion.setText(question.question)
        etOption1.setText(question.optionOne)
        etOption2.setText(question.optionTwo)
        etOption3.setText(question.optionThree)
        etOption4.setText(question.optionFour)
        spinnerCorrectOption.setSelection(spinnerAdapter.getPosition(question.correctOption.toString()))

        AlertDialog.Builder(this)
            .setTitle("Add New Question")
            .setView(questionView)
            .setCancelable(false)
            .setPositiveButton("Add Question") { _, _ ->
                val qn = etQuestion.text.toString()
                if (qn.isNotBlank()){
                    val option1 = etOption1.text.toString()
                    val option2 = etOption2.text.toString()
                    val option3 = etOption3.text.toString()
                    val option4 = etOption4.text.toString()
                    val correctOption = spinnerCorrectOption.selectedItem.toString().toInt()
                    val newQuestion = Question(UUID.randomUUID().toString(), qn, option1, option2, option3, option4, correctOption)
                    if (question == newQuestion || newQuestion == Question()){
                        Log.e(TAG, "Question not changed or empty")
                    }
                    else if (question == Question()) {
                        addQuestion(newQuestion)
                    }
                    else {
                        update(newQuestion)
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun update(newQuestion: Question){
        val qnPosition= questionsList.indexOfFirst {
            it.id == newQuestion.id
        }
        this.questionsList[qnPosition] = newQuestion
        adapter.updateQuestion(newQuestion)
    }


    private fun addQuestion(question: Question) {
        questionsList.add(question)
        adapter.addQuestion(question)
    }

    override fun onQuestionSelected(question: Question) {
        addOrUpdateQuestion(question)
    }

}