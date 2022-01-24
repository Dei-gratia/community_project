package com.nema.eduup.viewnote

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.nema.eduup.BaseActivity
import com.nema.eduup.BuildConfig
import com.nema.eduup.R
import com.nema.eduup.auth.User
import com.nema.eduup.databinding.ActivityViewNoteBinding
import com.nema.eduup.roomDatabase.Download
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.AppConstants.fileTypeImage
import com.nema.eduup.utils.AppConstants.roundTo
import com.nema.eduup.utils.buildPdf
import com.nema.eduup.utils.getFiles
import com.nema.eduup.utils.proposalExists
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class ViewNoteActivity : BaseActivity(), View.OnClickListener, RatingBar.OnRatingBarChangeListener,
    View.OnLongClickListener {

    private val TAG = ViewNoteActivity::class.qualifiedName
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityViewNoteBinding
    private lateinit var toolbar: Toolbar
    private lateinit var noteId : String
    private lateinit var noteLevel: String
    private lateinit var tvNoteUploadDate: TextView
    private lateinit var tvNoteTitle: TextView
    private lateinit var tvNoteLevel: TextView
    private lateinit var tvNoteSubject: TextView
    private lateinit var tvNoteDescription: TextView
    private lateinit var tvNoteBody: TextView
    private lateinit var tvFileName: TextView
    private lateinit var tvDownloadFile: TextView
    private lateinit var tvRateNote: TextView
    private lateinit var tvNoteRating: TextView
    private lateinit var tvUserComment: TextView
    private lateinit var txtUserCommentName: TextView
    private lateinit var tvUserCommentDate: TextView
    private lateinit var tvEditReview: TextView
    private lateinit var txtDeleteReview: TextView
    private lateinit var rbUserRateValue: RatingBar
    private lateinit var userCommentLayout: ConstraintLayout
    private lateinit var clRatingsLayout: ConstraintLayout
    private lateinit var imgBookmark: ImageView
    private lateinit var imgShare: ImageView
    private lateinit var imgFileType: ImageView
    private lateinit var cvDownload: CardView
    private lateinit var downloadFileLayout: LinearLayout
    private lateinit var note: Note
    private lateinit var noteFileUrl: String
    private lateinit var ratingView: View
    private lateinit var userId: String
    private lateinit var userName: String
    private lateinit var noteReference: DocumentReference
    private lateinit var downloadedFileName: String
    private lateinit var downloadedFileId: String
    private lateinit var downloadedFileSize: String
    private lateinit var tvNumComments: TextView
    private lateinit var ratingsRecyclerView: RecyclerView
    private lateinit var ratingsAdapter: RatingsRecyclerAdapter
    private lateinit var firestoreRatingsListener: ListenerRegistration
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private var currentUser: User = User()
    private var noteRating = 0F
    private var givenRating = 0.0
    private var bookmarkList = ArrayList<String>()
    private var imgFileTypeId = 0
    private var numReviews = 0

    private val firestoreInstance by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(this)[ViewNoteActivityViewModel::class.java] }
    private val publicNoteDocumentReference by lazy { firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES)
        .collection(noteLevel)
        .document(noteId).collection(AppConstants.NOTE_RATINGS).document(userId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        binding = ActivityViewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
            userName = "${currentUser.firstNames} ${currentUser.familyName}"
        }
        note = intent.getParcelableExtra(AppConstants.NOTE)!!
        noteLevel = note.level
        noteId = note.id
        if (intent.hasExtra(AppConstants.BOOKMARKS)) {
            bookmarkList = intent.getStringArrayListExtra(AppConstants.BOOKMARKS)!!
        }


        noteFileUrl = note.fileUrl
        noteRating = note.avgRating.toFloat()
        setupActionBar()
        displayNoteDetails()

        registerReceiver(onComplete,  IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        ratingsAdapter = RatingsRecyclerAdapter(this)
        ratingsRecyclerView.layoutManager = LinearLayoutManager(this)
        ratingsRecyclerView.adapter = ratingsAdapter
        getUserRating()
        loadNoteRatings()

        requestStoragePermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        pdfGenerateOrDownload(note.title, noteFileUrl)
                    } else {
                        Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        askStoragePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: MutableMap<String, Boolean> ->
            if (!map.values.contains(false)){
                pdfGenerateOrDownload(note.title, noteFileUrl)
            } else {
                Toast.makeText(this, resources.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG).show()
            }
        }

        cvDownload.setOnClickListener(this)
        imgBookmark.setOnClickListener(this)
        imgShare.setOnClickListener(this)
        tvRateNote.setOnClickListener(this)
        tvEditReview.setOnClickListener(this)
        userCommentLayout.setOnLongClickListener(this)

    }

    private fun init() {
        toolbar = binding.toolbarViewNoteActivity
        tvNoteTitle = binding.tvNoteTitle
        tvNoteUploadDate = binding.tvUploadDate
        tvNoteLevel = binding.tvLevel
        tvNoteSubject = binding.tvSubject
        tvNoteDescription = binding.tvNoteDescription
        tvNoteBody = binding.tvNoteBody
        tvFileName = binding.txtFileName
        tvDownloadFile = binding.tvDownloadFile
        tvRateNote = binding.tvRateNote
        tvNoteRating = binding.tvNoteRating
        tvNumComments = binding.tvNumComments
        tvUserComment = binding.tvUserRatingComment
        tvUserCommentDate = binding.tvUserReviewDate
        tvEditReview = binding.tvEditReview
        rbUserRateValue = binding.userRatingValueBar
        ratingsRecyclerView = binding.noteCommentsRecyclerView
        imgBookmark = binding.imgBookmark
        imgFileType = binding.imgFileType
        imgShare = binding.imgShare
        cvDownload = binding.cvDownload
        downloadFileLayout = binding.downloadFileLayout
        userCommentLayout = binding.userRatingLayout
        clRatingsLayout = binding.clRatingsLayout
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24)
        }

        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun displayNoteDetails() {
        tvNoteTitle.text = note.title
        val formattedDate = DateUtils.getRelativeTimeSpanString(
            note.date,
            Calendar.getInstance().timeInMillis,
            DateUtils.MINUTE_IN_MILLIS
        )
        tvNoteUploadDate.text = formattedDate
        tvNoteLevel.text = note.level
        tvNoteSubject.text = note.subject
        tvNoteDescription.text = note.description
        tvNoteBody.text = note.body
        tvFileName.text = note.title
        tvNoteRating.text = note.avgRating.roundTo(1).toString()
        if (bookmarkList.contains(note.id)){
            imgBookmark.setImageResource(R.drawable.ic_bookmark_filled_blue_24)
        }else{
            imgBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24)
        }
        Log.e(TAG, "Setting file view")
        if (!note.fileUrl.isNullOrBlank()){
            Log.e(TAG, "file not null")
            tvFileName.text = "${note.title}.${note.fileType}"
            imgFileTypeId = fileTypeImage(note.fileType)
            if (imgFileTypeId != 0) {
                imgFileType.setImageResource(imgFileTypeId)
            }
        }
        else {
            tvFileName.text = "${note.title}.pdf"
            imgFileType.setImageResource(R.drawable.image_pdf)
            if (this.proposalExists(noteId, note.title)){
                tvDownloadFile.text = "open pdf"
            }
            else {
                tvDownloadFile.text = "Download"
            }
        }
    }



    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.cv_download -> {
                    checkStoragePermission()
                }
                R.id.img_bookmark -> {
                    toggleBookmarks()
                }
                R.id.tv_rate_note -> {
                    rateNote(0F, "")
                }
                R.id.tv_edit_review -> {
                    rateNote(rbUserRateValue.rating, tvUserComment.text.toString())
                }
                R.id.img_share -> {
                    checkPdf("share")
                }
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                pdfGenerateOrDownload(note.title, noteFileUrl)
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
                pdfGenerateOrDownload(note.title, noteFileUrl)
            }
            else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                askStoragePermissions.launch(permissions)
            }
        }
    }

    private fun pdfGenerateOrDownload(fname : String, url : String) {
        if (!note.fileUrl.isNullOrBlank()){
            downloadFile(fname, url)
        }
        else {
            generatePdf {
                checkPdf("open")
            }
        }
    }

    private fun downloadFile(fname : String, url : String){
        val toast = Toast.makeText(this, "Your download has started",Toast.LENGTH_LONG)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
        toast.show()
        this.registerReceiver(onComplete, IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(url)
        var fileName = "$fname.$fileExtension"
        downloadedFileName = fileName
        // fileName -> fileName with extension
        var file = File(getExternalFilesDir(null), "Dummy")
        val request = DownloadManager.Request(Uri.parse(url))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setTitle(fileName)
            .setDescription("Downloading $fileName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName)
        val downloadManager= getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadID = downloadManager.enqueue(request)
    }

    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                val downloadId = intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID, 0
                )
                downloadedFileId = downloadId.toString()
                openDownloadedAttachment(ctxt, downloadId)
            }
        }
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_SUBJECT, note.title)
        intent.putExtra(Intent.EXTRA_TEXT, note.description)
        startActivity(Intent.createChooser(intent, "Share Notes"))
    }

    @SuppressLint("Range")
    private fun openDownloadedAttachment(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        val cursor: Cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val downloadStatus: Int =
                cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val downloadLocalUri: String =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val downloadMimeType: String =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))

            downloadedFileSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).toString()
            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                Log.e(TAG, "on download $downloadLocalUri $downloadMimeType")
                storeDownload(downloadLocalUri, downloadMimeType)
                openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType)
            }
        }
        cursor.close()
    }

    private fun openDownloadedAttachment(context: Context, attachmentUri: Uri, attachmentMimeType: String) {
        var attachmentUri: Uri? = attachmentUri
        if (attachmentUri != null) {
            // Get Content Uri.
            if (ContentResolver.SCHEME_FILE == attachmentUri.scheme) {
                // FileUri - Convert it to contentUri.
                val file = File(attachmentUri.path)
                attachmentUri =
                    FileProvider.getUriForFile(this, "com.nema.eduup.provider", file)
            }
            val openAttachmentIntent = Intent(Intent.ACTION_VIEW)
            openAttachmentIntent.setDataAndType(attachmentUri, attachmentMimeType)
            openAttachmentIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.startActivity(openAttachmentIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun storeDownload(downloadLocalUri: String, downloadMimeType: String) {
        val download = Download(
            downloadedFileId,
            downloadedFileName,
            downloadMimeType,
            downloadLocalUri,
            downloadedFileSize,
            Calendar.getInstance().timeInMillis
        )

        viewModel.insertDownload(download)
    }

    private fun toggleBookmarks() {
        if (bookmarkList.contains(noteId)){
            imgBookmark.setImageResource(R.drawable.ic_bookmark_border_black_24)
            viewModel.removeBookmark(noteId)
            bookmarkList.remove(noteId)
        }else{
            imgBookmark.setImageResource(R.drawable.ic_bookmark_filled_blue_24)
            viewModel.addBookmark(noteId)
            bookmarkList.add(noteId)
        }
    }

    private fun rateNote(rateValue: Float, dComment: String) {
        ratingView = View.inflate(
            this,
            R.layout.rating,
            null
        )
        val ratingBar = ratingView.findViewById<RatingBar>(R.id.ratingBarNote)
        val etComment = ratingView.findViewById<EditText>(R.id.etComment)
        ratingBar.rating = rateValue
        etComment.setText(dComment)
        ratingBar.onRatingBarChangeListener = this
        AlertDialog.Builder(this)
            .setTitle("How did you find these notes")
            .setView(ratingView)
            .setPositiveButton("Rate") { _, _ ->
                givenRating = ratingBar.rating.toDouble()
                val comment = etComment.text.toString()
                val rating = Rating(userName, Calendar.getInstance().timeInMillis, givenRating, comment )
                val noteRef = firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES).collection(noteLevel).document(noteId)
                updateNoteRating( noteRef, rating)
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

    override fun onRatingChanged(ratingBar: RatingBar?, rating: Float, fromUser: Boolean) {}

    private fun updateNoteRating(noteRef: DocumentReference, rating: Rating) {
        if (rating.comment.isNotEmpty()) {
            userCommentLayout.visibility = View.VISIBLE
            tvUserComment.text = rating.comment
            rbUserRateValue.rating = rating.rateValue.toFloat()
        }
        viewModel.updateRating(noteRef, rating) {

        }
    }

    private fun getUserRating() {
        viewModel.getUserRating(publicNoteDocumentReference) {
            if (it != null) {
                val review = it
                userCommentLayout.visibility = View.VISIBLE
                tvRateNote.visibility = View.INVISIBLE
                tvUserComment.text = review.comment
                rbUserRateValue.rating = review.rateValue.toFloat()
            }
            else{
                userCommentLayout.visibility = View.GONE
                tvRateNote.visibility = View.VISIBLE
            }
        }
    }

    private fun loadNoteRatings() {
        val collectionReference = firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES)
            .collection(noteLevel).document(noteId).collection(AppConstants.NOTE_RATINGS)
        viewModel.getNoteRatings(collectionReference).observe(this, androidx.lifecycle.Observer { ratings ->
            ratingsAdapter.addRatings(ratings)

        })

        viewModel.numRating.observe(this, androidx.lifecycle.Observer {
            numReviews = it
            tvNumComments.text = "(${numReviews})"
        })
    }

    override fun onLongClick(v: View?): Boolean {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Remove review")
            .setPositiveButton("Remove") { _, _ ->
                deleteReview()
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()

        return true
    }

    private fun deleteReview() {
        userCommentLayout.visibility = View.GONE

        tvNumComments.text = "(${numReviews.minus(1)})"
        val  documentReference = firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES)
            .collection(noteLevel).document(noteId).collection(AppConstants.NOTE_RATINGS).document(userId)
        viewModel.deleteReview(documentReference)
    }

    private fun checkPdf(mode: String) {
        if (this.proposalExists(noteId, note.title)){
            checkMode(mode)
        }
        else {
            generatePdf {
                tvDownloadFile.text = "Open pdf"
                checkMode(mode)
            }
        }
    }


    private fun generatePdf(onComplete: () -> Unit) {
        this.buildPdf(note){
            if (it == 1){
                onComplete()
            }
            else {
                val toast = Toast.makeText(this, resources.getString(R.string.pdf_generation_failed), Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
        }
    }

    private fun checkMode(mode: String) {
        val pdfs = this.getFiles(note.id)
        for (pdf in pdfs) {
            if (pdf.name == "${note.title}.pdf") {
                if (mode == "share"){
                    sharePdf(pdf)
                }
                else if (mode == "open"){
                    openPdf(pdf)
                }
            }
        }
    }

    private fun openPdf(pdf: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uriFromFile(this, pdf),"application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(Intent.createChooser(intent, "Share Note as pdf"))

    }

    private fun sharePdf(pdf: File) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM,  uriFromFile(this,pdf))
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(Intent.createChooser(intent, "Share Note as pdf"))
    }

    private fun uriFromFile(context:Context, file:File):Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
        } else {
            Uri.fromFile(file)
        }
    }

}