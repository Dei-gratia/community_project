package com.nema.eduup.viewnote

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Rating(
    var userName: String = "",
    var Date: Long = 0,
    var rateValue: Double = 0.0,
    var comment: String = "",
): Parcelable