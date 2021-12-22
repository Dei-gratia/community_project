package com.nema.eduup.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.activities.ViewNoteActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.*

class DashboardRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<DashboardRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private val notes = ArrayList<Note>()
    private val TAG = DashboardRecyclerAdapter::class.qualifiedName

    fun addNotes(newNotes: List<Note>) {
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    fun setNotes(newNotes: List<Note>) {
        val diffCallback = AllNotesRecyclerAdapter.NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes.clear()
        notes.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
    }

    fun clearNotes(reminders: Boolean) {
        val newNotes = ArrayList<Note>()
        for (note in notes) {
            if (note.reminders != reminders) {
                newNotes.add(note)
            }
        }

        notes.clear()
        notes.addAll(newNotes)
    }

    fun addNote(newNote: Note) {
        notes.add(newNote)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_dashboard, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notes[position]
        Log.d(TAG, "Binding note " + note.title)
        holder.txtTitle.text = note.title
        holder.txtSubject.text = note.subject
        //holder.fileName.text = note.fileUrl
        //val dateFormat = SimpleDateFormat.getDateInstance("MM/dd/yyyy")
        val dateFormat = SimpleDateFormat.getDateInstance().format(note.date)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = note.date
        //holder.textDate.text = calendar.time.toString()
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
            val intent = Intent(context, ViewNoteActivity::class.java)
            intent.putExtra(AppConstants.NOTE, note)
            intent.putExtra(AppConstants.PERSONAL_NOTE, AppConstants.PERSONAL_NOTE)
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = notes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle = itemView.findViewById<TextView>(R.id.txtTitle)
        val txtSubject = itemView.findViewById<TextView>(R.id.tv_subject)
        val txtDate = itemView.findViewById<TextView>(R.id.txtDate)
        val cardNote = itemView.findViewById<CardView>(R.id.cardDashboard)
    }

}