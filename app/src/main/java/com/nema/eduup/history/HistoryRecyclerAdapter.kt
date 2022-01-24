package com.nema.eduup.history

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.roundTo
import com.nema.eduup.utils.NotesDiffCallback
import com.nema.eduup.viewnote.ViewNoteActivity
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class
HistoryRecyclerAdapter(private val context: Context,
                       private var onBookmarkListener: OnBookmarkListener?,
                       private var onNoteSelectedListener: OnNoteSelectedListener?
) :
    RecyclerView.Adapter<HistoryRecyclerAdapter.ViewHolder>() {
    private val TAG = HistoryRecyclerAdapter::class.qualifiedName
    private val layoutInflater = LayoutInflater.from(context)
    private var notes = ArrayList<Note>()
    private var history = ArrayList<HashMap<String, Any>>()

    fun setNotes(newNotes: List<Note>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes.clear()
        notes.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setHistory(newHistory: List<HashMap<String, Any>>) {
        history.clear()
        history.addAll(newHistory)
        Log.e(TAG, "history is $history")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_history, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notes[position]
        var date = note.date
        for (i in history) {
            if (i[AppConstants.NOTE_ID] == note.id){
                Log.e(TAG, "date is ${i[AppConstants.DATE]}")
                date = i[AppConstants.DATE].toString().toLong()
            }
        }
        val formattedDate = DateUtils.getRelativeTimeSpanString(
            date,
            Calendar.getInstance().timeInMillis,
            DateUtils.MINUTE_IN_MILLIS
        )
        holder.tvHistoryDate.text = formattedDate
        holder.tvTitle.text = note.title.trim()
        holder.tvDescription.text = note.description.trim()

        holder.cardHistory.setOnClickListener {
            onNoteSelectedListener?.onNoteSelected(note.id)
            val intent = Intent(context, ViewNoteActivity::class.java)
            intent.putExtra(AppConstants.NOTE, note)
            context.startActivity(intent)
        }

    }


    override fun getItemCount() = notes.size

    fun filterList(filteredList: ArrayList<Note>) {
        notes = filteredList
        notifyDataSetChanged()
    }

    fun setBookmarkListener(listener: OnBookmarkListener) {
        onBookmarkListener = listener
    }



    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        val tvHistoryDate: TextView = itemView.findViewById(R.id.tv_history_date)
        val cardHistory: CardView = itemView.findViewById(R.id.cardHistory)
    }


    interface OnBookmarkListener {
        fun onBookmarkSelected(noteId: String)
    }

    interface OnNoteSelectedListener {
        fun onNoteSelected(noteId: String)
    }


}