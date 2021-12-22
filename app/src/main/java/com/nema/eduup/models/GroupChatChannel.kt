package com.nema.eduup.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GroupChatChannel(
    val channelId: String = "",
    val groupName: String = "",
    val creatorId: String = "",
    val groupImageUrl: String = "",
    val about: String = "",
    val securityMode: String = "Public",
    val userIds: MutableList<String> = mutableListOf()
    ): Parcelable
