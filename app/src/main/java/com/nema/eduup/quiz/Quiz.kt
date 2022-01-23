package com.nema.eduup.quiz

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Quiz(
    @PrimaryKey
    @get:Exclude
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var subject: String = "",
    var level: String = "",
    var totalQuestions: Int = 0,
    var duration: Int = 0,
    var date: Long = 0,
): Parcelable