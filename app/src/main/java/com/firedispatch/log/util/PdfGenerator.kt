package com.firedispatch.log.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.firedispatch.log.ui.viewmodel.AttendanceSummary
import com.firedispatch.log.ui.viewmodel.EventColumn
import com.firedispatch.log.ui.viewmodel.MemberRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator {
    companion object {
        private const val PAGE_WIDTH = 842 // A4横向き
        private const val PAGE_HEIGHT = 595
        private const val MARGIN = 40f
        private const val CELL_HEIGHT = 30f
        private const val NAME_COLUMN_WIDTH = 100f
        private const val EVENT_COLUMN_WIDTH = 60f
        private const val SUMMARY_COLUMN_WIDTH = 50f

        suspend fun generatePdf(
            context: Context,
            uri: Uri,
            title: String,
            memberRows: List<MemberRow>,
            eventColumns: List<EventColumn>,
            attendanceSummaries: Map<Long, AttendanceSummary>
        ): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val dateFormat = SimpleDateFormat("M/d", Locale.JAPANESE)

                // ページサイズを計算
                val eventsPerPage = ((PAGE_WIDTH - MARGIN * 2 - NAME_COLUMN_WIDTH - SUMMARY_COLUMN_WIDTH * 2) / EVENT_COLUMN_WIDTH).toInt()
                val totalPages = (eventColumns.size + eventsPerPage - 1) / eventsPerPage

                for (page in 0 until totalPages) {
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), page + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    val canvas = pdfPage.canvas

                    val paint = Paint().apply {
                        color = Color.BLACK
                        textSize = 12f
                        isAntiAlias = true
                    }

                    val boldPaint = Paint(paint).apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }

                    // タイトル
                    boldPaint.textSize = 16f
                    canvas.drawText(title, MARGIN, MARGIN + 20, boldPaint)
                    boldPaint.textSize = 12f

                    // 表の開始位置
                    var currentY = MARGIN + 50

                    // このページに表示する行事
                    val startEventIndex = page * eventsPerPage
                    val endEventIndex = minOf(startEventIndex + eventsPerPage, eventColumns.size)
                    val pageEvents = eventColumns.subList(startEventIndex, endEventIndex)

                    // ヘッダー行
                    var currentX = MARGIN

                    // 氏名ヘッダー
                    canvas.drawRect(currentX, currentY, currentX + NAME_COLUMN_WIDTH, currentY + CELL_HEIGHT * 2, paint)
                    canvas.drawText("氏名", currentX + 10, currentY + CELL_HEIGHT, boldPaint)
                    currentX += NAME_COLUMN_WIDTH

                    // 行事ヘッダー
                    pageEvents.forEach { eventColumn ->
                        canvas.drawRect(currentX, currentY, currentX + EVENT_COLUMN_WIDTH, currentY + CELL_HEIGHT * 2, paint)
                        val dateStr = dateFormat.format(Date(eventColumn.event.date))
                        canvas.drawText("$dateStr[${eventColumn.event.allowanceIndex}]", currentX + 5, currentY + CELL_HEIGHT / 2, paint)
                        canvas.drawText(eventColumn.event.eventName, currentX + 5, currentY + CELL_HEIGHT + CELL_HEIGHT / 2, paint)
                        currentX += EVENT_COLUMN_WIDTH
                    }

                    // 集計ヘッダー
                    canvas.drawRect(currentX, currentY, currentX + SUMMARY_COLUMN_WIDTH, currentY + CELL_HEIGHT * 2, paint)
                    canvas.drawText("出動", currentX + 10, currentY + CELL_HEIGHT / 2, boldPaint)
                    canvas.drawText("回数", currentX + 10, currentY + CELL_HEIGHT + CELL_HEIGHT / 2, boldPaint)
                    currentX += SUMMARY_COLUMN_WIDTH

                    canvas.drawRect(currentX, currentY, currentX + SUMMARY_COLUMN_WIDTH, currentY + CELL_HEIGHT * 2, paint)
                    canvas.drawText("手当", currentX + 10, currentY + CELL_HEIGHT / 2, boldPaint)
                    canvas.drawText("指数", currentX + 10, currentY + CELL_HEIGHT + CELL_HEIGHT / 2, boldPaint)

                    currentY += CELL_HEIGHT * 2

                    // データ行
                    memberRows.forEach { memberRow ->
                        currentX = MARGIN

                        // 氏名
                        canvas.drawRect(currentX, currentY, currentX + NAME_COLUMN_WIDTH, currentY + CELL_HEIGHT, paint)
                        canvas.drawText(memberRow.member.name, currentX + 10, currentY + CELL_HEIGHT / 2 + 5, paint)
                        currentX += NAME_COLUMN_WIDTH

                        // 出動セル
                        pageEvents.forEach { eventColumn ->
                            canvas.drawRect(currentX, currentY, currentX + EVENT_COLUMN_WIDTH, currentY + CELL_HEIGHT, paint)
                            val attended = eventColumn.attendanceMap[memberRow.member.id] ?: false
                            if (attended) {
                                val fillPaint = Paint().apply {
                                    color = Color.RED
                                    alpha = 80
                                }
                                canvas.drawRect(currentX, currentY, currentX + EVENT_COLUMN_WIDTH, currentY + CELL_HEIGHT, fillPaint)
                                canvas.drawText("出動", currentX + 10, currentY + CELL_HEIGHT / 2 + 5, paint)
                            }
                            currentX += EVENT_COLUMN_WIDTH
                        }

                        // 集計
                        val summary = attendanceSummaries[memberRow.member.id]
                        canvas.drawRect(currentX, currentY, currentX + SUMMARY_COLUMN_WIDTH, currentY + CELL_HEIGHT, paint)
                        canvas.drawText("${summary?.attendanceCount ?: 0}", currentX + 15, currentY + CELL_HEIGHT / 2 + 5, paint)
                        currentX += SUMMARY_COLUMN_WIDTH

                        canvas.drawRect(currentX, currentY, currentX + SUMMARY_COLUMN_WIDTH, currentY + CELL_HEIGHT, paint)
                        canvas.drawText("${summary?.allowanceTotal ?: 0}", currentX + 15, currentY + CELL_HEIGHT / 2 + 5, paint)

                        currentY += CELL_HEIGHT
                    }

                    pdfDocument.finishPage(pdfPage)
                }

                // PDFを保存
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                pdfDocument.close()
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }
}
