package com.nema.eduup.utils

import android.content.Context
import android.graphics.*
import android.text.format.DateFormat
import android.util.Log
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.nema.eduup.roomDatabase.Note
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.DocumentException
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPageEventHelper

private val TAG = "FileUtils"

fun Context.buildPdf(note: Note, onComplete: (Int) -> Unit) {
    try {
        val fileName = "${note.title}.pdf"
        val directory = this.getFolder(note.id)
        val file = File(directory, fileName)
        val output = FileOutputStream(file)
        val document = Document()
        PdfWriter.getInstance(document, output)
        document.open()
        document.pageSize = PageSize.LETTER
        val footer = HeaderFooter()
        val calendar = Calendar.getInstance()
        val dateFormat = DateFormat.getDateFormat(this)
        calendar.timeInMillis = note.date
        val dateCreated = "Date created: ${dateFormat.format(calendar.time)}"
        val dateFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC, BaseColor.DARK_GRAY)
        val dateText = Chunk(dateCreated, dateFont)
        val pDate = Paragraph(dateText)
        pDate.alignment = Paragraph.ALIGN_RIGHT
        document.add(pDate)

        val textFont = Font(Font.FontFamily.HELVETICA, 12f, Font.ITALIC, BaseColor.DARK_GRAY)
        val levelText = Chunk("${note.level}    ", textFont)
        val subjectText = Chunk(note.subject, textFont)
        val pLevelAndSubject = Paragraph()
        pLevelAndSubject.add(levelText)
        pLevelAndSubject.add(subjectText)
        pLevelAndSubject.alignment = Paragraph.ALIGN_LEFT
        document.add(pLevelAndSubject)

        val titleFont = Font(Font.FontFamily.HELVETICA, 13f, Font.BOLD, BaseColor.BLACK)
        val titleText = Chunk(note.title, titleFont)
        val pTitle = Paragraph(titleText)
        pTitle.alignment = Paragraph.ALIGN_CENTER
        document.add(pTitle)
        document.add(Chunk.NEWLINE)

        val bodyFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK)
        val bodyText = Chunk(note.body, bodyFont)
        val pBody = Paragraph(bodyText)
        pBody.alignment = Paragraph.ALIGN_LEFT
        document.add(pBody)

        document.close()
        onComplete(1)

    } catch (e: IOException) {
        Log.e(TAG, "Note write error", e)
        onComplete(0)
    } catch (e: DocumentException) {
        Log.e(TAG, "Note write error", e)
        onComplete(0)
    }

}

internal class HeaderFooter : PdfPageEventHelper() {
    override fun onEndPage(writer: PdfWriter, document: Document?) {
        val rect = writer.getBoxSize("footer")
        val bfTimes: BaseFont
        try {
            bfTimes = BaseFont.createFont(
                BaseFont.TIMES_ROMAN, "Cp1252",
                false
            )
            val font = Font(bfTimes, 9f)
            ColumnText.showTextAligned(
                writer.directContent,
                Element.ALIGN_RIGHT,
                Phrase(
                    String.format("%d", writer.pageNumber),
                    font
                ), rect.right,
                rect.bottom - 18, 0f
            )
        } catch (e: DocumentException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


fun Context.proposalExists(id: String, noteTitle: String): Boolean {
    val fileName = "${noteTitle}.pdf"
    return File(this.getFolder(id), fileName).exists()
}

fun Context.getFiles(id: String) = this.getFolder(id).listFiles()!!.asList()

fun Context.saveImage(bitmap: Bitmap, fileName: String, id: String){
    val directory = this.getFolder(id)
    val file = File(directory, "$fileName.png")
    val outputStream = FileOutputStream(file)

    bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
    outputStream.flush()
    outputStream.close()
}

private fun Context.getFolder(id: String): File {
    val directory = File("${this.filesDir}/$id")

    if (!directory.exists()) {
        directory.mkdir()
    }

    return directory
}


