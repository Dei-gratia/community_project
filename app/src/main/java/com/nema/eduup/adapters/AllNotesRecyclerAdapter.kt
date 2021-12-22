package com.nema.eduup.adapters

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
import com.nema.eduup.activities.ViewNoteActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import kotlin.collections.ArrayList


class AllNotesRecyclerAdapter(private val context: Context, onBookmarkListener: OnBookmarkListener?, onNoteSelectedListener: OnNoteSelectedListener?) :
    RecyclerView.Adapter<AllNotesRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private var notes = ArrayList<Note>()
    private var bookmarks = ArrayList<Note>()
    private val TAG = AllNotesRecyclerAdapter::class.qualifiedName
    private var onBookmarkListener: OnBookmarkListener? = onBookmarkListener
    private var onNoteSelectedListener: OnNoteSelectedListener? = onNoteSelectedListener
    private var isBookmarks = false

    fun addNotes(newNotes: List<Note>) {
        notes = newNotes as ArrayList<Note>
        super.notifyDataSetChanged()
    }

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

    fun sort() {
        notes.reverse()
        notifyDataSetChanged()
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

    fun removeNote(rNote: Note){
        notes.remove(rNote)
    }

    fun addNote(newNote: Note) {
        println("Newnote id is ${newNote.id} \n ")
        Log.d("NoteIds", newNote.id)
        notes.add(newNote)
        notifyDataSetChanged()
    }

    fun addBookmarks(rbookmarks: ArrayList<Note>) {
        bookmarks.addAll(rbookmarks)
        notifyDataSetChanged()
    }

    fun addBookmark(newBookmark: Note) {
        bookmarks.add(newBookmark)
        notifyDataSetChanged()
    }

    fun removeBookmark(newBookmark: Note) {
        bookmarks.remove(newBookmark)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_note, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notes[position]
        Log.d(TAG, "Binding note " + note.id)
        holder.txtTitle.text = note.title
        holder.txtDescription.text = note.description
        val avgRating = note.avgRating?.roundTo(1)
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
            if (bookmarks.contains(note)){
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_filled_blue_24)
            }else {
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24)
            }
        }

        if (!note.reminders) {
            holder.cardNote.setOnClickListener {
                onNoteSelectedListener?.onNoteSelected(note)
                val intent = Intent(context, ViewNoteActivity::class.java)
                intent.putExtra(AppConstants.NOTE, note)
                intent.putExtra(AppConstants.BOOKMARKS, bookmarks)
                context.startActivity(intent)
            }

        }

        holder.imgBookmark.setOnClickListener {
            onBookmarkListener?.onBookmarkSelected(note)
            if (bookmarks.contains(note)){
                bookmarks.remove(note)
                Toast.makeText(context, "${note.title} removed from bookmarks", Toast.LENGTH_SHORT).show()
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24)
            } else{
                Toast.makeText(context, "${note.title} bookmarked", Toast.LENGTH_SHORT).show()
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_filled_blue_24)
            }
        }
    }

    fun Double.roundTo(n : Int) : Double {
        return "%.${n}f".format(this).toDouble()
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
        val imgIcon = itemView.findViewById<ImageView>(R.id.img_quiez)
        val txtTitle = itemView.findViewById<TextView>(R.id.txtTitle)
        val txtDescription = itemView.findViewById<TextView>(R.id.txtDescription)
        val txtNoteRating = itemView.findViewById<TextView>(R.id.txtNoteRating)
        val imgBookmark = itemView.findViewById<ImageView>(R.id.imgNoteBookmark)
        val cardNote = itemView.findViewById<CardView>(R.id.cardNote)
    }


    interface OnBookmarkListener {
        fun onBookmarkSelected(note: Note)
    }

    interface OnNoteSelectedListener {
        fun onNoteSelected(note: Note)
    }

    class NotesDiffCallback(private val oldList: List<Note>, private val newList: List<Note>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id === newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val (_, value, name) = oldList[oldPosition]
            val (_, value1, name1) = newList[newPosition]

            return name == name1 && value == value1
        }

        @Nullable
        override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
            return super.getChangePayload(oldPosition, newPosition)
        }
    }


}