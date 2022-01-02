package com.nema.eduup.browse

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.home.HomeActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.roundTo
import com.nema.eduup.utils.NotesDiffCallback
import com.nema.eduup.viewnote.ViewNoteActivity
import kotlin.collections.ArrayList


class
AllNotesRecyclerAdapter(private val context: Context,
                              private var onBookmarkListener: OnBookmarkListener?,
                              private var onNoteSelectedListener: OnNoteSelectedListener?
) :
    RecyclerView.Adapter<AllNotesRecyclerAdapter.ViewHolder>() {
    private val TAG = AllNotesRecyclerAdapter::class.qualifiedName
    private val layoutInflater = LayoutInflater.from(context)
    private var notes = ArrayList<Note>()
    private var bookmarks = ArrayList<String>()
    private var isBookmarks = false

    fun setNotes(newNotes: List<Note>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes.clear()
        notes.addAll(newNotes)
        diffResult.dispatchUpdatesTo(this)
    }

    fun isBookmarksFrag(isIt: Boolean){
        isBookmarks = isIt
    }

    fun addBookmarks(newBookmarks: ArrayList<String>) {
        bookmarks = newBookmarks
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_note, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notes[position]
        holder.txtTitle.text = note.title
        holder.txtDescription.text = note.description
        val avgRating = note.avgRating.roundTo(1)
        holder.txtNoteRating.text = avgRating.toString()
        if (note.reminders) {
            holder.imgBookmark.visibility = View.INVISIBLE
            holder.txtNoteRating.visibility = View.INVISIBLE
            holder.imgIcon.setImageResource(R.drawable.ic_calendar_today_black_24)
            holder.imgIcon.visibility = View.INVISIBLE
            holder.txtTitle.setTextColor(ContextCompat.getColor(
                context,
                R.color.white
            ))
            holder.cardNote.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.grey_btm_nav
                )
            )
        }

        if (isBookmarks){
            holder.imgBookmark.setImageResource(R.drawable.ic_delete_black_24)
        } else {
            if (bookmarks.contains(note.id)){
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_filled_blue_24)
            }else {
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24)
            }
        }

        if (!note.reminders) {
            holder.cardNote.setOnClickListener {
                onNoteSelectedListener?.onNoteSelected(note.id)
                val intent = Intent(context, ViewNoteActivity::class.java)
                intent.putExtra(AppConstants.NOTE, note)
                intent.putExtra(AppConstants.BOOKMARKS, bookmarks)
                context.startActivity(intent)
            }

        }

        holder.imgBookmark.setOnClickListener {
            onBookmarkListener?.onBookmarkSelected(note.id)
            if (bookmarks.contains(note.id)){
                bookmarks.remove(note.id)
                Toast.makeText(context, "${note.title} removed from bookmarks", Toast.LENGTH_SHORT).show()
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24)
            } else{
                Toast.makeText(context, "${note.title} bookmarked", Toast.LENGTH_SHORT).show()
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_filled_blue_24)
            }
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

    fun setOnSelectedListener(listener: OnNoteSelectedListener) {
        onNoteSelectedListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.img_quiez)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtDescription: TextView = itemView.findViewById(R.id.txtDescription)
        val txtNoteRating: TextView = itemView.findViewById(R.id.txtNoteRating)
        val imgBookmark: ImageView = itemView.findViewById(R.id.imgNoteBookmark)
        val cardNote: CardView = itemView.findViewById(R.id.cardNote)
    }


    interface OnBookmarkListener {
        fun onBookmarkSelected(noteId: String)
    }

    interface OnNoteSelectedListener {
        fun onNoteSelected(noteId: String)
    }


}