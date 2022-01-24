package com.nema.eduup.quizzes

import android.os.Bundle
import android.text.format.DateUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.Navigation
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentQuizBinding
import com.nema.eduup.reminder.QuizReminderDialog
import com.nema.eduup.utils.AppConstants
import java.util.*

class QuizFragment : Fragment(), View.OnClickListener {

    private val TAG = Quiz::class.qualifiedName
    private lateinit var binding: FragmentQuizBinding
    private lateinit var tvToolbarTitle: TextView
    private lateinit var tvQuizUploadDate: TextView
    private lateinit var tvQuizLevel: TextView
    private lateinit var tvQuizSubject: TextView
    private lateinit var tvQuizDuration: TextView
    private lateinit var tvQuizNumberOfQuestions: TextView
    private lateinit var tvQuizDescription: TextView
    private lateinit var imgShareQuiz: ImageView
    private lateinit var imgQuizReminder: ImageView
    private lateinit var btnStartQuiz: Button
    private var quiz = Quiz()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentQuizBinding.inflate(layoutInflater, container, false)
        init()
        val bundle = this.arguments
        if (bundle != null) {
            quiz = bundle.getParcelable(AppConstants.QUIZ)!!
            tvToolbarTitle.text = quiz.title
            setQuizDetails()
        }

        imgShareQuiz.setOnClickListener(this)
        imgQuizReminder.setOnClickListener(this)
        btnStartQuiz.setOnClickListener(this)

        return binding.root
    }

    private fun init() {
        tvToolbarTitle = (activity as PractiseQuestionsActivity).tvToolbarTitle
        tvQuizUploadDate = binding.tvQuizUploadDate
        tvQuizLevel = binding.tvQuizLevel
        tvQuizSubject = binding.tvQuizSubject
        tvQuizDuration = binding.tvQuizDuration
        tvQuizNumberOfQuestions = binding.tvNumberOfQuestions
        tvQuizDescription = binding.tvQuizDescription
        imgShareQuiz = binding.imgQuizShare
        imgQuizReminder = binding.imgQuizReminder
        btnStartQuiz = binding.btnStart
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when(view.id) {
                R.id.btn_start -> {
                    startQuiz()
                }
                R.id.img_quiz_share -> {
                    shareQuiz()
                }
                R.id.img_quiz_reminder -> {
                    setQuizReminder()
                }
            }
        }
    }

    private fun setQuizDetails() {
        val formattedDate = DateUtils.getRelativeTimeSpanString(
            quiz.date,
            Calendar.getInstance().timeInMillis,
            DateUtils.MINUTE_IN_MILLIS
        )
        tvQuizUploadDate.text = formattedDate
        tvQuizLevel.text = quiz.level
        tvQuizSubject.text = quiz.subject
        tvQuizDuration.text = "${quiz.duration}minutes"
        tvQuizNumberOfQuestions.text = "${quiz.totalQuestions}questions"
        tvQuizDescription.text = quiz.description
    }

    private fun startQuiz() {
        val bundle = Bundle()
        bundle.putParcelable(AppConstants.QUIZ, quiz)
        Navigation.findNavController(requireActivity(), R.id.quizzes_nav_host_frag).navigate(R.id.fragmentQuizQuestions, bundle)
    }

    private fun shareQuiz() {
        Toast.makeText(requireContext(), "Share quiz", Toast.LENGTH_LONG).show()
    }

    private fun setQuizReminder() {
        displayCreateReminder()
        Toast.makeText(requireContext(), "Set reminder", Toast.LENGTH_LONG).show()
    }

    private fun displayCreateReminder() {
        val args = Bundle()
        args.putString(AppConstants.KEY_DATA, quiz.id)
        args.putString(AppConstants.NOTE_TITLE, quiz.title)
        val reminderDialog = QuizReminderDialog.newInstance(args)
        reminderDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_EduUp)
        reminderDialog.show(requireActivity().supportFragmentManager, QuizReminderDialog.TAG)
    }

}