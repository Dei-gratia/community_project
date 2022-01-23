package com.nema.eduup.quiz

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.Navigation
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentQuizResultBinding
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.setTint


class QuizResultFragment : Fragment(), View.OnClickListener {

    private val TAG = QuizResultFragment::class.qualifiedName
    private lateinit var binding: FragmentQuizResultBinding
    private lateinit var tvRightQns: TextView
    private lateinit var tvWrongQns: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvSkipped: TextView
    private lateinit var tvRetakeQuiz: TextView
    private lateinit var tvShareScore: TextView
    private lateinit var tvTakeAnotherQuiz: TextView
    private lateinit var tvHome: TextView
    private lateinit var tvResultReaction: TextView
    private var correctQuestions = 0
    private var wrongQuestions = 0
    private var totalQuestions = 0
    private var quiz = Quiz()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentQuizResultBinding.inflate(layoutInflater, container, false)
        init()

        val bundle = this.arguments
        if (bundle != null) {
            correctQuestions = bundle.getInt(AppConstants.CORRECT_QUESTIONS)
            wrongQuestions = bundle.getInt(AppConstants.WRONG_QUESTIONS)
            totalQuestions = bundle.getInt(AppConstants.TOTAL_QUESTIONS)
            quiz = bundle.getParcelable(AppConstants.QUIZ)!!
            setResult()
        }

        tvRetakeQuiz.setOnClickListener(this)
        tvShareScore.setOnClickListener(this)
        tvTakeAnotherQuiz.setOnClickListener(this)
        tvHome.setOnClickListener(this)


        return binding.root
    }

    private fun init() {
        tvResultReaction = binding.tvResultReaction
        tvRightQns = binding.tvRightQns
        tvWrongQns = binding.tvWrongQns
        tvScore = binding.tvScore
        tvSkipped = binding.tvSkippedQns
        tvRetakeQuiz = binding.tvRetakeQuiz
        tvShareScore = binding.tvShareScore
        tvTakeAnotherQuiz = binding.tvTakeAnotherQuiz
        tvHome = binding.tvHome
    }

    private fun setResult() {
        tvRightQns.text = "Correct: $correctQuestions"
        tvWrongQns.text = "Wrong: $wrongQuestions"
        tvScore.text = "Score: $correctQuestions/$totalQuestions"
        tvSkipped.text = "Unattempted: ${totalQuestions.minus(correctQuestions.plus(wrongQuestions) )}"
        if (correctQuestions >= totalQuestions/2 ){
            tvScore.setTextColor(resources.getColor(R.color.badgeGreen))
            tvResultReaction.setTextColor(resources.getColor(R.color.badgeGreen))
            tvResultReaction.text = "Congratulations you passed!!"
            if (correctQuestions == totalQuestions) {
                tvResultReaction.text = "Wow you rock!!"
            }
        }
    }

    private fun quizzesActivity() {
        val intent = Intent(requireContext(),PractiseQuestionsActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    private fun homeActivity() {
        val intent = Intent(requireContext(),HomeActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    private fun shareScore() {
        Toast.makeText(requireContext(), "Share score $correctQuestions/$totalQuestions", Toast.LENGTH_LONG).show()
    }

    private fun retakeQuiz() {
        val bundle = Bundle()
        bundle.putParcelable(AppConstants.QUIZ, quiz)
        Navigation.findNavController(requireActivity(), R.id.quizzes_nav_host_frag).popBackStack()
        Navigation.findNavController(requireActivity(), R.id.quizzes_nav_host_frag).navigate(R.id.fragmentQuiz, bundle)
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.tv_retake_quiz -> {
                    retakeQuiz()
                }
                R.id.tv_share_score -> {
                    shareScore()
                }
                R.id.tv_take_another_quiz -> {
                    quizzesActivity()
                }
                R.id.tv_home -> {
                    homeActivity()
                }
            }
        }
    }

}