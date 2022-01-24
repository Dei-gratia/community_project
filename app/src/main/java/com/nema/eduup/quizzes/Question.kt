package com.nema.eduup.quizzes

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Question(
    @PrimaryKey
    @get:Exclude
    var id: String = "",
    var question: String = "",
    var optionOne: String = "",
    var optionTwo: String = "",
    var optionThree: String = "",
    var optionFour: String = "",
    var correctOption: Int = 1
): Parcelable