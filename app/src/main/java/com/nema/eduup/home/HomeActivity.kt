package com.nema.eduup.home

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.nema.eduup.BaseActivity
import com.nema.eduup.R
import com.nema.eduup.auth.AuthActivity
import com.nema.eduup.auth.SplashActivity
import com.nema.eduup.auth.User
import com.nema.eduup.uploadnote.UploadNoteActivity
import com.nema.eduup.databinding.ActivityHomeBinding
import com.nema.eduup.discussions.DiscussionsFragment
import com.nema.eduup.uploadquiz.UploadQuizActivity
import com.nema.eduup.roomDatabase.Note
import com.nema.eduup.utils.AppConstants


class HomeActivity : BaseActivity() {

    private val TAG = HomeActivity::class.qualifiedName
    private lateinit var binding            : ActivityHomeBinding
    private lateinit var sharedPreferences  : SharedPreferences
    private lateinit var toolbar            : MaterialToolbar
    private lateinit var navController      : NavController
    private lateinit var bottomNavView      : BottomNavigationView
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var startUploadActivityForResult: ActivityResultLauncher<Intent>
    private lateinit var fabNew             : FloatingActionButton
    lateinit var pullToRefreshHome          : SwipeRefreshLayout
    lateinit var appBarLayout               : AppBarLayout
    lateinit var etSearch                   : EditText
    private var currentUser : User = User()
    private var userId = "-1"


    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val viewModel by lazy { ViewModelProvider(this)[HomeActivityViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        sharedPreferences = getSharedPreferences(AppConstants.EDUUP_PREFERENCES, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(AppConstants.CURRENT_USER, "")
        if (!json.isNullOrBlank()){
            currentUser = Gson().fromJson(json, User::class.java)
            userId = currentUser.id
        }
        setContentView(binding.root)
        init()
        setupActionBar()
        initGoogleSignInClient()

        val navHostFrag = supportFragmentManager.findFragmentById(R.id.home_nav_host_frag) as NavHostFragment
        navController   = navHostFrag.navController
        val topLevelDestinations = setOf( R.id.fragmentHome, R.id.fragmentBrowse, R.id.fragmentDownloads,
            R.id.fragmentDiscussions, R.id.fragmentProfile)
        val appBarConfiguration = AppBarConfiguration(topLevelDestinations)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)
        visibilityNavElements(navController)

        if (currentUser.profileCompleted == 0) {
            navController.navigate(R.id.fragmentProfile)
        }

        pullToRefreshHome.viewTreeObserver.addOnGlobalLayoutListener(OnGlobalLayoutListener {
            val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
            val windowHeightMethod = InputMethodManager::class.java.getMethod("getInputMethodWindowVisibleHeight")
            val height = windowHeightMethod.invoke(imm) as Int
            if (height > 0) {
                bottomNavView.visibility = View.GONE
                fabNew.visibility = View.GONE
            }
            else {
                bottomNavView.visibility = View.VISIBLE
                val currentFragment = supportFragmentManager.fragments.last()
                if (currentFragment is HomeFragment || currentFragment is DiscussionsFragment) {
                    fabNew.visibility = View.VISIBLE
                }
            }
        })

        startUploadActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uploadNote = result.data?.getParcelableExtra<Note>(AppConstants.UPLOAD_NOTE)
                if (uploadNote != null) {
                    var noteSubject = "All Levels"
                    if (uploadNote.subject.isNotBlank()){
                        noteSubject = uploadNote.subject
                    }
                    //val collection = firestoreInstance.collection(uploadNote.level.lowercase()).document(noteSubject.lowercase()).collection("${uploadNote.level.lowercase()}${AppConstants.PUBLIC_NOTES}")
                    val document = firestoreInstance.collection(uploadNote.level.lowercase()).document(noteSubject.lowercase())
                        .collection("${uploadNote.level.lowercase()}${AppConstants.PUBLIC_NOTES}").document(uploadNote.id)
                    viewModel.addNoteToFirestore1(uploadNote, document){
                        Toast.makeText(this, "New note saved to firestore!", Toast.LENGTH_LONG).show()
                    }
                }

            }

        }

    }

    private fun init() {
        appBarLayout = binding.appBarLayout
        toolbar = binding.activityMainToolbar
        bottomNavView = binding.bottomNavView
        etSearch = binding.etSearch
        fabNew = binding.fabNew
        pullToRefreshHome = binding.refreshLayoutHome
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun setActionBarTitle(title: String?) {
        supportActionBar?.title = title
    }

    private fun initGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
                appSettings()
                return true
            }

            R.id.action_upload -> {
                upload()
                return true
            }

            R.id.action_bookmarks -> {
                loadBookmarks()
                return true
            }
            R.id.action_history -> {
                loadHistory()
                return true
            }

            R.id.action_sort  ->false
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun visibilityNavElements(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            appBarLayout.setExpanded(true)
            when (destination.id) {
                R.id.fragmentHome -> {
                    bottomNavView.visibility = View.VISIBLE
                    etSearch.visibility = View.GONE
                    fabNew.visibility = View.VISIBLE
                }
                R.id.fragmentBrowse -> {
                    bottomNavView.visibility = View.VISIBLE
                    etSearch.visibility = View.VISIBLE
                    fabNew.visibility = View.GONE
                }
                R.id.fragmentDiscussions -> {
                    fabNew.visibility = View.VISIBLE
                    etSearch.visibility = View.GONE
                    bottomNavView.visibility = View.VISIBLE
                }
                else -> {
                    fabNew.visibility = View.GONE
                    bottomNavView.visibility = View.VISIBLE
                    etSearch.visibility = View.GONE
                }

            }
        }
    }

    private fun loadBookmarks() {
        navController.navigate(R.id.fragmentBookmarks)
    }

    private fun loadHistory() {
        navController.navigate(R.id.fragmentHistory)
    }

    private fun uploadNote() {
        val uploadIntent = Intent(this, UploadNoteActivity::class.java)
        uploadIntent.putExtra(AppConstants.USER_LEVEL, currentUser.schoolLevel)
        startUploadActivityForResult.launch(uploadIntent)
    }

    private fun authActivity() {
        auth.signOut()
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun upload() {
        val options = arrayOf("Upload Notes", "Upload Quiz")

        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    uploadNote()
                }
                1 -> {
                    uploadQuiz()
                }
            }
        }

        builder.show()
    }

    private fun uploadQuiz() {
        val uploadIntent = Intent(this, UploadQuizActivity::class.java)
        uploadIntent.putExtra(AppConstants.USER_LEVEL, currentUser.schoolLevel)
        startUploadActivityForResult.launch(uploadIntent)
    }

    private fun appSettings() {
        val options = arrayOf("App Theme")
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.app_settings))
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    chooseThemeDialog()
                }
            }
        }

        builder.show()
    }

    private fun chooseThemeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_theme_text))
        val styles = arrayOf("Light","Dark","System default")
        val checkedItem = sharedPreferences.getInt(AppConstants.APP_THEME, 2)
        builder.setSingleChoiceItems(styles, checkedItem) { dialog, which ->

            when (which) {
                0 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    sharedPreferences.edit().putInt(AppConstants.APP_THEME, 0).apply()
                    delegate.applyDayNight()
                    dialog.dismiss()
                }
                1 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    delegate.applyDayNight()
                    sharedPreferences.edit().putInt(AppConstants.APP_THEME, 1).apply()
                    dialog.dismiss()
                }
                2 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    sharedPreferences.edit().putInt(AppConstants.APP_THEME, 2).apply()
                    delegate.applyDayNight()
                    dialog.dismiss()
                }

            }
        }

        val dialog = builder.create()
        dialog.show()
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

    private fun restart() {
        val intent = Intent(this, SplashActivity::class.java)
        this.startActivity(intent)
        finishAffinity()
    }

    private fun signOut(){
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to Logout")
            .setPositiveButton("Logout") { _, _ ->
                for (user in auth.currentUser!!.providerData) {
                    if (user.providerId == "password") {
                        authActivity()
                    } else {
                        googleSignInClient.signOut().addOnCompleteListener {
                            authActivity()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->

            }
            .create()
            .show()
    }

}