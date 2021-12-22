package com.nema.eduup.roomDatabase

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Note(
    @PrimaryKey
    @get:Exclude
    var id: String = "",
    var subject: String = "",
    var title: String = "",
    var description: String = "",
    var body: String = "",
    var fileUrl: String = "",
    var fileType: String = "",
    var level: String = "",
    var date: Long = 0,
    var avgRating: Double = 0.0,
    var numRating: Long = 0,
    var reminders: Boolean = false
): Parcelable