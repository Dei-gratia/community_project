package com.nema.eduup.quiz

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentQuizQuestionsBinding
import com.nema.eduup.utils.AppConstants
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class QuizQuestionsFragment : Fragment(), View.OnClickListener {

    private val TAG = QuizQuestionsFragment::class.qualifiedName
    private lateinit var binding: FragmentQuizQuestionsBinding
    private lateinit var tvToolbarTitle: TextView
    private lateinit var tvQuizTimer: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvOptionOne: TextView
    private lateinit var tvOptionTwo: TextView
    private lateinit var tvOptionThree: TextView
    private lateinit var tvOptionFour: TextView
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private var countDownTimer: CountDownTimer? = null
    private var countDownInMilliSecond: Long = 0
    private val countDownInterval: Long = 60
    private var timeLeftMilliSeconds: Long = 0
    private var defaultColor: ColorStateList? = null
    private var quizLevel = AppConstants.ALL_LEVELS
    private var quizSubject = AppConstants.ALL_SUBJECTS
    private var currentPosition: Int = 1
    private var questionList: ArrayList<Question>? = null
    private var selectedOptionPosition: Int = 0
    private var quizId = "1"
    private var correctQuestions = 0
    private var wrongQuestions = 0
    private var quiz = Quiz()

    private val viewModel by lazy { ViewModelProvider(requireActivity())[QuizzesFragmentViewModel::class.java] }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentQuizQuestionsBinding.inflate(layoutInflater, container, false)
        init()
        val bundle = this.arguments
        if (bundle != null) {
            quiz = bundle.getParcelable(AppConstants.QUIZ)!!
            quizId = quiz.id
            quizSubject = quiz.subject
            quizLevel = quiz.level
            countDownInMilliSecond = (quiz.duration * 60 * 1000).toLong()
        }

        getQuestions()

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val dialog = android.app.AlertDialog.Builder(requireContext())
                dialog.setTitle("End quiz")
                dialog.setMessage("Going back will end this quiz")
                dialog.setPositiveButton("Continue with quiz") { mDialog, _ ->
                    mDialog.cancel()
                }
                dialog.setNegativeButton("End Quiz") { _, _ ->
                    if (timeLeftMilliSeconds != 0L){
                        countDownTimer?.cancel()
                    }
                    NavHostFragment.findNavController(this@QuizQuestionsFragment).navigateUp()
                }
                dialog.create()
                dialog.show()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, onBackPressedCallback
        )

        tvOptionOne.setOnClickListener(this)
        tvOptionTwo.setOnClickListener(this)
        tvOptionThree.setOnClickListener(this)
        tvOptionFour.setOnClickListener(this)
        btnSubmit.setOnClickListener(this)

        return binding.root
    }

    private fun init() {
        tvQuizTimer = binding.tvQuizTimer
        tvProgress = binding.tvProgress
        tvQuestion = binding.tvQuestion
        tvOptionOne = binding.tvOptionOne
        tvOptionTwo = binding.tvOptionTwo
        tvOptionThree = binding.tvOptionThree
        tvOptionFour = binding.tvOptionFour
        btnSubmit = binding.btnSubmit
        progressBar = binding.progressBar
        defaultColor = tvQuizTimer.textColors
        tvToolbarTitle = (activity as PractiseQuestionsActivity).tvToolbarTitle
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.tv_option_one -> {
                    selectedOptionView(tvOptionOne, 1)
                }
                R.id.tv_option_two -> {
                    selectedOptionView(tvOptionTwo, 2)
                }
                R.id.tv_option_three -> {
                    selectedOptionView(tvOptionThree, 3)
                }
                R.id.tv_option_four -> {
                    selectedOptionView(tvOptionFour, 4)
                }
                R.id.btn_submit -> {
                    submitQuestion()
                }
            }
        }
    }

    private fun submitQuestion() {
        if (selectedOptionPosition == 0) {
            currentPosition++

            when {
                currentPosition <= questionList!!.size -> {
                    setQuestion()
                }
                else -> {
                    finishQuiz()
                    Toast.makeText(
                        requireContext(),
                        "You have successfully completed the Quiz", Toast.LENGTH_SHORT
                    ).show()

                }
            }
        } else {
            checkAnswer()
        }
    }

    private fun checkAnswer() {
        val question = questionList?.get(currentPosition - 1)
        if (question!!.correctOption != selectedOptionPosition) {
            answerView(selectedOptionPosition, R.drawable.wrong_option_border_bg)
            wrongQuestions ++
        }
        else if (question!!.correctOption == selectedOptionPosition){
            correctQuestions++
            answerView(question.correctOption, R.drawable.correct_option_border_bg)
        }


        if (currentPosition == questionList!!.size) {
            btnSubmit.text = "Finish"
        } else {
            btnSubmit.text = "Go to next question"
        }
        tvOptionOne.isEnabled = false
        tvOptionTwo.isEnabled = false
        tvOptionThree.isEnabled = false
        tvOptionFour.isEnabled = false
        selectedOptionPosition = 0
    }

    private fun finishQuiz() {
        if (timeLeftMilliSeconds != 0L){
            countDownTimer?.cancel()
        }
        val bundle = Bundle()
        bundle.putInt(AppConstants.CORRECT_QUESTIONS, correctQuestions)
        bundle.putInt(AppConstants.WRONG_QUESTIONS, wrongQuestions)
        bundle.putInt(AppConstants.TOTAL_QUESTIONS, questionList!!.size)
        bundle.putParcelable(AppConstants.QUIZ, quiz)
        Navigation.findNavController(requireActivity(), R.id.quizzes_nav_host_frag).popBackStack()
        Navigation.findNavController(requireActivity(), R.id.quizzes_nav_host_frag).navigate(R.id.fragmentQuizResult, bundle)
    }

    private fun getQuestions(){
        val documentReference = firestoreInstance.collection(quizLevel.lowercase()).document(quizSubject.lowercase())
            .collection("${quizLevel.lowercase()}${AppConstants.PUBLIC_QUIZZES}").document(quizId)
        Log.e(TAG, documentReference.toString())
        viewModel.getQuestions(documentReference) {
            progressBar.max = it.size
            questionList = it
            if (questionList!!.isNotEmpty()){
                setTimer()
                setQuestion()
            }

        }
    }

    private fun setTimer() {
        if (countDownInMilliSecond == 0L){
            tvQuizTimer.visibility = View.GONE
        }
        else {
            timeLeftMilliSeconds = countDownInMilliSecond
            statCountDownTimer()
        }
    }

    private fun setQuestion() {
        tvOptionOne.isEnabled = true
        tvOptionTwo.isEnabled = true
        tvOptionThree.isEnabled = true
        tvOptionFour.isEnabled = true
        val question = questionList!![currentPosition - 1]

        defaultOptionsView()
        if (currentPosition == questionList!!.size) {
            btnSubmit.text = "Finish"
        } else {
            btnSubmit.text = "Submit"
        }

        progressBar.progress = currentPosition
        tvProgress.text = "$currentPosition" + "/" + progressBar.max

        tvQuestion.text = question.question
        tvOptionOne.text = question.optionOne
        tvOptionTwo.text = question.optionTwo
        tvOptionThree.text = question.optionThree
        tvOptionFour.text = question.optionFour
    }

    private fun defaultOptionsView() {
        val options = ArrayList<TextView>()
        options.add(0, tvOptionOne)
        options.add(1, tvOptionTwo)
        options.add(2, tvOptionThree)
        options.add(3, tvOptionFour)

        for (option in options) {
            option.setTextColor(Color.parseColor("#7A8089"))
            option.typeface = Typeface.DEFAULT
            option.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.default_option_border_bg
            )
        }

    }

    private fun selectedOptionView(tv: TextView, selectedOptionNum: Int) {
        defaultOptionsView()
        selectedOptionPosition = selectedOptionNum
        tv.setTextColor(Color.parseColor("#363A43"))
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.selected_option_border_bg
        )
    }

    private fun answerView(answer: Int, drawableView: Int) {
        when (answer) {
            1 -> {
                tvOptionOne.background = ContextCompat.getDrawable(
                    requireContext(), drawableView
                )
            }
            2 -> {
                tvOptionTwo.background = ContextCompat.getDrawable(
                    requireContext(), drawableView
                )
            }
            3 -> {
                tvOptionThree.background = ContextCompat.getDrawable(
                    requireContext(), drawableView
                )
            }
            4 -> {
                tvOptionFour.background = ContextCompat.getDrawable(
                    requireContext(), drawableView
                )
            }
        }
    }

    private fun timeOverAlertDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.quiz_time_over_dialog, null)
        builder.setView(view)
        val btnTimeOverOk = view.findViewById<Button>(R.id.btn_time_over_ok)
        val tvQuizDuration = view.findViewById<TextView>(R.id.tv_quiz_duration)
        tvQuizDuration.text = "${quiz.duration}minutes"
        val alertDialog = builder.create()
        btnTimeOverOk.setOnClickListener {
            finishQuiz()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun statCountDownTimer() {
        countDownTimer = object : CountDownTimer(timeLeftMilliSeconds, countDownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                binding.apply {
                    timeLeftMilliSeconds = millisUntilFinished
                    val hms = String.format(
                        "%02d:%02d:%02d",
                        (TimeUnit.MILLISECONDS.toHours(millisUntilFinished) - TimeUnit.DAYS.toHours(
                            TimeUnit.MILLISECONDS.toDays(
                                millisUntilFinished
                            )
                        )),
                        (TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished))),
                        (TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                        ))
                    )
                    val second = TimeUnit.MILLISECONDS.toSeconds(timeLeftMilliSeconds).toInt()
                    // %02d format the integer with 2 digit
                    val timer = String.format(Locale.getDefault(), "Time: %02d", second)
                    tvQuizTimer.text = hms
                    when {
                        timeLeftMilliSeconds < (60000) -> {
                            tvQuizTimer.setTextColor(Color.RED)
                        }
                        timeLeftMilliSeconds < (5 * 60 * 1000) -> {
                            if (countDownInMilliSecond >= 15 * 60 * 1000){
                                tvQuizTimer.setTextColor(resources.getColor(R.color.timer_warning_1))
                            }

                        }
                        else -> {
                            tvQuizTimer.setTextColor(defaultColor)
                        }
                    }
                }
            }
            override fun onFinish() {
                checkSelectedAnswer()
            }
        }.start()
    }

    private fun checkSelectedAnswer() {
        if (selectedOptionPosition == 0) {
            timeOverAlertDialog()
        }
        else {
            checkAnswer()
            timeOverAlertDialog()
        }
    }

}