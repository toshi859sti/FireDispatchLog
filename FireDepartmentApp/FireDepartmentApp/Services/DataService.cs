using FireDepartmentApp.Models;
using System.Collections.ObjectModel;
using System.Text.Json;

namespace FireDepartmentApp.Services;

public class DataService
{
    private static DataService? _instance;
    public static DataService Instance => _instance ??= new DataService();

    public ObservableCollection<Member> Members { get; private set; } = [];
    public ObservableCollection<Event> Events { get; private set; } = [];

    public event EventHandler? DataChanged;
    private bool _isInitialized = false;

    private DataService()
    {
        InitializeDefaultData();
        Task.Run(async () =>
        {
            try
            {
                await LoadDataAsync();
                _isInitialized = true;
                MainThread.BeginInvokeOnMainThread(() => DataChanged?.Invoke(this, EventArgs.Empty));
            }
            catch
            {
                _isInitialized = true;
            }
        });
    }

    public async Task EnsureInitializedAsync()
    {
        if (!_isInitialized)
        {
            while (!_isInitialized)
            {
                await Task.Delay(50);
            }
        }
    }

    private void InitializeDefaultData()
    {
        if (Members.Count == 0)
        {
            Members.Add(new Member { Id = 1, Position = "分団長", Name = "城田敏郎", PhoneNumber = "" });
            Members.Add(new Member { Id = 2, Position = "副分団長", Name = "柘植泰史", PhoneNumber = "" });
            Members.Add(new Member { Id = 3, Position = "消防部長", Name = "鬼塚尚人", PhoneNumber = "" });
            Members.Add(new Member { Id = 4, Position = "警護部長", Name = "高木真太郎", PhoneNumber = "" });
            Members.Add(new Member { Id = 5, Position = "給水班長", Name = "小松裕也", PhoneNumber = "" });
            Members.Add(new Member { Id = 6, Position = "給水団員", Name = "吉田勇介", PhoneNumber = "" });
            Members.Add(new Member { Id = 7, Position = "給水団員", Name = "松元大悟", PhoneNumber = "" });
            Members.Add(new Member { Id = 8, Position = "機械班長", Name = "高木三成", PhoneNumber = "" });
            Members.Add(new Member { Id = 9, Position = "機械団員", Name = "中岡正樹", PhoneNumber = "" });
            Members.Add(new Member { Id = 10, Position = "機械団員", Name = "長尾雄二", PhoneNumber = "" });
            Members.Add(new Member { Id = 11, Position = "火先班長", Name = "永友雄大", PhoneNumber = "" });
            Members.Add(new Member { Id = 12, Position = "火先団員", Name = "吉村幸城", PhoneNumber = "" });
            Members.Add(new Member { Id = 13, Position = "火先団員", Name = "但馬勝兵", PhoneNumber = "" });
            Members.Add(new Member { Id = 14, Position = "救護班長", Name = "永友健聖", PhoneNumber = "" });
            Members.Add(new Member { Id = 15, Position = "救護団員", Name = "白水涼", PhoneNumber = "" });
            Members.Add(new Member { Id = 16, Position = "警交班長", Name = "寺田隼", PhoneNumber = "" });
            Members.Add(new Member { Id = 17, Position = "警交団員", Name = "円口智仁", PhoneNumber = "" });
        }
    }

    public async Task LoadDataAsync()
    {
        var documentsPath = FileSystem.AppDataDirectory;
        var membersFilePath = Path.Combine(documentsPath, "members.json");
        var eventsFilePath = Path.Combine(documentsPath, "events.json");

        try
        {
            var hasExistingData = false;

            if (File.Exists(membersFilePath))
            {
                var membersJson = await File.ReadAllTextAsync(membersFilePath);
                if (!string.IsNullOrWhiteSpace(membersJson))
                {
                    var loadedMembers = JsonSerializer.Deserialize<List<Member>>(membersJson);
                    if (loadedMembers != null && loadedMembers.Count > 0)
                    {
                        Members.Clear();
                        foreach (var member in loadedMembers)
                        {
                            Members.Add(member);
                        }
                        hasExistingData = true;
                    }
                }
            }

            if (File.Exists(eventsFilePath))
            {
                var eventsJson = await File.ReadAllTextAsync(eventsFilePath);
                if (!string.IsNullOrWhiteSpace(eventsJson))
                {
                    var loadedEvents = JsonSerializer.Deserialize<List<Event>>(eventsJson);
                    if (loadedEvents != null)
                    {
                        Events.Clear();
                        foreach (var eventItem in loadedEvents)
                        {
                            Events.Add(eventItem);
                        }
                        hasExistingData = true;
                    }
                }
            }

            if (!hasExistingData && Members.Count == 0)
            {
                InitializeDefaultData();
            }

            Events.CollectionChanged -= OnEventsChanged;
            Events.CollectionChanged += OnEventsChanged;
        }
        catch (Exception ex)
        {
            throw new Exception($"データ読み込みエラー: {ex.Message}");
        }
    }

    private void OnEventsChanged(object? sender, System.Collections.Specialized.NotifyCollectionChangedEventArgs e)
    {
        DataChanged?.Invoke(this, EventArgs.Empty);
    }

    public async Task SaveDataAsync()
    {
        var documentsPath = FileSystem.AppDataDirectory;
        var membersFilePath = Path.Combine(documentsPath, "members.json");
        var eventsFilePath = Path.Combine(documentsPath, "events.json");

        try
        {
            var options = new JsonSerializerOptions
            {
                WriteIndented = true,
                Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping
            };

            var membersJson = JsonSerializer.Serialize(Members.ToList(), options);
            await File.WriteAllTextAsync(membersFilePath, membersJson);

            var eventsJson = JsonSerializer.Serialize(Events.ToList(), options);
            await File.WriteAllTextAsync(eventsFilePath, eventsJson);

            await CreateBackupAsync(membersJson, eventsJson);
        }
        catch (Exception ex)
        {
            throw new Exception($"データ保存エラー: {ex.Message}");
        }
    }

    private async Task CreateBackupAsync(string membersJson, string eventsJson)
    {
        try
        {
            var timestamp = DateTime.Now.ToString("yyyyMMdd_HHmmss");
            var backupPath = Path.Combine(FileSystem.AppDataDirectory, "Backups");

            if (!Directory.Exists(backupPath))
            {
                Directory.CreateDirectory(backupPath);
            }

            var membersBackupPath = Path.Combine(backupPath, $"members_{timestamp}.json");
            var eventsBackupPath = Path.Combine(backupPath, $"events_{timestamp}.json");

            await File.WriteAllTextAsync(membersBackupPath, membersJson);
            await File.WriteAllTextAsync(eventsBackupPath, eventsJson);

            await CleanOldBackupsAsync(backupPath);
        }
        catch
        {
        }
    }

    private async Task CleanOldBackupsAsync(string backupPath)
    {
        await Task.Run(() =>
        {
            try
            {
                var files = Directory.GetFiles(backupPath, "*.json");
                var cutoffDate = DateTime.Now.AddDays(-30);

                foreach (var file in files)
                {
                    var fileInfo = new FileInfo(file);
                    if (fileInfo.CreationTime < cutoffDate)
                    {
                        File.Delete(file);
                    }
                }
            }
            catch
            {
            }
        });
    }

    public int GetAttendanceCount(int memberId)
    {
        return Events.Count(e => e.AttendingMemberIds.Contains(memberId));
    }

    public int GetTotalAllowanceIndex(int memberId)
    {
        return Events.Where(e => e.AttendingMemberIds.Contains(memberId))
                    .Sum(e => e.AllowanceIndex);
    }

    public void AddEvent(Event eventItem)
    {
        eventItem.Id = Events.Count > 0 ? Events.Max(e => e.Id) + 1 : 1;
        Events.Add(eventItem);
        DataChanged?.Invoke(this, EventArgs.Empty);
    }

    public void UpdateEvent(Event eventItem)
    {
        var existingEvent = Events.FirstOrDefault(e => e.Id == eventItem.Id);
        if (existingEvent != null)
        {
            existingEvent.Date = eventItem.Date;
            existingEvent.EventName = eventItem.EventName;
            existingEvent.AllowanceIndex = eventItem.AllowanceIndex;
            existingEvent.AttendingMemberIds = [.. eventItem.AttendingMemberIds];
            DataChanged?.Invoke(this, EventArgs.Empty);
        }
    }

    public void DeleteEvent(int eventId)
    {
        var eventToRemove = Events.FirstOrDefault(e => e.Id == eventId);
        if (eventToRemove != null)
        {
            Events.Remove(eventToRemove);
            DataChanged?.Invoke(this, EventArgs.Empty);
        }
    }

    public async Task<string> GetDataSummaryAsync()
    {
        await EnsureInitializedAsync();

        var totalMembers = Members.Count;
        var totalEvents = Events.Count;
        var totalAttendances = Events.SelectMany(e => e.AttendingMemberIds).Count();
        var averageAttendancePerEvent = totalEvents > 0 ? (double)totalAttendances / totalEvents : 0;

        var summary = $"データ統計\n" +
                     $"団員数: {totalMembers}名\n" +
                     $"行事数: {totalEvents}件\n" +
                     $"総出動回数: {totalAttendances}回\n" +
                     $"行事あたり平均出動: {averageAttendancePerEvent:F1}名\n\n" +
                     $"保存場所: {FileSystem.AppDataDirectory}";

        return summary;
    }

    // PDF出力用のデータ取得メソッド
    public List<Event> GetEventsSortedByDate()
    {
        return Events.OrderBy(e => e.Date).ToList();
    }

    public async Task<string> GenerateAttendanceHtmlAsync()
    {
        var members = Members.ToList();
        var events = GetEventsSortedByDate();

        var html = @"
<!DOCTYPE html>
<html>
<head>
    <meta charset='UTF-8'>
    <title>消防団行事出欠表</title>
    <style>
        body { font-family: 'MS Gothic', monospace; font-size: 12px; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #000; padding: 4px; text-align: center; }
        th { background-color: #f0f0f0; font-weight: bold; }
        .attending { background-color: #ff6b6b; color: white; }
        .member-name { font-weight: bold; }
    </style>
</head>
<body>
    <h2>消防団行事出欠表</h2>
    <table>
        <tr>
            <th>氏名</th>";

        foreach (var ev in events)
        {
            html += $"<th>{ev.Date:MM/dd}<br>{ev.EventName}<br>指数:{ev.AllowanceIndex}</th>";
        }

        html += "<th>出動回数</th><th>手当指数</th></tr>";

        foreach (var member in members)
        {
            html += $"<tr><td class='member-name'>{member.Name}</td>";

            foreach (var ev in events)
            {
                var isAttending = ev.AttendingMemberIds.Contains(member.Id);
                var cellClass = isAttending ? "attending" : "";
                var cellText = isAttending ? "出動" : "";
                html += $"<td class='{cellClass}'>{cellText}</td>";
            }

            var attendanceCount = GetAttendanceCount(member.Id);
            var totalAllowance = GetTotalAllowanceIndex(member.Id);
            html += $"<td>{attendanceCount}</td><td>{totalAllowance}</td></tr>";
        }

        html += "</table></body></html>";
        return html;
    }
}