package com.nema.eduup.discussions.chat

import java.util.*

object MessageType {
    const val TEXT = "TEXT"
    const val FILE = "FILE"
    const val IMAGE = "IMAGE"
    const val VIDEO = "VIDEO"
}

interface Message {
    val time: Date
    val senderId: String
    val recipientId: String
    val senderName: String
    val senderImageUrl: String
    val type: String
}