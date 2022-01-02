package com.nema.eduup.roomDatabase

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity
data class Reminder(
    @PrimaryKey
    @get:Exclude
    var id: String = "",
    var name: String = "",
    var type: ReminderType = ReminderType.Other,
    var desc: String = "",
    var note: String? = null,
    var time: Long = 0,
    var opened: Boolean = false
): Parcelable {

    enum class ReminderType {
        Other,
        Study,
        Share
    }


}