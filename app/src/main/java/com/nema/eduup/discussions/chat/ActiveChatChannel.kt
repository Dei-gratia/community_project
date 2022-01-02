package com.nema.eduup.discussions.chat

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class ActiveChatChannel(
    var channelId: String = "",
    var otherUserName: String = "",
    var otherUserImageUrl: String = "",
    var otherUserId: String = "",
    var newestMessage: String = "",
    var newestMessageDate: Date = Date(0),
    var newestMessageSenderId: String = "",
    var unreadMessagesCount: Long = 0,
    val isGroup: Boolean = false
): Parcelable