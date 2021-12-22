package com.nema.eduup.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.URLUtil
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nema.eduup.R
import com.nema.eduup.models.*
import com.nema.eduup.utils.GlideLoader
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

import kotlin.collections.ArrayList


class MessagesRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TAG = MessagesRecyclerAdapter::class.qualifiedName
    private val layoutInflater = LayoutInflater.from(context)
    private val messages: ArrayList<Message> = ArrayList()
    //private var onMessagesListerner: OnMessageListener = onMessageListerner
    private var currentUserId = "-1"
    private var isGroupMessages = false
    private val textMessage = 1
    private val fileMessage = 2
    private val imageMessage = 3
    private val videoMessage = 4

    fun addMessages(newMessages: List<Message>) {
        val oldSize = this.messages.size
        this.messages.addAll(newMessages)
        notifyItemRangeInserted(oldSize, newMessages.size)
        /*Log.d("List old and new" , "$messages $newMessages" )
        messages.addAll(newMessages)
        notifyDataSetChanged()
        Log.d("List old and new" , "$messages $newMessages" )*/

    }


    fun addMessage(newMessage: Message) {
        /*messages.add(newMessage)
        notifyDataSetChanged()*/
    }

    fun getUserId(userId: String) {
        currentUserId = userId
    }

    fun isGroup(isGroupMsg: Boolean){
        isGroupMessages = isGroupMsg
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
         when(viewType) {
            textMessage -> {
                val itemView = layoutInflater.inflate(R.layout.item_text_message, parent, false)
                return TextMessageViewHolder(itemView)
            }
            imageMessage -> {
                val itemView = layoutInflater.inflate(R.layout.item_image_massage, parent, false)
                return ImageMessageViewHolder(itemView)
            }
            videoMessage -> {
                val itemView = layoutInflater.inflate(R.layout.item_image_massage, parent, false)
                return ImageMessageViewHolder(itemView)
            }
            else -> {
                val itemView = layoutInflater.inflate(R.layout.item_file_massage, parent, false)
                return FileMessageViewHolder(itemView)
            }
        }

    }

    fun setDate(topPosition: Int, txtDate: TextView){
        if (topPosition > 1){
            val date = messages[topPosition].time
            txtDate.text = getDate(date)
        }

    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        holder.setIsRecyclable(false)
        if (holder is TextMessageViewHolder && message is TextMessage) {
            holder.txtMessageText.text = message.text
            holder.txtMessageTime.text = message.time.toString("HH:mm")

            if (isGroupMessages){
                holder.txtSenderName.visibility = View.VISIBLE
                if (message.senderId == currentUserId){
                    holder.txtSenderName.text = "You"
                }else {
                    holder.txtSenderName.text = message.senderName
                }
            }
            if (message.senderId != currentUserId){
                holder.rlMessage.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rect_round_ascent_color
                    )
                    val lParams = FrameLayout.LayoutParams(WRAP_CONTENT , WRAP_CONTENT , Gravity.START)
                    this.layoutParams = lParams
                   setOnClickListener {
                        Toast.makeText(context, " Message", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                holder.rlMessage.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rect_round_primary_color
                    )
                    val lParams =
                        FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.END)
                    this.layoutParams = lParams
                    setOnClickListener {
                        Toast.makeText(context, " Message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            holder.rlMessage.setOnLongClickListener {
                val options = arrayOf("Forward", "Delete", "Star", "Reply")

                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                    // the user clicked on colors[which]
                    when(which) {
                        0 -> {
                            Toast.makeText(context, "Ckicked on ${options[0]}", Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            Toast.makeText(context, "Ckicked on ${options[1]}", Toast.LENGTH_SHORT).show()
                        }
                        2 -> {
                            Toast.makeText(context, "Ckicked on ${options[2]}", Toast.LENGTH_SHORT).show()
                        }
                        3 -> {
                            Toast.makeText(context, "Ckicked on ${options[3]}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })

                builder.show()
                return@setOnLongClickListener true
            }
        }

        else if (holder is ImageMessageViewHolder && message is ImageMessage) {
            GlideLoader(context).loadImage(URL(message.filePath), holder.imgMessageFile)
            holder.txtMessageTime.text = message.time.toString("HH:mm")

            if (isGroupMessages){
                holder.txtSenderName.visibility = View.VISIBLE
                if (message.senderId == currentUserId){
                    holder.txtSenderName.text = "You"
                }else {
                    holder.txtSenderName.text = message.senderName
                }
            }
            if (message.senderId != currentUserId){
                holder.rlMessage.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rect_round_ascent_color
                    )
                    val lParams = FrameLayout.LayoutParams(WRAP_CONTENT , WRAP_CONTENT , Gravity.START)
                    this.layoutParams = lParams
                    setOnClickListener {
                        Toast.makeText(context, " Message", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                holder.rlMessage.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rect_round_primary_color
                    )
                    val lParams =
                        FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.END)
                    this.layoutParams = lParams
                    setOnClickListener {
                        Toast.makeText(context, " Message", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            holder.imgMessageFile.setOnClickListener {
                try {
                    val url = URL(message.filePath)
                    val uri = Uri.parse(url.toURI().toString())
                    showPhoto(uri)
                } catch (e1: MalformedURLException) {
                    e1.printStackTrace()
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }

            }

        }

        else if (holder is FileMessageViewHolder && message is FileMessage) {
            //GlideLoader(context).loadUserPicture(URL(message.filePath), holder.imgMessageFile)
            holder.txtFileName.text = getFileNameFromURL(message.filePath, 1)
            val fileExtension = getFileNameFromURL(message.filePath, 2)
            holder.imgMessageFileType.setImageResource(R.drawable.ic_attach_file_24)
            holder.txtMessageTime.text = message.time.toString("HH:mm")
            if (isGroupMessages){
                holder.txtSenderName.visibility = View.VISIBLE
                if (message.senderId == currentUserId){
                    holder.txtSenderName.text = "You"
                }else {
                    holder.txtSenderName.text = message.senderName
                }
            }

            if (message.senderId != currentUserId){
                holder.rlMessage.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rect_round_ascent_color
                    )
                    val lParams = FrameLayout.LayoutParams(WRAP_CONTENT , WRAP_CONTENT , Gravity.START)
                    this.layoutParams = lParams
                    setOnClickListener {
                        Toast.makeText(context, " Message", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                holder.rlMessage.apply {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.rect_round_primary_color
                    )
                    val lParams =
                        FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.END)
                    this.layoutParams = lParams
                    setOnClickListener {
                        Toast.makeText(context, " Message", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            holder.rlMessage.setOnClickListener {
                try {
                    val url = URL(message.filePath)
                    val uri = Uri.parse(url.toURI().toString())
                    if (fileExtension != null) {
                        showFile(uri, fileExtension)
                    }
                } catch (e1: MalformedURLException) {
                    e1.printStackTrace()
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }

            }
            holder.rlMessage.setOnLongClickListener {
                val options = arrayOf("Forward", "Delete", "Star", "Reply")

                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setTitle("Pick a color")
                builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
                    // the user clicked on colors[which]
                    when(which) {
                        0 -> {
                            Toast.makeText(context, "Ckicked on ${options[0]}", Toast.LENGTH_SHORT).show()
                        }
                        1 -> {

                            Toast.makeText(context, "Ckicked on ${options[1]}", Toast.LENGTH_SHORT).show()
                        }
                        2 -> {
                            Toast.makeText(context, "Ckicked on ${options[2]}", Toast.LENGTH_SHORT).show()
                        }
                        3 -> {
                            Toast.makeText(context, "Ckicked on ${options[3]}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })

                builder.show()
                return@setOnLongClickListener true
            }

        }

    }

    private fun starMessage(message: Message) {

    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            MessageType.TEXT -> {
                textMessage
            }
            MessageType.IMAGE -> {
                imageMessage
            }
            MessageType.VIDEO -> {
                videoMessage
            }
            else ->
                fileMessage
        }
    }

    override fun getItemCount() = messages.size

    fun setMessagesListener(listener: OnMessageListener) {
        //onMessagesListerner = listener
    }

    inner class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtMessageText = itemView.findViewById<TextView>(R.id.txtMessageText)
        val txtMessageTime = itemView.findViewById<TextView>(R.id.txtMessageTime)
        val rlMessage = itemView.findViewById<RelativeLayout>(R.id.rlMessage)
        val txtSenderName = itemView.findViewById<TextView>(R.id.txtSenderName)
    }

    inner class ImageMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSenderName = itemView.findViewById<TextView>(R.id.txtSenderName)
        val imgMessageFile = itemView.findViewById<ImageView>(R.id.imgMessageImage)
        val txtMessageTime = itemView.findViewById<TextView>(R.id.txtMessageTime)
        val rlMessage = itemView.findViewById<RelativeLayout>(R.id.rlMessage)
    }

    inner class FileMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMessageFileType = itemView.findViewById<ImageView>(R.id.imgMessageFileType)
        val txtFileName = itemView.findViewById<TextView>(R.id.tv_file_name)
        val txtMessageTime = itemView.findViewById<TextView>(R.id.txtMessageTime)
        val txtSenderName = itemView.findViewById<TextView>(R.id.txtSenderName)
        val rlMessage = itemView.findViewById<RelativeLayout>(R.id.rlMessage)
    }

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun getDate(gDate: Date): String {
        if (DateUtils.isToday(gDate.time)) {
            return "Today"
        }else if (isYesterday(gDate)){
            return "Yesterday"
        } else {
            return  SimpleDateFormat.getDateInstance().format(gDate).toString()
        }
    }

    fun isYesterday(d: Date): Boolean {
        return DateUtils.isToday(d.time + DateUtils.DAY_IN_MILLIS)
    }

    fun showPhoto(photoUri: Uri?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(photoUri, "image/*")
        context.startActivity(intent)
    }

    fun showFile(fileUri: Uri?, ext: String) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(fileUri, "application/$ext")
        context.startActivity(intent)
    }

    fun getFileNameFromURL(url: String?, mode: Int): String? {
        var fileNameWithExtension: String? = null
        var fileExtension: String? = null
        if (URLUtil.isValidUrl(url)) {
            fileNameWithExtension = URLUtil.guessFileName(url, null, null)
            if (fileNameWithExtension != null && !fileNameWithExtension.isEmpty()) {
                val f = fileNameWithExtension.split(".").toTypedArray()
                if (f.isNotEmpty() and (f.size > 1)) {
                    fileExtension = f[1]
                }
            }
        }
        return if (mode == 1) {
            fileNameWithExtension
        }else {
            fileExtension
        }

    }

    interface OnMessageListener {
        fun onMessage(message: Message, mode: Int)
    }
}