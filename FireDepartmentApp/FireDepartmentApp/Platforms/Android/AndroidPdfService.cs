using Android.Graphics.Pdf;
using Android.Graphics;
using FireDepartmentApp.Models;
using FireDepartmentApp.Services;
using AndroidColor = Android.Graphics.Color;
using AndroidPaint = Android.Graphics.Paint;
using AndroidRectF = Android.Graphics.RectF;
using AndroidRect = Android.Graphics.Rect;
using SystemPath = System.IO.Path;

namespace FireDepartmentApp.Platforms.Android;

public class AndroidPdfService : IPdfService
{
    private readonly DataService _dataService;
    private const float PAGE_MARGIN = 40f;
    private const float CELL_HEIGHT = 30f;
    private const float HEADER_HEIGHT = 40f;
    private const int A4_WIDTH = 842;  // A4 landscape width in points
    private const int A4_HEIGHT = 595; // A4 landscape height in points

    public bool IsSupported => true;

    public AndroidPdfService()
    {
        _dataService = DataService.Instance;
    }

    public async Task<string> GenerateAttendancePdfAsync()
    {
        try
        {
            await _dataService.EnsureInitializedAsync();

            var members = _dataService.Members.ToList();
            var events = _dataService.GetEventsSortedByDate();

            var fileName = $"消防団出欠表_{DateTime.Now:yyyyMMdd_HHmmss}.pdf";
            var filePath = SystemPath.Combine(FileSystem.AppDataDirectory, fileName);

            // PdfDocument作成
            using var pdfDocument = new PdfDocument();

            // ページ情報設定
            var pageInfo = new PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).Create();
            var page = pdfDocument.StartPage(pageInfo);
            var canvas = page.Canvas;

            // エクセル風の表を描画
            await Task.Run(() => DrawAttendanceTable(canvas, members, events));

            pdfDocument.FinishPage(page);

            // ファイル保存
            using var fileStream = new FileStream(filePath, FileMode.Create, FileAccess.Write);
            pdfDocument.WriteTo(fileStream);
            pdfDocument.Close();

            return filePath;
        }
        catch (Exception ex)
        {
            throw new Exception($"Android PDF生成エラー: {ex.Message}");
        }
    }

    private void DrawAttendanceTable(Canvas canvas, List<Member> members, List<Event> events)
    {
        // 描画用のPaintオブジェクト
        var headerPaint = CreatePaint(AndroidColor.White, 12f, true);
        var cellPaint = CreatePaint(AndroidColor.Black, 10f, false);
        var borderPaint = CreatePaint(AndroidColor.Black, 1f, false, AndroidPaint.Style.Stroke);
        var headerBgPaint = CreatePaint(AndroidColor.ParseColor("#4ECDC4"), 1f, false, AndroidPaint.Style.Fill);
        var attendingBgPaint = CreatePaint(AndroidColor.ParseColor("#FF6B6B"), 1f, false, AndroidPaint.Style.Fill);
        var alternateBgPaint = CreatePaint(AndroidColor.ParseColor("#F8F9FA"), 1f, false, AndroidPaint.Style.Fill);

        var currentX = PAGE_MARGIN;
        var currentY = PAGE_MARGIN;

        // タイトル描画
        var titlePaint = CreatePaint(AndroidColor.Black, 16f, true);
        canvas.DrawText("消防団行事出欠表", currentX, currentY, titlePaint);
        currentY += 40f;

        // 作成日描画
        var datePaint = CreatePaint(AndroidColor.Gray, 10f, false);
        canvas.DrawText($"作成日: {DateTime.Now:yyyy年MM月dd日}", currentX, currentY, datePaint);
        currentY += 30f;

        // 列幅計算
        var nameColumnWidth = 80f;
        var eventColumnWidth = Math.Min(60f, (A4_WIDTH - PAGE_MARGIN * 2 - nameColumnWidth - 120f) / Math.Max(1, events.Count));
        var countColumnWidth = 60f;
        var allowanceColumnWidth = 60f;

        // ヘッダー行描画
        DrawTableHeader(canvas, currentX, currentY, nameColumnWidth, eventColumnWidth, countColumnWidth, allowanceColumnWidth,
                       events, headerPaint, headerBgPaint, borderPaint);

        currentY += HEADER_HEIGHT;

        // データ行描画
        for (int i = 0; i < members.Count; i++)
        {
            var member = members[i];
            var isAlternateRow = i % 2 == 0;

            DrawMemberRow(canvas, currentX, currentY, nameColumnWidth, eventColumnWidth, countColumnWidth, allowanceColumnWidth,
                         member, events, cellPaint, borderPaint, alternateBgPaint, attendingBgPaint, isAlternateRow);

            currentY += CELL_HEIGHT;
        }

        // 合計行描画
        DrawSummaryRow(canvas, currentX, currentY, nameColumnWidth, eventColumnWidth, countColumnWidth, allowanceColumnWidth,
                      members, events, cellPaint, headerBgPaint, headerPaint, borderPaint);
    }

    private void DrawTableHeader(Canvas canvas, float x, float y, float nameWidth, float eventWidth,
                                float countWidth, float allowanceWidth, List<Event> events,
                                AndroidPaint headerPaint, AndroidPaint bgPaint, AndroidPaint borderPaint)
    {
        var currentX = x;

        // 氏名列ヘッダー
        var nameRect = new AndroidRectF(currentX, y, currentX + nameWidth, y + HEADER_HEIGHT);
        canvas.DrawRect(nameRect, bgPaint);
        canvas.DrawRect(nameRect, borderPaint);
        DrawCenteredText(canvas, "氏名", nameRect, headerPaint);
        currentX += nameWidth;

        // 各行事列ヘッダー
        foreach (var ev in events)
        {
            var eventRect = new AndroidRectF(currentX, y, currentX + eventWidth, y + HEADER_HEIGHT);
            canvas.DrawRect(eventRect, bgPaint);
            canvas.DrawRect(eventRect, borderPaint);

            var headerText = $"{ev.Date:MM/dd}\n{TruncateText(ev.EventName, 8)}\n指数:{ev.AllowanceIndex}";
            DrawMultilineText(canvas, headerText, eventRect, headerPaint);
            currentX += eventWidth;
        }

        // 出動回数列ヘッダー
        var countRect = new AndroidRectF(currentX, y, currentX + countWidth, y + HEADER_HEIGHT);
        canvas.DrawRect(countRect, bgPaint);
        canvas.DrawRect(countRect, borderPaint);
        DrawCenteredText(canvas, "出動\n回数", countRect, headerPaint);
        currentX += countWidth;

        // 手当指数列ヘッダー
        var allowanceRect = new AndroidRectF(currentX, y, currentX + allowanceWidth, y + HEADER_HEIGHT);
        canvas.DrawRect(allowanceRect, bgPaint);
        canvas.DrawRect(allowanceRect, borderPaint);
        DrawCenteredText(canvas, "手当\n指数", allowanceRect, headerPaint);
    }

    private void DrawMemberRow(Canvas canvas, float x, float y, float nameWidth, float eventWidth,
                              float countWidth, float allowanceWidth, Member member, List<Event> events,
                              AndroidPaint cellPaint, AndroidPaint borderPaint, AndroidPaint alternateBgPaint,
                              AndroidPaint attendingBgPaint, bool isAlternateRow)
    {
        var currentX = x;

        // 氏名セル
        var nameRect = new AndroidRectF(currentX, y, currentX + nameWidth, y + CELL_HEIGHT);
        if (isAlternateRow) canvas.DrawRect(nameRect, alternateBgPaint);
        canvas.DrawRect(nameRect, borderPaint);
        DrawCenteredText(canvas, member.Name, nameRect, cellPaint);
        currentX += nameWidth;

        // 各行事セル
        foreach (var ev in events)
        {
            var eventRect = new AndroidRectF(currentX, y, currentX + eventWidth, y + CELL_HEIGHT);
            var isAttending = ev.AttendingMemberIds.Contains(member.Id);

            if (isAttending)
            {
                canvas.DrawRect(eventRect, attendingBgPaint);
                var attendingPaint = CreatePaint(AndroidColor.White, 10f, true);
                DrawCenteredText(canvas, "出動", eventRect, attendingPaint);
            }
            else if (isAlternateRow)
            {
                canvas.DrawRect(eventRect, alternateBgPaint);
            }

            canvas.DrawRect(eventRect, borderPaint);
            currentX += eventWidth;
        }

        // 出動回数セル
        var countRect = new AndroidRectF(currentX, y, currentX + countWidth, y + CELL_HEIGHT);
        if (isAlternateRow) canvas.DrawRect(countRect, alternateBgPaint);
        canvas.DrawRect(countRect, borderPaint);
        var attendanceCount = _dataService.GetAttendanceCount(member.Id);
        DrawCenteredText(canvas, attendanceCount.ToString(), countRect, cellPaint);
        currentX += countWidth;

        // 手当指数セル
        var allowanceRect = new AndroidRectF(currentX, y, currentX + allowanceWidth, y + CELL_HEIGHT);
        if (isAlternateRow) canvas.DrawRect(allowanceRect, alternateBgPaint);
        canvas.DrawRect(allowanceRect, borderPaint);
        var totalAllowance = _dataService.GetTotalAllowanceIndex(member.Id);
        DrawCenteredText(canvas, totalAllowance.ToString(), allowanceRect, cellPaint);
    }

    private void DrawSummaryRow(Canvas canvas, float x, float y, float nameWidth, float eventWidth,
                               float countWidth, float allowanceWidth, List<Member> members, List<Event> events,
                               AndroidPaint cellPaint, AndroidPaint bgPaint, AndroidPaint headerPaint, AndroidPaint borderPaint)
    {
        var currentX = x;

        // 合計ラベル
        var nameRect = new AndroidRectF(currentX, y, currentX + nameWidth, y + CELL_HEIGHT);
        canvas.DrawRect(nameRect, bgPaint);
        canvas.DrawRect(nameRect, borderPaint);
        DrawCenteredText(canvas, "合計", nameRect, headerPaint);
        currentX += nameWidth;

        // 各行事の出動者数
        foreach (var ev in events)
        {
            var eventRect = new AndroidRectF(currentX, y, currentX + eventWidth, y + CELL_HEIGHT);
            canvas.DrawRect(eventRect, bgPaint);
            canvas.DrawRect(eventRect, borderPaint);
            var attendingCount = ev.AttendingMemberIds.Count;
            DrawCenteredText(canvas, attendingCount.ToString(), eventRect, headerPaint);
            currentX += eventWidth;
        }

        // 総出動回数
        var countRect = new AndroidRectF(currentX, y, currentX + countWidth, y + CELL_HEIGHT);
        canvas.DrawRect(countRect, bgPaint);
        canvas.DrawRect(countRect, borderPaint);
        var totalAttendance = events.SelectMany(e => e.AttendingMemberIds).Count();
        DrawCenteredText(canvas, totalAttendance.ToString(), countRect, headerPaint);
        currentX += countWidth;

        // 総手当指数
        var allowanceRect = new AndroidRectF(currentX, y, currentX + allowanceWidth, y + CELL_HEIGHT);
        canvas.DrawRect(allowanceRect, bgPaint);
        canvas.DrawRect(allowanceRect, borderPaint);
        var totalAllowanceIndex = members.Sum(m => _dataService.GetTotalAllowanceIndex(m.Id));
        DrawCenteredText(canvas, totalAllowanceIndex.ToString(), allowanceRect, headerPaint);
    }

    public async Task<string> GenerateMemberListPdfAsync()
    {
        try
        {
            await _dataService.EnsureInitializedAsync();

            var members = _dataService.Members.ToList();

            var fileName = $"消防団員名簿_{DateTime.Now:yyyyMMdd_HHmmss}.pdf";
            var filePath = SystemPath.Combine(FileSystem.AppDataDirectory, fileName);

            using var pdfDocument = new PdfDocument();

            // 縦向きページ
            var pageInfo = new PdfDocument.PageInfo.Builder(A4_HEIGHT, A4_WIDTH, 1).Create();
            var page = pdfDocument.StartPage(pageInfo);
            var canvas = page.Canvas;

            await Task.Run(() => DrawMemberListTable(canvas, members));

            pdfDocument.FinishPage(page);

            using var fileStream = new FileStream(filePath, FileMode.Create, FileAccess.Write);
            pdfDocument.WriteTo(fileStream);
            pdfDocument.Close();

            return filePath;
        }
        catch (Exception ex)
        {
            throw new Exception($"Android PDF生成エラー: {ex.Message}");
        }
    }

    private void DrawMemberListTable(Canvas canvas, List<Member> members)
    {
        var headerPaint = CreatePaint(AndroidColor.White, 12f, true);
        var cellPaint = CreatePaint(AndroidColor.Black, 11f, false);
        var borderPaint = CreatePaint(AndroidColor.Black, 1f, false, AndroidPaint.Style.Stroke);
        var headerBgPaint = CreatePaint(AndroidColor.ParseColor("#4ECDC4"), 1f, false, AndroidPaint.Style.Fill);
        var alternateBgPaint = CreatePaint(AndroidColor.ParseColor("#F8F9FA"), 1f, false, AndroidPaint.Style.Fill);

        var currentY = PAGE_MARGIN;

        // タイトル
        var titlePaint = CreatePaint(AndroidColor.Black, 16f, true);
        canvas.DrawText("消防団員名簿", PAGE_MARGIN, currentY, titlePaint);
        currentY += 40f;

        // 作成日
        var datePaint = CreatePaint(AndroidColor.Gray, 10f, false);
        canvas.DrawText($"作成日: {DateTime.Now:yyyy年MM月dd日}", PAGE_MARGIN, currentY, datePaint);
        currentY += 30f;

        // 列幅設定
        var noWidth = 40f;
        var positionWidth = 120f;
        var nameWidth = 100f;
        var phoneWidth = 140f;

        // ヘッダー描画
        DrawMemberListHeader(canvas, PAGE_MARGIN, currentY, noWidth, positionWidth, nameWidth, phoneWidth,
                            headerPaint, headerBgPaint, borderPaint);
        currentY += HEADER_HEIGHT;

        // データ行描画
        for (int i = 0; i < members.Count; i++)
        {
            var member = members[i];
            var isAlternateRow = i % 2 == 0;

            DrawMemberListRow(canvas, PAGE_MARGIN, currentY, noWidth, positionWidth, nameWidth, phoneWidth,
                             i + 1, member, cellPaint, borderPaint, alternateBgPaint, isAlternateRow);

            currentY += CELL_HEIGHT;
        }
    }

    private void DrawMemberListHeader(Canvas canvas, float x, float y, float noWidth, float positionWidth,
                                     float nameWidth, float phoneWidth, AndroidPaint headerPaint,
                                     AndroidPaint bgPaint, AndroidPaint borderPaint)
    {
        var currentX = x;

        // No.列
        var noRect = new AndroidRectF(currentX, y, currentX + noWidth, y + HEADER_HEIGHT);
        canvas.DrawRect(noRect, bgPaint);
        canvas.DrawRect(noRect, borderPaint);
        DrawCenteredText(canvas, "No.", noRect, headerPaint);
        currentX += noWidth;

        // 役職列
        var positionRect = new AndroidRectF(currentX, y, currentX + positionWidth, y + HEADER_HEIGHT);
        canvas.DrawRect(positionRect, bgPaint);
        canvas.DrawRect(positionRect, borderPaint);
        DrawCenteredText(canvas, "役職", positionRect, headerPaint);
        currentX += positionWidth;

        // 氏名列
        var nameRect = new AndroidRectF(currentX, y, currentX + nameWidth, y + HEADER_HEIGHT);
        canvas.DrawRect(nameRect, bgPaint);
        canvas.DrawRect(nameRect, borderPaint);
        DrawCenteredText(canvas, "氏名", nameRect, headerPaint);
        currentX += nameWidth;

        // 電話番号列
        var phoneRect = new AndroidRectF(currentX, y, currentX + phoneWidth, y + HEADER_HEIGHT);
        canvas.DrawRect(phoneRect, bgPaint);
        canvas.DrawRect(phoneRect, borderPaint);
        DrawCenteredText(canvas, "電話番号", phoneRect, headerPaint);
    }

    private void DrawMemberListRow(Canvas canvas, float x, float y, float noWidth, float positionWidth,
                                  float nameWidth, float phoneWidth, int no, Member member,
                                  AndroidPaint cellPaint, AndroidPaint borderPaint, AndroidPaint alternateBgPaint, bool isAlternateRow)
    {
        var currentX = x;

        // No.セル
        var noRect = new AndroidRectF(currentX, y, currentX + noWidth, y + CELL_HEIGHT);
        if (isAlternateRow) canvas.DrawRect(noRect, alternateBgPaint);
        canvas.DrawRect(noRect, borderPaint);
        DrawCenteredText(canvas, no.ToString(), noRect, cellPaint);
        currentX += noWidth;

        // 役職セル
        var positionRect = new AndroidRectF(currentX, y, currentX + positionWidth, y + CELL_HEIGHT);
        if (isAlternateRow) canvas.DrawRect(positionRect, alternateBgPaint);
        canvas.DrawRect(positionRect, borderPaint);
        DrawCenteredText(canvas, member.Position, positionRect, cellPaint);
        currentX += positionWidth;

        // 氏名セル
        var nameRect = new AndroidRectF(currentX, y, currentX + nameWidth, y + CELL_HEIGHT);
        if (isAlternateRow) canvas.DrawRect(nameRect, alternateBgPaint);
        canvas.DrawRect(nameRect, borderPaint);
        DrawCenteredText(canvas, member.Name, nameRect, cellPaint);
        currentX += nameWidth;

        // 電話番号セル
        var phoneRect = new AndroidRectF(currentX, y, currentX + phoneWidth, y + CELL_HEIGHT);
        if (isAlternateRow) canvas.DrawRect(phoneRect, alternateBgPaint);
        canvas.DrawRect(phoneRect, borderPaint);
        DrawCenteredText(canvas, member.PhoneNumber ?? "", phoneRect, cellPaint);
    }

    // ヘルパーメソッド
    private AndroidPaint CreatePaint(AndroidColor color, float textSize, bool isBold, AndroidPaint.Style style = AndroidPaint.Style.Fill)
    {
        var paint = new AndroidPaint
        {
            Color = color,
            TextSize = textSize,
            AntiAlias = true
        };
        paint.SetStyle(style);

        if (isBold)
            paint.SetTypeface(Typeface.DefaultBold);

        return paint;
    }

    private void DrawCenteredText(Canvas canvas, string text, AndroidRectF rect, AndroidPaint paint)
    {
        var bounds = new AndroidRect();
        paint.GetTextBounds(text, 0, text.Length, bounds);

        var x = rect.CenterX() - bounds.Width() / 2f;
        var y = rect.CenterY() + bounds.Height() / 2f;

        canvas.DrawText(text, x, y, paint);
    }

    private void DrawMultilineText(Canvas canvas, string text, AndroidRectF rect, AndroidPaint paint)
    {
        var lines = text.Split('\n');
        var lineHeight = paint.TextSize + 2;
        var totalHeight = lines.Length * lineHeight;
        var startY = rect.CenterY() - totalHeight / 2f + lineHeight / 2f;

        for (int i = 0; i < lines.Length; i++)
        {
            var bounds = new AndroidRect();
            paint.GetTextBounds(lines[i], 0, lines[i].Length, bounds);
            var x = rect.CenterX() - bounds.Width() / 2f;
            var y = startY + i * lineHeight;
            canvas.DrawText(lines[i], x, y, paint);
        }
    }

    private string TruncateText(string text, int maxLength)
    {
        return text.Length <= maxLength ? text : text.Substring(0, maxLength - 1) + "…";
    }
}