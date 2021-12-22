package com.nema.eduup.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val firstNames: String = "",
    val familyName: String = "",
    val email: String = "",
    val mobile: Long = 0,
    val gender: String = "",
    val about: String = "",
    val schoolLevel: String = "",
    val school: String = "",
    val program: String = "",
    val imageUrl: String = "",
    val profileCompleted: Int = 0,
    val registrationTokens: MutableList<String> = mutableListOf()
): Parcelable