package com.firedispatch.log.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.firedispatch.log.data.entity.Event
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.model.RoleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExportGenerator {
    companion object {
        // A4サイズ（ポイント単位）
        private const val A4_WIDTH = 595  // A4 portrait width
        private const val A4_HEIGHT = 842 // A4 portrait height
        private const val A4_LANDSCAPE_WIDTH = 842  // A4 landscape width
        private const val A4_LANDSCAPE_HEIGHT = 595 // A4 landscape height

        private const val PAGE_MARGIN = 40f
        private const val CELL_HEIGHT = 30f
        private const val HEADER_HEIGHT = 40f

        // カラー定義
        private val HEADER_COLOR = Color.parseColor("#4ECDC4")
        private val ATTENDING_COLOR = Color.parseColor("#FF6B6B")
        private val ALTERNATE_BG_COLOR = Color.parseColor("#F8F9FA")
        private val BORDER_COLOR = Color.parseColor("#DDDDDD")

        /**
         * 団員名簿PDFを生成
         */
        suspend fun generateMemberListPdf(
            context: Context,
            organizationName: String,
            members: List<Member>,
            roleAssignments: List<RoleAssignment>
        ): Result<File> = withContext(Dispatchers.IO) {
            try {
                val pdfDir = File(context.cacheDir, "PDFs")
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs()
                }

                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val pdfFile = File(pdfDir, "団員名簿_$timestamp.pdf")

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                drawMemberListTable(canvas, organizationName, members, roleAssignments)

                pdfDocument.finishPage(page)

                FileOutputStream(pdfFile).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                pdfDocument.close()
                Result.success(pdfFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun drawMemberListTable(
            canvas: Canvas,
            organizationName: String,
            members: List<Member>,
            roleAssignments: List<RoleAssignment>
        ) {
            val headerPaint = createPaint(Color.WHITE, 12f, true)
            val cellPaint = createPaint(Color.BLACK, 11f, false)
            val borderPaint = createPaint(Color.BLACK, 1f, false, Paint.Style.STROKE)
            val headerBgPaint = createPaint(HEADER_COLOR, 1f, false, Paint.Style.FILL)
            val alternateBgPaint = createPaint(ALTERNATE_BG_COLOR, 1f, false, Paint.Style.FILL)

            var currentY = PAGE_MARGIN

            // タイトル
            val titlePaint = createPaint(Color.BLACK, 16f, true)
            canvas.drawText("$organizationName 団員名簿", PAGE_MARGIN, currentY, titlePaint)
            currentY += 40f

            // 作成日
            val datePaint = createPaint(Color.GRAY, 10f, false)
            canvas.drawText("作成日: ${SimpleDateFormat("yyyy年MM月dd日", Locale.JAPANESE).format(Date())}", PAGE_MARGIN, currentY, datePaint)
            currentY += 30f

            // 列幅設定
            val noWidth = 40f
            val positionWidth = 120f
            val nameWidth = 100f
            val phoneWidth = 140f

            // 役職マップを作成して役職順に並び替え
            val roleMap = roleAssignments.associateBy { it.memberId }
            val sortedMembers = members.mapNotNull { member ->
                val roleType = roleMap[member.id]?.let { RoleType.valueOf(it.roleType) }
                if (roleType != null && roleType != RoleType.HOJODAN) {
                    Pair(member, roleType)
                } else null
            }.sortedBy { it.second.order }

            // テーブルを中央に配置
            val tableWidth = noWidth + positionWidth + nameWidth + phoneWidth
            val tableStartX = (A4_WIDTH - tableWidth) / 2f

            // ヘッダー描画
            var currentX = tableStartX

            // No.ヘッダー
            var rect = RectF(currentX, currentY, currentX + noWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "No.", rect, headerPaint)
            currentX += noWidth

            // 役職ヘッダー
            rect = RectF(currentX, currentY, currentX + positionWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "役職", rect, headerPaint)
            currentX += positionWidth

            // 氏名ヘッダー
            rect = RectF(currentX, currentY, currentX + nameWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "氏名", rect, headerPaint)
            currentX += nameWidth

            // 電話番号ヘッダー
            rect = RectF(currentX, currentY, currentX + phoneWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "電話番号", rect, headerPaint)

            currentY += HEADER_HEIGHT

            // データ行描画（役職順）
            sortedMembers.forEachIndexed { index, (member, roleType) ->
                val isAlternateRow = index % 2 == 0
                currentX = tableStartX

                // No.セル
                rect = RectF(currentX, currentY, currentX + noWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, (index + 1).toString(), rect, cellPaint)
                currentX += noWidth

                // 役職セル
                rect = RectF(currentX, currentY, currentX + positionWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, roleType?.displayName ?: "", rect, cellPaint)
                currentX += positionWidth

                // 氏名セル
                rect = RectF(currentX, currentY, currentX + nameWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, member.name, rect, cellPaint)
                currentX += nameWidth

                // 電話番号セル
                rect = RectF(currentX, currentY, currentX + phoneWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, member.phoneNumber ?: "", rect, cellPaint)

                currentY += CELL_HEIGHT
            }
        }

        /**
         * 出動表PDFを生成（CSV形式：日付・行事名を縦、団員名を横）
         */
        suspend fun generateAttendanceTablePdf(
            context: Context,
            organizationName: String,
            members: List<Member>,
            roleAssignments: List<RoleAssignment>,
            events: List<Event>,
            eventColumnsData: List<Triple<Long, String, Map<Long, Boolean>>>
        ): Result<File> = withContext(Dispatchers.IO) {
            try {
                val pdfDir = File(context.cacheDir, "PDFs")
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs()
                }

                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val pdfFile = File(pdfDir, "出動表_$timestamp.pdf")

                val pdfDocument = PdfDocument()

                // 役職順に並べたメンバーリストを作成（補助団員を除く）
                val roleMap = roleAssignments.associateBy { it.memberId }
                val sortedMembers = members.mapNotNull { member ->
                    val roleType = roleMap[member.id]?.let { RoleType.valueOf(it.roleType) }
                    if (roleType != null && roleType != RoleType.HOJODAN) {
                        Pair(member, roleType)
                    } else null
                }.sortedBy { it.second.order }

                // 1ページあたりの行事数を余白から自動計算
                val nameColumnWidth = 80f
                val eventColumnWidth = 55f
                val summaryColumnWidth = 50f
                val availableWidth = A4_WIDTH - PAGE_MARGIN * 2 - nameColumnWidth - summaryColumnWidth
                val eventsPerPage = (availableWidth / eventColumnWidth).toInt()
                val totalPages = if (events.isEmpty()) 1 else ((events.size + eventsPerPage - 1) / eventsPerPage)

                // ページごとに生成
                for (pageNum in 0 until totalPages) {
                    val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNum + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    // このページに表示する行事
                    val startEventIndex = pageNum * eventsPerPage
                    val endEventIndex = minOf(startEventIndex + eventsPerPage, events.size)
                    val pageEvents = events.subList(startEventIndex, endEventIndex)
                    val pageEventColumnsData = eventColumnsData.subList(startEventIndex, endEventIndex)

                    drawAttendanceTableExcelStyle(
                        canvas,
                        organizationName,
                        sortedMembers,
                        pageEvents,
                        pageEventColumnsData,
                        events,
                        eventColumnsData,
                        pageNum + 1,
                        totalPages
                    )

                    pdfDocument.finishPage(page)
                }

                FileOutputStream(pdfFile).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                pdfDocument.close()
                Result.success(pdfFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun drawAttendanceTableExcelStyle(
            canvas: Canvas,
            organizationName: String,
            sortedMembers: List<Pair<Member, RoleType>>,
            pageEvents: List<Event>,
            pageEventColumnsData: List<Triple<Long, String, Map<Long, Boolean>>>,
            allEvents: List<Event>,
            allEventColumnsData: List<Triple<Long, String, Map<Long, Boolean>>>,
            currentPage: Int,
            totalPages: Int
        ) {
            val headerPaint = createPaint(Color.WHITE, 11f, true)
            val cellPaint = createPaint(Color.BLACK, 9f, false)
            val borderPaint = createPaint(BORDER_COLOR, 0.5f, false, Paint.Style.STROKE)
            val headerBgPaint = createPaint(HEADER_COLOR, 1f, false, Paint.Style.FILL)
            val attendingBgPaint = createPaint(ATTENDING_COLOR, 1f, false, Paint.Style.FILL)
            val alternateBgPaint = createPaint(ALTERNATE_BG_COLOR, 1f, false, Paint.Style.FILL)

            var currentY = PAGE_MARGIN

            // タイトル
            val titlePaint = createPaint(Color.BLACK, 16f, true)
            canvas.drawText("$organizationName 出動表", PAGE_MARGIN, currentY, titlePaint)

            // ページ番号
            val pagePaint = createPaint(Color.GRAY, 10f, false)
            val pageText = "ページ $currentPage / $totalPages"
            val pageTextWidth = pagePaint.measureText(pageText)
            canvas.drawText(pageText, A4_WIDTH - PAGE_MARGIN - pageTextWidth, currentY, pagePaint)
            currentY += 30f

            // 作成日
            val datePaint = createPaint(Color.GRAY, 10f, false)
            canvas.drawText("作成日: ${SimpleDateFormat("yyyy年MM月dd日", Locale.JAPANESE).format(Date())}", PAGE_MARGIN, currentY, datePaint)
            currentY += 25f

            // 列幅設定
            val nameColumnWidth = 80f  // 氏名列（横書き）
            val eventColumnWidth = 55f  // 行事列（横書き3行用）
            val summaryColumnWidth = 50f  // 合計列
            val headerHeightExtended = 50f  // ヘッダー高さ

            var currentX = PAGE_MARGIN

            // ヘッダー行
            // 氏名ヘッダー
            var rect = RectF(currentX, currentY, currentX + nameColumnWidth, currentY + headerHeightExtended)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "氏名", rect, headerPaint)
            currentX += nameColumnWidth

            // 各行事ヘッダー（横書き3行：日付1行+行事名2行）
            val eventDateFormat = SimpleDateFormat("M/d", Locale.JAPANESE)
            pageEvents.forEach { event ->
                rect = RectF(currentX, currentY, currentX + eventColumnWidth, currentY + headerHeightExtended)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)

                // 日付1行、行事名を2行に分割して表示
                val dateStr = eventDateFormat.format(Date(event.date))
                val eventName = event.eventName

                // 行事名を適切な長さで2行に分割
                val line1 = if (eventName.length > 6) truncateText(eventName.substring(0, minOf(6, eventName.length)), 6) else eventName
                val line2 = if (eventName.length > 6) truncateText(eventName.substring(6), 6) else ""

                val headerText = if (line2.isNotEmpty()) {
                    "$dateStr\n$line1\n$line2"
                } else {
                    "$dateStr\n$line1"
                }

                drawMultilineText(canvas, headerText, rect, headerPaint)
                currentX += eventColumnWidth
            }

            // 合計ヘッダー（最終ページのみ）
            if (currentPage == totalPages) {
                rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + headerHeightExtended)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, "合計", rect, headerPaint)
            }

            currentY += headerHeightExtended

            // データ行（各団員）
            sortedMembers.forEachIndexed { memberIndex, (member, _) ->
                val isAlternateRow = memberIndex % 2 == 0
                currentX = PAGE_MARGIN

                // 氏名セル（横書き）
                rect = RectF(currentX, currentY, currentX + nameColumnWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, member.name, rect, cellPaint)
                currentX += nameColumnWidth

                // このページの行事の出動状況
                pageEvents.forEachIndexed { eventIndex, event ->
                    rect = RectF(currentX, currentY, currentX + eventColumnWidth, currentY + CELL_HEIGHT)
                    val (_, _, attendanceMap) = pageEventColumnsData[eventIndex]
                    val isAttending = attendanceMap[member.id] ?: false

                    if (isAttending) {
                        canvas.drawRect(rect, attendingBgPaint)
                        val attendingPaint = createPaint(Color.WHITE, 10f, true)
                        drawCenteredText(canvas, event.allowanceIndex.toString(), rect, attendingPaint)
                    } else if (isAlternateRow) {
                        canvas.drawRect(rect, alternateBgPaint)
                    }

                    canvas.drawRect(rect, borderPaint)
                    currentX += eventColumnWidth
                }

                // 合計セル（全イベントの合計、最終ページのみ）
                if (currentPage == totalPages) {
                    var memberTotal = 0
                    allEvents.forEachIndexed { eventIndex, event ->
                        val (_, _, attendanceMap) = allEventColumnsData[eventIndex]
                        if (attendanceMap[member.id] == true) {
                            memberTotal += event.allowanceIndex
                        }
                    }

                    rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + CELL_HEIGHT)
                    if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                    canvas.drawRect(rect, borderPaint)
                    drawCenteredText(canvas, memberTotal.toString(), rect, cellPaint)
                }

                currentY += CELL_HEIGHT
            }

            // 合計行（最後のページのみ）
            if (currentPage == totalPages) {
                currentX = PAGE_MARGIN

                // 「合計」ラベル
                rect = RectF(currentX, currentY, currentX + nameColumnWidth, currentY + CELL_HEIGHT)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, "合計", rect, headerPaint)
                currentX += nameColumnWidth

                // このページの各行事の合計
                pageEvents.forEachIndexed { eventIndex, event ->
                    rect = RectF(currentX, currentY, currentX + eventColumnWidth, currentY + CELL_HEIGHT)
                    canvas.drawRect(rect, headerBgPaint)
                    canvas.drawRect(rect, borderPaint)

                    val (_, _, attendanceMap) = pageEventColumnsData[eventIndex]
                    var eventTotal = 0
                    sortedMembers.forEach { (member, _) ->
                        if (attendanceMap[member.id] == true) {
                            eventTotal += event.allowanceIndex
                        }
                    }

                    drawCenteredText(canvas, eventTotal.toString(), rect, headerPaint)
                    currentX += eventColumnWidth
                }

                // 全団員・全イベントの合計
                var grandTotal = 0
                allEvents.forEachIndexed { eventIndex, event ->
                    val (_, _, attendanceMap) = allEventColumnsData[eventIndex]
                    sortedMembers.forEach { (member, _) ->
                        if (attendanceMap[member.id] == true) {
                            grandTotal += event.allowanceIndex
                        }
                    }
                }

                rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + CELL_HEIGHT)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, grandTotal.toString(), rect, headerPaint)
            }
        }

        private fun drawAttendanceTablePage(
            canvas: Canvas,
            organizationName: String,
            sortedMembers: List<Pair<Member, RoleType>>,
            allEvents: List<Event>,
            allEventColumnsData: List<Triple<Long, String, Map<Long, Boolean>>>,
            pageEvents: List<Event>,
            pageEventColumns: List<Triple<Long, String, Map<Long, Boolean>>>,
            nameColumnWidth: Float,
            eventColumnWidth: Float,
            summaryColumnWidth: Float,
            currentPage: Int,
            totalPages: Int
        ) {
            val headerPaint = createPaint(Color.WHITE, 11f, true)
            val cellPaint = createPaint(Color.BLACK, 10f, false)
            val borderPaint = createPaint(BORDER_COLOR, 0.5f, false, Paint.Style.STROKE)
            val headerBgPaint = createPaint(HEADER_COLOR, 1f, false, Paint.Style.FILL)
            val attendingBgPaint = createPaint(ATTENDING_COLOR, 1f, false, Paint.Style.FILL)
            val alternateBgPaint = createPaint(ALTERNATE_BG_COLOR, 1f, false, Paint.Style.FILL)

            var currentY = PAGE_MARGIN

            // タイトル
            val titlePaint = createPaint(Color.BLACK, 16f, true)
            canvas.drawText("$organizationName 出動表", PAGE_MARGIN, currentY, titlePaint)

            // ページ番号
            val pagePaint = createPaint(Color.GRAY, 10f, false)
            val pageText = "ページ $currentPage / $totalPages"
            val pageTextWidth = pagePaint.measureText(pageText)
            canvas.drawText(pageText, A4_LANDSCAPE_WIDTH - PAGE_MARGIN - pageTextWidth, currentY, pagePaint)
            currentY += 30f

            // 作成日
            val datePaint = createPaint(Color.GRAY, 10f, false)
            canvas.drawText("作成日: ${SimpleDateFormat("yyyy年MM月dd日", Locale.JAPANESE).format(Date())}", PAGE_MARGIN, currentY, datePaint)
            currentY += 30f

            var currentX = PAGE_MARGIN

            // ヘッダー行
            // 氏名ヘッダー
            var rect = RectF(currentX, currentY, currentX + nameColumnWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "氏名", rect, headerPaint)
            currentX += nameColumnWidth

            // 各行事ヘッダー
            val eventDateFormat = SimpleDateFormat("MM/dd", Locale.JAPANESE)
            pageEvents.forEach { event ->
                rect = RectF(currentX, currentY, currentX + eventColumnWidth, currentY + HEADER_HEIGHT)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)
                val dateStr = eventDateFormat.format(Date(event.date))
                val headerText = "$dateStr\n${truncateText(event.eventName, 6)}\n[${event.allowanceIndex}]"
                drawMultilineText(canvas, headerText, rect, headerPaint)
                currentX += eventColumnWidth
            }

            // 出動回数ヘッダー
            rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "出動\n回数", rect, headerPaint)
            currentX += summaryColumnWidth

            // 手当指数ヘッダー
            rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "手当\n指数", rect, headerPaint)

            currentY += HEADER_HEIGHT

            // データ行
            sortedMembers.forEachIndexed { index, (member, _) ->
                val isAlternateRow = index % 2 == 0
                currentX = PAGE_MARGIN

                // 氏名セル
                rect = RectF(currentX, currentY, currentX + nameColumnWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, member.name, rect, cellPaint)
                currentX += nameColumnWidth

                // 各行事セル - 手当指数を表示
                pageEvents.forEachIndexed { eventIndex, event ->
                    rect = RectF(currentX, currentY, currentX + eventColumnWidth, currentY + CELL_HEIGHT)
                    val attendanceMap = pageEventColumns[eventIndex].third
                    val isAttending = attendanceMap[member.id] ?: false

                    if (isAttending) {
                        canvas.drawRect(rect, attendingBgPaint)
                        val attendingPaint = createPaint(Color.WHITE, 10f, true)
                        // 手当指数を表示
                        drawCenteredText(canvas, event.allowanceIndex.toString(), rect, attendingPaint)
                    } else if (isAlternateRow) {
                        canvas.drawRect(rect, alternateBgPaint)
                    }

                    canvas.drawRect(rect, borderPaint)
                    currentX += eventColumnWidth
                }

                // 全イベントでの出動回数と手当指数の合計を計算
                var attendanceCount = 0
                var allowanceTotal = 0
                allEvents.forEachIndexed { eventIndex, event ->
                    val (_, _, attendanceMap) = allEventColumnsData[eventIndex]
                    val isAttending = attendanceMap[member.id] ?: false
                    if (isAttending) {
                        attendanceCount++
                        allowanceTotal += event.allowanceIndex
                    }
                }

                rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, attendanceCount.toString(), rect, cellPaint)
                currentX += summaryColumnWidth

                // 手当指数セル
                rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, allowanceTotal.toString(), rect, cellPaint)

                currentY += CELL_HEIGHT
            }

            // 合計行を追加
            currentX = PAGE_MARGIN

            // 「合計」ラベル
            rect = RectF(currentX, currentY, currentX + nameColumnWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "合計", rect, headerPaint)
            currentX += nameColumnWidth

            // 各行事の出動者合計
            var grandTotalAttendance = 0
            var grandTotalAllowance = 0
            pageEvents.forEachIndexed { eventIndex, event ->
                rect = RectF(currentX, currentY, currentX + eventColumnWidth, currentY + CELL_HEIGHT)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)

                val attendanceMap = pageEventColumns[eventIndex].third
                var eventTotal = 0
                sortedMembers.forEach { (member, _) ->
                    if (attendanceMap[member.id] == true) {
                        eventTotal += event.allowanceIndex
                    }
                }
                grandTotalAttendance += eventTotal

                drawCenteredText(canvas, eventTotal.toString(), rect, headerPaint)
                currentX += eventColumnWidth
            }

            // 出動回数合計（空欄）
            rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            currentX += summaryColumnWidth

            // 手当指数合計
            rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)

            // 全団員の手当指数合計を計算
            sortedMembers.forEach { (member, _) ->
                allEvents.forEachIndexed { eventIndex, event ->
                    val (_, _, attendanceMap) = allEventColumnsData[eventIndex]
                    if (attendanceMap[member.id] == true) {
                        grandTotalAllowance += event.allowanceIndex
                    }
                }
            }

            drawCenteredText(canvas, grandTotalAllowance.toString(), rect, headerPaint)
            currentY += CELL_HEIGHT

            // 全団員合計行を追加（最終ページのみ）
            if (currentPage == totalPages) {
                currentX = PAGE_MARGIN

                // 「全団員合計」ラベル
                rect = RectF(currentX, currentY, currentX + nameColumnWidth, currentY + CELL_HEIGHT)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, "全団員合計", rect, headerPaint)
                currentX += nameColumnWidth

                // 全イベントの手当指数合計
                var allEventsTotal = 0
                allEvents.forEachIndexed { eventIndex, event ->
                    val (_, _, attendanceMap) = allEventColumnsData[eventIndex]
                    sortedMembers.forEach { (member, _) ->
                        if (attendanceMap[member.id] == true) {
                            allEventsTotal += event.allowanceIndex
                        }
                    }
                }

                // 残りのセルをスキップして最後の手当指数列に表示
                currentX += eventColumnWidth * pageEvents.size + summaryColumnWidth

                rect = RectF(currentX, currentY, currentX + summaryColumnWidth, currentY + CELL_HEIGHT)
                canvas.drawRect(rect, headerBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, allEventsTotal.toString(), rect, headerPaint)
            }
        }

        /**
         * 出動手当PDFを生成
         */
        suspend fun generateAllowancePdf(
            context: Context,
            organizationName: String,
            members: List<Member>,
            roleAssignments: List<RoleAssignment>,
            allowanceIndexMap: Map<Long, Int>,
            allowancePerAttendance: Int
        ): Result<File> = withContext(Dispatchers.IO) {
            try {
                val pdfDir = File(context.cacheDir, "PDFs")
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs()
                }

                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val pdfFile = File(pdfDir, "出動手当_$timestamp.pdf")

                val pdfDocument = PdfDocument()
                // 縦向きA4に変更
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                drawAllowanceTable(canvas, organizationName, members, roleAssignments, allowanceIndexMap, allowancePerAttendance)

                pdfDocument.finishPage(page)

                FileOutputStream(pdfFile).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                pdfDocument.close()
                Result.success(pdfFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun drawAllowanceTable(
            canvas: Canvas,
            organizationName: String,
            members: List<Member>,
            roleAssignments: List<RoleAssignment>,
            allowanceIndexMap: Map<Long, Int>,
            allowancePerAttendance: Int
        ) {
            val headerPaint = createPaint(Color.WHITE, 12f, true)
            val cellPaint = createPaint(Color.BLACK, 11f, false)
            val borderPaint = createPaint(BORDER_COLOR, 0.5f, false, Paint.Style.STROKE)
            val headerBgPaint = createPaint(HEADER_COLOR, 1f, false, Paint.Style.FILL)
            val alternateBgPaint = createPaint(ALTERNATE_BG_COLOR, 1f, false, Paint.Style.FILL)

            // 役職順に並び替え
            val roleMap = roleAssignments.associateBy { it.memberId }
            val sortedMembers = members.mapNotNull { member ->
                val roleType = roleMap[member.id]?.let { RoleType.valueOf(it.roleType) }
                if (roleType != null && roleType != RoleType.HOJODAN) {
                    Pair(member, roleType)
                } else null
            }.sortedBy { it.second.order }

            var currentY = PAGE_MARGIN

            // タイトル
            val titlePaint = createPaint(Color.BLACK, 16f, true)
            canvas.drawText("$organizationName 出動手当", PAGE_MARGIN, currentY, titlePaint)
            currentY += 40f

            // 作成日
            val datePaint = createPaint(Color.GRAY, 10f, false)
            canvas.drawText("作成日: ${SimpleDateFormat("yyyy年MM月dd日", Locale.JAPANESE).format(Date())}", PAGE_MARGIN, currentY, datePaint)
            currentY += 30f

            // 列幅設定
            val nameWidth = 100f
            val countWidth = 70f
            val amountWidth = 90f
            val spacerWidth = 20f
            val billWidth = 60f  // 紙幣枚数列

            var currentX = PAGE_MARGIN

            // ヘッダー行
            // 氏名
            var rect = RectF(currentX, currentY, currentX + nameWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "氏名", rect, headerPaint)
            currentX += nameWidth

            // 手当指数
            rect = RectF(currentX, currentY, currentX + countWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "手当指数", rect, headerPaint)
            currentX += countWidth

            // 手当金額
            rect = RectF(currentX, currentY, currentX + amountWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "手当金額", rect, headerPaint)
            currentX += amountWidth

            // スペーサー
            currentX += spacerWidth

            // 壱萬円
            rect = RectF(currentX, currentY, currentX + billWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "壱萬円", rect, headerPaint)
            currentX += billWidth

            // 五千円
            rect = RectF(currentX, currentY, currentX + billWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "五千円", rect, headerPaint)
            currentX += billWidth

            // 千円
            rect = RectF(currentX, currentY, currentX + billWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "千円", rect, headerPaint)
            currentX += billWidth

            // 五百円
            rect = RectF(currentX, currentY, currentX + billWidth, currentY + HEADER_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "五百円", rect, headerPaint)

            currentY += HEADER_HEIGHT

            // データ行（役職順）
            var totalAllowanceIndex = 0
            var totalAmount = 0
            var total10000 = 0
            var total5000 = 0
            var total1000 = 0
            var total500 = 0

            sortedMembers.forEachIndexed { index, (member, _) ->
                val isAlternateRow = index % 2 == 0
                currentX = PAGE_MARGIN

                val allowanceIndex = allowanceIndexMap[member.id] ?: 0
                val amount = allowanceIndex * allowancePerAttendance

                // 紙幣枚数計算
                var remainingAmount = amount
                val bills10000 = remainingAmount / 10000
                remainingAmount %= 10000
                val bills5000 = remainingAmount / 5000
                remainingAmount %= 5000
                val bills1000 = remainingAmount / 1000
                remainingAmount %= 1000
                val bills500 = remainingAmount / 500

                totalAllowanceIndex += allowanceIndex
                totalAmount += amount
                total10000 += bills10000
                total5000 += bills5000
                total1000 += bills1000
                total500 += bills500

                // 氏名セル
                rect = RectF(currentX, currentY, currentX + nameWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, member.name, rect, cellPaint)
                currentX += nameWidth

                // 手当指数セル
                rect = RectF(currentX, currentY, currentX + countWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, allowanceIndex.toString(), rect, cellPaint)
                currentX += countWidth

                // 手当金額セル
                rect = RectF(currentX, currentY, currentX + amountWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                drawCenteredText(canvas, String.format("%,d", amount), rect, cellPaint)
                currentX += amountWidth

                // スペーサー
                currentX += spacerWidth

                // 壱萬円セル
                rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                if (bills10000 > 0) drawCenteredText(canvas, bills10000.toString(), rect, cellPaint)
                currentX += billWidth

                // 五千円セル
                rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                if (bills5000 > 0) drawCenteredText(canvas, bills5000.toString(), rect, cellPaint)
                currentX += billWidth

                // 千円セル
                rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                if (bills1000 > 0) drawCenteredText(canvas, bills1000.toString(), rect, cellPaint)
                currentX += billWidth

                // 五百円セル
                rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
                if (isAlternateRow) canvas.drawRect(rect, alternateBgPaint)
                canvas.drawRect(rect, borderPaint)
                if (bills500 > 0) drawCenteredText(canvas, bills500.toString(), rect, cellPaint)

                currentY += CELL_HEIGHT
            }

            // 合計行
            currentX = PAGE_MARGIN

            // 「計」ラベル
            rect = RectF(currentX, currentY, currentX + nameWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, "計", rect, headerPaint)
            currentX += nameWidth

            // 合計手当指数
            rect = RectF(currentX, currentY, currentX + countWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, totalAllowanceIndex.toString(), rect, headerPaint)
            currentX += countWidth

            // 合計手当金額
            rect = RectF(currentX, currentY, currentX + amountWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, String.format("%,d", totalAmount), rect, headerPaint)
            currentX += amountWidth

            // スペーサー
            currentX += spacerWidth

            // 合計紙幣枚数
            rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, total10000.toString(), rect, headerPaint)
            currentX += billWidth

            rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, total5000.toString(), rect, headerPaint)
            currentX += billWidth

            rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, total1000.toString(), rect, headerPaint)
            currentX += billWidth

            rect = RectF(currentX, currentY, currentX + billWidth, currentY + CELL_HEIGHT)
            canvas.drawRect(rect, headerBgPaint)
            canvas.drawRect(rect, borderPaint)
            drawCenteredText(canvas, total500.toString(), rect, headerPaint)

            currentY += CELL_HEIGHT + 20f

            // 1回出動の支給額
            val infoPaint = createPaint(Color.BLACK, 12f, true)
            canvas.drawText("1回出動の支給額: ${String.format("%,d", allowancePerAttendance)}円", PAGE_MARGIN, currentY, infoPaint)
        }

        /**
         * FileProviderを使ってPDFを開くIntentを作成
         */
        fun openPdfWithIntent(context: Context, pdfFile: File): Intent {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            return Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }

        // ヘルパーメソッド
        private fun createPaint(
            color: Int,
            textSize: Float,
            isBold: Boolean,
            style: Paint.Style = Paint.Style.FILL
        ): Paint {
            return Paint().apply {
                this.color = color
                this.textSize = textSize
                this.isAntiAlias = true
                this.style = style
                if (isBold) {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
        }

        private fun drawCenteredText(canvas: Canvas, text: String, rect: RectF, paint: Paint) {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)

            val x = rect.centerX() - bounds.width() / 2f
            val y = rect.centerY() + bounds.height() / 2f

            canvas.drawText(text, x, y, paint)
        }

        private fun drawMultilineText(canvas: Canvas, text: String, rect: RectF, paint: Paint) {
            val lines = text.split('\n')
            val lineHeight = paint.textSize + 2
            val totalHeight = lines.size * lineHeight
            var startY = rect.centerY() - totalHeight / 2f + lineHeight / 2f

            lines.forEach { line ->
                val bounds = Rect()
                paint.getTextBounds(line, 0, line.length, bounds)
                val x = rect.centerX() - bounds.width() / 2f
                canvas.drawText(line, x, startY, paint)
                startY += lineHeight
            }
        }

        private fun truncateText(text: String, maxLength: Int): String {
            return if (text.length <= maxLength) text else text.substring(0, maxLength - 1) + "…"
        }

        private fun drawVerticalText(canvas: Canvas, text: String, rect: RectF, paint: Paint) {
            val chars = text.toCharArray()
            val charHeight = paint.textSize + 2
            val totalHeight = chars.size * charHeight
            var startY = rect.centerY() - totalHeight / 2f + charHeight / 2f

            chars.forEach { char ->
                val bounds = Rect()
                paint.getTextBounds(char.toString(), 0, 1, bounds)
                val x = rect.centerX() - bounds.width() / 2f
                canvas.drawText(char.toString(), x, startY, paint)
                startY += charHeight
            }
        }
    }
}
