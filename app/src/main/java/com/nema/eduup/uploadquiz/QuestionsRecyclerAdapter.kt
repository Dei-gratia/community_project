package com.nema.eduup.uploadquiz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import java.util.*
import android.util.Log
import com.nema.eduup.quizzes.Question


class QuestionsRecyclerAdapter(private val context: Context, private var onQuestionSelectedListener: OnQuestionSelectedListener?) :
    RecyclerView.Adapter<QuestionsRecyclerAdapter.ViewHolder>() {
    private val TAG = QuestionsRecyclerAdapter::class.qualifiedName
    private lateinit var questionView: View
    private val layoutInflater = LayoutInflater.from(context)
    private val questions = ArrayList<Question>()


    fun addQuestions(newQuestions: List<Question>) {
        questions.addAll(newQuestions)
        notifyDataSetChanged()
    }

    fun clearQuestions() {
        this.questions.clear()
    }

    fun addQuestion(question: Question){
        this.questions.add(question)
        notifyItemChanged(questions.size - 1)
    }

    fun updateQuestion(newQuestion: Question) {
        val qnPosition= this.questions.indexOfFirst {
            it.id == newQuestion.id
        }
        this.questions[qnPosition] = newQuestion
        notifyItemChanged(qnPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_question, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]
        holder.tvQuestion.text = question.question
        holder.tvOption1.text = question.optionOne
        holder.tvOption2.text = question.optionTwo
        holder.tvOption3.text = question.optionThree
        holder.tvOption4.text = question.optionFour
        Log.e(TAG, "Correct option is ${question.correctOption}")
        holder.tvOption1.setTextColor(context.resources.getColor(R.color.text_hint_color))
        holder.tvOption2.setTextColor(context.resources.getColor(R.color.text_hint_color))
        holder.tvOption3.setTextColor(context.resources.getColor(R.color.text_hint_color))
        holder.tvOption4.setTextColor(context.resources.getColor(R.color.text_hint_color))
        when(question.correctOption) {
            1 -> {
                holder.tvOption1.setTextColor(context.resources.getColor(R.color.badgeGreen))
            }
            2 -> {
                holder.tvOption2.setTextColor(context.resources.getColor(R.color.badgeGreen))
            }
            3 -> {
                holder.tvOption3.setTextColor(context.resources.getColor(R.color.badgeGreen))
            }
            4 -> {
                holder.tvOption4.setTextColor(context.resources.getColor(R.color.badgeGreen))
            }
        }
        holder.cardQuestion.setOnClickListener {
            onQuestionSelectedListener?.onQuestionSelected(question)
        }
    }

    override fun getItemCount() = questions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestion: TextView = itemView.findViewById(R.id.tv_question)
        val tvOption1: TextView = itemView.findViewById(R.id.tv_option_1)
        val tvOption2: TextView = itemView.findViewById(R.id.tv_option_2)
        val tvOption3: TextView = itemView.findViewById(R.id.tv_option_3)
        val tvOption4: TextView = itemView.findViewById(R.id.tv_option_4)
        val cardQuestion: CardView = itemView.findViewById(R.id.cardQuestion)
    }

    fun setOnSelectedListener(listener: OnQuestionSelectedListener) {
        onQuestionSelectedListener = listener
    }

    interface OnQuestionSelectedListener {
        fun onQuestionSelected(question: Question)
    }


}