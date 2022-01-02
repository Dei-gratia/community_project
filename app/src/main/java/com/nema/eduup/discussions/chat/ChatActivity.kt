package com.nema.eduup.discussions.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.*
import java.net.URL
import java.util.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.nema.eduup.BaseActivity
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityChatBinding
import com.nema.eduup.auth.User
import com.nema.eduup.discussions.people.PeopleFragment
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.GlideLoader
import java.io.ByteArrayOutputStream
import java.lang.Exception


class ChatActivity : BaseActivity(), View.OnClickListener{

    private val TAG = PeopleFragment::class.qualifiedName
    private lateinit var binding: ActivityChatBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messagesRecyclerAdapter: MessagesRecyclerAdapter
    private lateinit var etTextMessage: EditText
    private lateinit var fabAttachment: FloatingActionButton
    private lateinit var imgSendMessage: ImageView
    private lateinit var toolbar: Toolbar
    private lateinit var otherUserId: String
    private lateinit var otherUserName: String
    private lateinit var otherUserImageUrl: String
    private lateinit var txtOtherUserName: TextView
    private lateinit var txtDate: TextView
    private lateinit var imgOtherUserProfileImage: ImageView
    private lateinit var channelId: String
    private lateinit var currentUserName: String
    private lateinit var selectFileForResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var messagesCollectionRef: CollectionReference
    private var groupMemberIds = mutableListOf<String>()
    private var currentUser: User = User()
    private var selectedFileUri: Uri? = null
    private var selectedFileType: String = "text"
    private var isGroupChat: Boolean = false
    private var currentUserId = "-1"
    private var currentUserImageUrl = ""

    private val firestoreInstance by lazy { FirebaseFirestore.getInstance() }
    private val chatChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.CHAT_CHANNELS) }
    private val groupChannelsCollectionRef by lazy { firestoreInstance.collection(AppConstants.GROUP_CHAT_CHANNELS) }
    private val viewModel by lazy { ViewModelProvider(this)[ChatActivityViewModel::class.java] }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = ActivityChatBinding.inflate(layoutInflater)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            currentUserId = currentUser.id
            currentUserName = "${currentUser.firstNames} ${currentUser.familyName}"
            currentUserImageUrl = currentUser.imageUrl
        }
        setContentView(binding.root)
        if(intent.hasExtra(AppConstants.USER_ID)){
            otherUserId = intent.getStringExtra(AppConstants.USER_ID)!!
        }
        if (intent.hasExtra(AppConstants.USER_NAME)){
            otherUserName = intent.getStringExtra(AppConstants.USER_NAME)!!
        }
        if (intent.hasExtra(AppConstants.USER_IMAGE_URL)){
            otherUserImageUrl = intent.getStringExtra(AppConstants.USER_IMAGE_URL)!!
        }

        if(intent.hasExtra(AppConstants.USER_IDS)){
            groupMemberIds = intent.getStringArrayListExtra(AppConstants.USER_IDS)!!
            isGroupChat = true
            channelId = otherUserId
            messagesCollectionRef = groupChannelsCollectionRef.document(channelId).collection(AppConstants.MESSAGES)
            if (!groupMemberIds.contains(currentUserId)){
                viewModel.addUserToGroup(channelId){

                }
            }
            loadMessages()
        }
        else{
            viewModel.getOrCreateChatChannel(otherUserId){ id ->
                channelId = id
                messagesCollectionRef = chatChannelsCollectionRef.document(channelId).collection(AppConstants.MESSAGES)
                loadMessages()
            }

        }
        init()
        setupActionBar()
        txtOtherUserName.text = otherUserName
        if (otherUserImageUrl.isNotBlank()){
            GlideLoader(this).loadImage(URL(otherUserImageUrl), imgOtherUserProfileImage)
        }

        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerAdapter = MessagesRecyclerAdapter(this)
        messagesRecyclerView.adapter = messagesRecyclerAdapter
        messagesRecyclerAdapter.getUserId(currentUserId)
        if (isGroupChat) {
            messagesRecyclerAdapter.isGroup(true)
        }





        messagesRecyclerView.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                messagesRecyclerView.postDelayed(Runnable {
                    messagesRecyclerView.adapter?.itemCount?.minus(1)?.let {
                        if (it > 0) {
                            messagesRecyclerView.smoothScrollToPosition(
                                it
                            )
                        }
                    }
                }, 100)
            }
        })


        fabAttachment.setOnClickListener(this)
        imgSendMessage.setOnClickListener(this)

        messagesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = messagesRecyclerView.layoutManager as LinearLayoutManager
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                messagesRecyclerAdapter.setDate(lastVisiblePosition, txtDate)

            }
        })

        requestStoragePermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
            if (result.resultCode == RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        showFileChooser()
                    } else {
                        Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        askStoragePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: MutableMap<String, Boolean> ->
            if (!map.values.contains(false)){
                showFileChooser()
            } else {
                Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG).show()
            }
        }

        selectFileForResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedFileUri = data?.data
                if (selectedFileUri != null) {
                    val file = DocumentFile.fromSingleUri(this, selectedFileUri!!)
                    val storagePath = AppConstants.USER_FILES + "/" + currentUserId + "/" + AppConstants.CHAT_FILES + "/" + file?.name
                    selectedFileType = AppConstants.getFileExtension(this, selectedFileUri).toString()
                    val mimeImages = arrayOf("jpeg", "png", "jpg")
                    if(mimeImages.contains(selectedFileType )) {
                        val selectedImageBmp = MediaStore.Images.Media.getBitmap(contentResolver, selectedFileUri)
                        val outputStream = ByteArrayOutputStream()

                        selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        val selectedImageBytes = outputStream.toByteArray()
                        viewModel.uploadImage(selectedImageBytes, storagePath,{ progress ->

                        }){
                            sendImageMessage(it.toString())
                        }
                    } else {
                        viewModel.uploadFile(selectedFileUri, selectedFileType, storagePath){
                            sendFileMessage(it.toString())
                        }
                    }

                }
            }
        }

    }

    private fun init() {
        messagesRecyclerView = binding.chatRecyclerView
        etTextMessage = binding.etTextMessage
        fabAttachment = binding.fabSendAttachment
        imgSendMessage = binding.imgSendMessage
        toolbar = binding.toolbarChatActivity
        txtOtherUserName = binding.txtOtherUserName
        txtDate = binding.txtDate
        imgOtherUserProfileImage = binding.imgOtherUserProfilePicture
    }

    private fun loadMessages() {
        viewModel.getChannelMessages(messagesCollectionRef).observe(this, androidx.lifecycle.Observer { messages ->
            messagesRecyclerAdapter.addMessages(messages)
            messagesRecyclerView.post {
                messagesRecyclerView.layoutManager?.scrollToPosition((messagesRecyclerView.adapter?.itemCount
                    ?: 1) -1)
            }
        })
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24)
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when(view.id) {
                R.id.imgSendMessage -> {
                    sendTextMessage()
                }
                R.id.fabSendAttachment -> {
                    checkPermission()
                }
            }
        }
    }

    private fun sendTextMessage() {
        val messageToSend = TextMessage(
            etTextMessage.text.toString(),
            Calendar.getInstance().time,
            currentUserId,
            otherUserId,
            currentUserName,
            currentUserImageUrl
        )
        etTextMessage.setText("")
        viewModel.sendMessage(messageToSend,messagesCollectionRef){

        }
    }


    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showFileChooser()
            }
            else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s",
                        applicationContext?.packageName
                    ))
                    requestStoragePermissionsResultLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    requestStoragePermissionsResultLauncher.launch(intent)
                }
            }
        }else {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                showFileChooser()
            }else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                askStoragePermissions.launch(permissions)
            }
        }
    }

    private fun showFileChooser(){
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        selectFileForResultLauncher.launch(Intent.createChooser(intent, "Select a file"))
    }

    private fun sendFileMessage(fileUrl: String) {
        val messageToSend = FileMessage(
            fileUrl,
            Calendar.getInstance().time,
            currentUserId,
            otherUserId,
            currentUserName,
            currentUserImageUrl
        )
        viewModel.sendMessage(messageToSend, messagesCollectionRef){
        }
    }

    private fun sendImageMessage(fileUrl: String) {
        val messageToSend = ImageMessage(
            fileUrl,
            Calendar.getInstance().time,
            currentUserId,
            otherUserId,
            currentUserName,
            currentUserImageUrl
        )
        viewModel.sendMessage(messageToSend, messagesCollectionRef){

        }
    }

}