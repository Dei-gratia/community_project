package com.nema.eduup.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.activities.ViewNoteActivity
import com.nema.eduup.roomDatabase.Download
import com.nema.eduup.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.*

class DownloadsRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<DownloadsRecyclerAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)
    private val downloads = ArrayList<Download>()
    private val TAG = DownloadsRecyclerAdapter::class.qualifiedName

    fun addDownloads(newDownloads: List<Download>) {
        downloads.addAll(newDownloads)
        notifyDataSetChanged()
    }

    fun clearDownloads() {
        this.downloads.clear()
    }

    fun addDownload(newDownload: Download) {
        downloads.add(newDownload)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_download, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = downloads[position]
        val dateFormat = SimpleDateFormat.getDateInstance().format(download.date)
        holder.tvFileNameame.text = download.fileName
        holder.tvDownloadDate.text = dateFormat
        holder.tvDownloadSize.text = download.size

        holder.cardDownload.setOnClickListener {
            val intent = Intent(context, ViewNoteActivity::class.java)
            intent.putExtra(AppConstants.NOTES, download)
            //context.startActivity(intent)
        }

    }

    override fun getItemCount() = downloads.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileNameame = itemView.findViewById<TextView>(R.id.tv_file_name)
        val tvDownloadDate = itemView.findViewById<TextView>(R.id.tv_download_date)
        val tvDownloadSize = itemView.findViewById<TextView>(R.id.tv_file_size)
        val cardDownload = itemView.findViewById<CardView>(R.id.cardDownload)
    }
}