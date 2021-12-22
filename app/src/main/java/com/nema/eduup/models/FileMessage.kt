package com.nema.eduup.models

import java.util.*

data class FileMessage(
    val filePath: String,
    override val time: Date,
    override val senderId: String,
    override val recipientId: String,
    override val senderName: String,
    override val senderImageUrl: String,
    override val type: String = MessageType.FILE
): Message {
    constructor(): this("", Date(0), "", "", "", "")
}
