package com.nema.eduup.models

import java.util.*

data class ImageMessage(
    val filePath: String,
    override val time: Date,
    override val senderId: String,
    override val recipientId: String,
    override val senderName: String,
    override val senderImageUrl: String,
    override val type: String = MessageType.IMAGE
): Message {
    constructor(): this("", Date(0), "", "", "", "")
}
