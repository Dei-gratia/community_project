package com.nema.eduup.roomDatabase

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Download(
    @PrimaryKey
    @get:Exclude
    var id: String = "",
    var fileName: String = "",
    var mimeType: String = "",
    var localUri: String = "",
    var size: String = "",
    var date: Long = 0
): Parcelable