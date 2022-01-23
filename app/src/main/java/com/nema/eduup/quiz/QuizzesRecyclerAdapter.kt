package com.nema.eduup.quiz

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.utils.AppConstants.openDownloadedAttachment
import java.text.SimpleDateFormat
import java.util.*
import android.os.Bundle
import android.util.Log
import com.nema.eduup.utils.AppConstants


class QuizzesRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<QuizzesRecyclerAdapter.ViewHolder>() {
    private val TAG = QuizzesRecyclerAdapter::class.qualifiedName
    private val layoutInflater = LayoutInflater.from(context)
    private val quizzes = ArrayList<Quiz>()

    fun addQuizzes(newQuizzes: List<Quiz>) {
        quizzes.addAll(newQuizzes)
        notifyDataSetChanged()
    }

    fun clearQuizzes() {
        this.quizzes.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_quiz, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val quiz = quizzes[position]
        holder.tvQuizTitle.text = quiz.title
        holder.tvQuizSubject.text = "${quiz.level} ${quiz.subject}"
        var duration = quiz.duration.toString()
        if (quiz.duration == 0) {
            duration = "untimed"
        }
        holder.tvDuration.text = "${duration}mins"

        holder.cardDownload.setOnClickListener {
            val bundle = Bundle()
            bundle.putParcelable(AppConstants.QUIZ, quiz)
            Navigation.findNavController(context as Activity, R.id.quizzes_nav_host_frag).navigate(R.id.fragmentQuiz, bundle)
        }
    }

    override fun getItemCount() = quizzes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuizTitle: TextView = itemView.findViewById(R.id.tv_quiz_title)
        val tvQuizSubject: TextView = itemView.findViewById(R.id.tv_quiz_subject)
        val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        val cardDownload: CardView = itemView.findViewById(R.id.cardDownload)
    }
}