package com.nema.eduup.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.nema.eduup.databinding.ActivityPractiseQuestionsBinding

class PractiseQuestionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPractiseQuestionsBinding
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPractiseQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setupActionBar()
    }

    private fun init() {
        toolbar = binding.activityQuestionsToolbar
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}