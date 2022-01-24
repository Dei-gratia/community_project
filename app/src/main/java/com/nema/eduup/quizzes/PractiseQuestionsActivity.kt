package com.nema.eduup.quizzes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityPractiseQuestionsBinding

class PractiseQuestionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPractiseQuestionsBinding
    private lateinit var toolbar: Toolbar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var navController      : NavController
    lateinit var tvToolbarTitle: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPractiseQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setupActionBar()
        val navHostFrag = supportFragmentManager.findFragmentById(R.id.quizzes_nav_host_frag) as NavHostFragment
        navController   = navHostFrag.navController
        val topLevelDestinations = setOf( R.id.fragmentQuizQuestions, R.id.fragmentQuizResult)
        val appBarConfiguration = AppBarConfiguration(topLevelDestinations)
        toolbar.setupWithNavController(navController, appBarConfiguration)

    }

    private fun init() {
        toolbar = binding.activityQuestionsToolbar
        tvToolbarTitle = binding.tvToolbarTitle
        swipeRefreshLayout = binding.refreshLayoutQuizzes
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_quizzes, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort  ->false
            R.id.action_upload_quiz -> false
            else -> super.onOptionsItemSelected(item)
        }
    }

}