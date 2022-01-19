package com.nema.eduup.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.newnote.NewNoteActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.NotesDiffCallback
import java.text.SimpleDateFormat
import java.util.*

class HomeRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<HomeRecyclerAdapter.ViewHolder>() {
    private val TAG = HomeRecyclerAdapter::class.qualifiedName
    private val layoutInflater = LayoutInflater.from(context)
    private val notes = ArrayList<Note>()



    fun setNotes(newNotes: List<Note>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes.clear()
        notes.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_dashboard, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notes[position]
        holder.txtTitle.text = note.title
        holder.txtSubject.text = note.subject
        val dateFormat = SimpleDateFormat.getDateInstance().format(note.date)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = note.date
        holder.txtDate.text = dateFormat
        if (note.reminders) {
            holder.txtTitle.setTextColor(ContextCompat.getColor(context, R.color.colorOnSecondary))
            holder.cardNote.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimaryVariant
                )
            )
        }

        holder.cardNote.setOnClickListener {
            val intent = Intent(context, NewNoteActivity::class.java)
            intent.putExtra(AppConstants.NOTE, note)
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = notes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtSubject: TextView = itemView.findViewById(R.id.tv_subject)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val cardNote: CardView = itemView.findViewById(R.id.cardDashboard)
    }

}