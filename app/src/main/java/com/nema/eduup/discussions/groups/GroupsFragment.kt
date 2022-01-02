package com.nema.eduup.discussions.groups

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.databinding.FragmentGroupsBinding
import com.nema.eduup.auth.User
import com.nema.eduup.utils.AppConstants
import com.nema.eduup.utils.ConnectionManager
import com.nema.eduup.utils.GlideLoader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception

class GroupsFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentGroupsBinding
    private lateinit var newGroupView: View
    private lateinit var groupsListenerRegistration: ListenerRegistration
    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var adapter: GroupsRecyclerAdapter
    private lateinit var askStoragePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var requestStoragePermissionsResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var chooseGroupImageResultLauncher: ActivityResultLauncher<String>
    private lateinit var cvNewDiscussionGroup: CardView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selectedImageBytes: ByteArray
    private lateinit var imgGroupImage: ImageView
    private var groupProfileImageURL: String = ""
    private var selectedGroupImageFileUri: Uri? = null
    private var currentUser: User = User()
    private var userId = "-1"


    private val viewModel by lazy { ViewModelProvider(requireActivity())[GroupsFragmentViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sharedPreferences = activity?.getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE) as SharedPreferences
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
        }
        binding = FragmentGroupsBinding.inflate(layoutInflater, container, false)
        init()

        groupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupsRecyclerAdapter(requireContext())
        groupsRecyclerView.adapter = adapter
        adapter.getUserId(userId)
        loadGroups()

        requestStoragePermissionsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        showImageChooser()
                    } else {
                        Toast.makeText(requireContext(), resources.getString(R.string.read_store_permission_denied),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        askStoragePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: MutableMap<String, Boolean> ->
            if (!map.values.contains(false)){
                showImageChooser()
            } else {
                Toast.makeText(requireContext(), resources.getString(R.string.read_store_permission_denied),
                    Toast.LENGTH_LONG).show()
            }
        }

        chooseGroupImageResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { imageIri: Uri? ->
            if(imageIri != null) {
                try {
                    selectedGroupImageFileUri = imageIri
                    GlideLoader(requireContext()).loadImage(selectedGroupImageFileUri!!, imgGroupImage)
                    imgGroupImage.visibility = View.VISIBLE
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), resources.getString(R.string.image_selection_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }


        cvNewDiscussionGroup.setOnClickListener(this)
        return binding.root
    }

    private fun init() {
        cvNewDiscussionGroup = binding.cvNewGroup
        groupsRecyclerView = binding.groupsRecyclerView
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.cv_new_group -> {
                    addGroup()
                }
            }
        }
    }

    private fun checkGroupImage(name: String, securityMode: String) {
        if (selectedGroupImageFileUri != null) {
            val fileExtension = AppConstants.getFileExtension(requireActivity(), selectedGroupImageFileUri)
            if (fileExtension != null) {
                val storagePath =
                    AppConstants.GROUP_FILES + "/" + name + "/" + AppConstants.PROFILE_PHOTOS
                var selectedImageBmp: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(
                        context?.contentResolver,
                        selectedGroupImageFileUri
                    )

                } else {
                    val source = context?.contentResolver?.let {
                        ImageDecoder.createSource(
                            it,
                            selectedGroupImageFileUri!!
                        )
                    }
                    source?.let { ImageDecoder.decodeBitmap(it) }!!
                }
                val outputStream = ByteArrayOutputStream()
                selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                selectedImageBytes = outputStream.toByteArray()

                viewModel.uploadImage(selectedImageBytes, storagePath,{ progress ->

                }) {
                    groupProfileImageURL = it.toString()
                    createNewGroup(name, securityMode)
                }
            }
        }
        else {
            createNewGroup(name, securityMode)
        }
    }

    private fun createNewGroup(name: String, securityMode: String) {
        viewModel.createGroup(name, groupProfileImageURL, securityMode)
    }


    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showImageChooser()
            }
            else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s",
                        context?.applicationContext?.packageName
                    ))
                    requestStoragePermissionsResultLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    requestStoragePermissionsResultLauncher.launch(intent)
                }
            }
        }else {
            if ((ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                &&
                (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                showImageChooser()
            }else {
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                askStoragePermissions.launch(permissions)
            }
        }
    }

    private fun showImageChooser() {
        chooseGroupImageResultLauncher.launch("image/*")
    }

    private fun loadGroups() {
        viewModel.getGroups().observe(viewLifecycleOwner, Observer {
            adapter.setGroups(it)
        })
    }


    private fun addGroup() {
        newGroupView = View.inflate(
            requireContext(),
            R.layout.new_group,
            null
        )
        val etGroupName = newGroupView.findViewById<EditText>(R.id.et_group_name)
        imgGroupImage = newGroupView.findViewById(R.id.img_group_photo)
        val flGroupImage = newGroupView.findViewById<FrameLayout>(R.id.fl_group_image)
        val spinnerSecurityMode = newGroupView.findViewById<Spinner>(R.id.spinner_security_mode)
        val securityModes = arrayOf("Public","Private")
        val spinnerAdapter = ArrayAdapter(
            requireContext(), R.layout.spinner_item, securityModes)
        spinnerSecurityMode.adapter = spinnerAdapter

        flGroupImage.setOnClickListener {
            Toast.makeText(requireContext(), "add image" , Toast.LENGTH_SHORT).show()
            checkPermission()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Create New Discussion Group")
            .setView(newGroupView)
            .setPositiveButton("Create") { _, _ ->
                val groupName = etGroupName.text.toString()
                val securityMode = spinnerSecurityMode.selectedItem.toString()
                checkGroupImage(groupName, securityMode)
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }
}