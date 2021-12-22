package com.nema.eduup.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.AppBarLayout
import com.nema.eduup.utils.AppConstants
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.R
import com.nema.eduup.activities.viewmodels.HomeFragmentViewModel
import com.nema.eduup.databinding.ActivityMainBinding
import com.nema.eduup.firebase.FirestoreUtil
import com.nema.eduup.models.User
import com.nema.eduup.roomDatabase.Note
import java.util.*

class MainActivity : BaseActivity() {

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var binding            : ActivityMainBinding
    private lateinit var sharedPreferences  : SharedPreferences
    private lateinit var toolbar            : MaterialToolbar
    private lateinit var navController      : NavController
    private lateinit var bottomNavView      : BottomNavigationView
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var startUploadActivityForResult: ActivityResultLauncher<Intent>
    private lateinit var fabNew: FloatingActionButton
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbLightTheme: RadioButton
    private lateinit var rbDarkTheme : RadioButton
    private lateinit var settingsView: View
    lateinit var appBarLayout: AppBarLayout
    lateinit var etSearch   : EditText
    private var currentUser : User = User()
    private var userId      = "-1"

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(this)[HomeFragmentViewModel::class.java] }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE)
        setContentView(binding.root)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
        }
        init()
        setupActionBar()


        val navHostFrag = supportFragmentManager.findFragmentById(R.id.nav_host_frag) as NavHostFragment
        navController   = navHostFrag.navController
        val topLevelDestinations = setOf(R.id.fragmentHome, R.id.fragmentDashboard ,R.id.fragmentBrowse, R.id.fragmentDownloads, R.id.fragmentDiscussions, R.id.fragmentProfile)
        val appBarConfiguration = AppBarConfiguration(topLevelDestinations)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)

        if (viewModel.isNewlyCreated && savedInstanceState != null)
            viewModel.restoreState(savedInstanceState)
        viewModel.isNewlyCreated = false

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.clientId))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        visibilityNavElements(navController)

        startUploadActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                //  you will get result here in result.data
                val uploadNote = result.data?.getParcelableExtra<Note>(AppConstants.UPLOAD_NOTE)
                if (uploadNote != null) {
                    val collection = firestoreInstance.collection(AppConstants.NOTES).document(AppConstants.PUBLIC_NOTES).collection(uploadNote.level)

                    FirestoreUtil.addNoteToFirestore(uploadNote, collection){
                        Toast.makeText(this, "New note saved to firestore!", Toast.LENGTH_LONG).show()
                    }
                }

            }

        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveState(outState)
    }

    private fun init() {
        appBarLayout = binding.appBarLayout
        toolbar = binding.activityMainToolbar
        bottomNavView = binding.bottomNavView
        etSearch = binding.etSearch
        fabNew = binding.fabNew
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = "Home"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                signOut()
                return true
            }

            R.id.action_settings -> {
                toggleTheme()
                return true
            }

            R.id.action_upload -> {
                uploadNote()
                return true
            }

            R.id.action_bookmarks -> {
                loadBookmarks()
                return true
            }
            R.id.action_history -> { loadHistory()
               return true
            }

            R.id.action_sort  ->false
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadBookmarks() {
       navController.navigate(R.id.fragmentBookmarks)
    }

    private fun loadHistory() {
        navController.navigate(R.id.fragmentHistory)
    }

    private fun signOut(){
        for (user in FirebaseAuth.getInstance().currentUser!!.providerData) {
            if (user.providerId == "password") {
                loginActivity()
            } else {
                googleSignInClient.signOut().addOnCompleteListener {
                    loginActivity()
                }
            }
        }
    }

    private fun loginActivity() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun uploadNote() {
        val uploadIntent = Intent(this, UploadNoteActivity::class.java)
        uploadIntent.putExtra(AppConstants.USER_LEVEL, currentUser.schoolLevel)
        Log.d("Level in main ", currentUser.schoolLevel)
        startUploadActivityForResult.launch(uploadIntent)
    }

    private fun visibilityNavElements(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fragmentPeople, R.id.fragmentBookmarks, R.id.fragmentHistory -> {
                    bottomNavView.visibility = View.GONE
                    etSearch.visibility = View.GONE
                    fabNew.visibility = View.INVISIBLE
                    appBarLayout.setExpanded(true)
                }

                R.id.fragmentHome, R.id.fragmentBrowse -> {
                    bottomNavView.visibility = View.VISIBLE
                    fabNew.visibility = View.INVISIBLE
                    etSearch.visibility = View.VISIBLE
                    appBarLayout.setExpanded(true)
                }
                R.id.fragmentDashboard, R.id.fragmentDiscussions-> {
                    bottomNavView.visibility = View.VISIBLE
                    fabNew.visibility = View.VISIBLE
                    etSearch.visibility = View.GONE
                    appBarLayout.setExpanded(true)
                }
                else -> {
                    fabNew.visibility = View.INVISIBLE
                    appBarLayout.setExpanded(true)
                    bottomNavView.visibility = View.VISIBLE
                    etSearch.visibility = View.GONE
                }

            }
        }
    }

    private fun toggleTheme() {
        settingsView = View.inflate(
            this,
            R.layout.settings_layout,
            null
        )

        radioGroup = settingsView.findViewById(R.id.group_radio)
        rbLightTheme = settingsView.findViewById(R.id.radio_light_theme)
        rbDarkTheme = settingsView.findViewById(R.id.radio_dark_theme)
        var theme = "1"
        AlertDialog.Builder(this)
            .setTitle("Select AppTheme")
            .setView(settingsView)
            .setPositiveButton("OK") { _, _ ->
                if (rbLightTheme.isChecked) {
                    showLongMassage("Dark theme")
                    theme = "1"
                }

                if (rbDarkTheme.isChecked) {
                    showLongMassage("Dark theme")
                    theme = "2"
                }
                setTheme(theme)
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

    private fun  setTheme(theme: String) {
        AlertDialog.Builder(this)
            .setTitle("Restart")
            .setMessage("App will restart to apply changes")
            .setPositiveButton("OK") { _, _ ->
                sharedPreferences.edit().putString(AppConstants.APP_THEME, theme).apply()
                restart()
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

    fun restart() {
        val intent = Intent(this, SplashActivity::class.java)
        this.startActivity(intent)
        finishAffinity()
    }


}