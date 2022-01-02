package com.nema.eduup.discussions.chat

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ChatChannel(
    val channelId: String = "",
    val userIds: MutableList<String>): Parcelable {
    constructor(): this("", mutableListOf())
}
