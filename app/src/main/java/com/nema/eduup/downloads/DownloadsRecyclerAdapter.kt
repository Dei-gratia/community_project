package com.nema.eduup.downloads

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.roomDatabase.Download
import com.nema.eduup.utils.AppConstants.openDownloadedAttachment
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.item_download, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = downloads[position]
        val dateFormat = SimpleDateFormat.getDateInstance().format(download.date)
        holder.tvFileName.text = download.fileName
        holder.tvDownloadDate.text = dateFormat
        holder.tvDownloadSize.text = download.size

        holder.cardDownload.setOnClickListener {
            openDownloadedAttachment(context, Uri.parse(download.localUri), download.mimeType)
        }
    }

    override fun getItemCount() = downloads.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileName: TextView = itemView.findViewById(R.id.tv_file_name)
        val tvDownloadDate: TextView = itemView.findViewById(R.id.tv_download_date)
        val tvDownloadSize: TextView = itemView.findViewById(R.id.tv_file_size)
        val cardDownload: CardView = itemView.findViewById(R.id.cardDownload)
    }
}