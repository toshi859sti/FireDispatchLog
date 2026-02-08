using FireDepartmentApp.Models;
using FireDepartmentApp.Services;
using FireDepartmentApp.Views;

namespace FireDepartmentApp;

public partial class AppShell : Shell
{
    private readonly DataService _dataService;
    private readonly IPdfService _pdfService;
    private Event? _selectedEvent;

    public AppShell()
    {
        InitializeComponent();
        _dataService = DataService.Instance;

        // DIコンテナからPDFサービスを取得
        _pdfService = ServiceHelper.GetService<IPdfService>();

        MembersCollectionView.ItemsSource = _dataService.Members;

        SaveDataButton.Clicked += OnSaveDataClicked;
        AddEventButton.Clicked += OnAddEventClicked;
        EditEventButton.Clicked += OnEditEventClicked;
        ExportPdfButton.Clicked += OnExportPdfClicked;

        // PDF機能が利用できない場合はボタンを非表示
        ExportPdfButton.IsVisible = _pdfService.IsSupported;

        _ = Task.Run(async () =>
        {
            await _dataService.EnsureInitializedAsync();
            MainThread.BeginInvokeOnMainThread(BuildAttendanceGrid);
        });

        _dataService.DataChanged += (s, e) => MainThread.BeginInvokeOnMainThread(BuildAttendanceGrid);
    }
    private async void OnExportPdfClicked(object? sender, EventArgs e)
    {
        if (!_pdfService.IsSupported)
        {
            await Shell.Current.DisplayAlert("機能未対応", "PDF生成機能は現在のプラットフォームでは利用できません。", "OK");
            return;
        }

        try
        {
            var result = await Shell.Current.DisplayActionSheet(
                "PDF出力",
                "キャンセル",
                null,
                "出欠表PDF",
                "団員名簿PDF");

            if (result == "キャンセル" || result == null)
                return;

            string filePath;
            if (result == "出欠表PDF")
            {
                filePath = await _pdfService.GenerateAttendancePdfAsync();
            }
            else
            {
                filePath = await _pdfService.GenerateMemberListPdfAsync();
            }

            await Shell.Current.DisplayAlert("PDF生成完了",
                $"PDFファイルが生成されました。\n\nファイル名: {Path.GetFileName(filePath)}\n場所: {FileSystem.AppDataDirectory}",
                "OK");

            await OpenFileAsync(filePath);
        }
        catch (Exception ex)
        {
            await Shell.Current.DisplayAlert("PDF生成エラー", $"PDF生成中にエラーが発生しました: {ex.Message}", "OK");
        }
    }

    private async Task OpenFileAsync(string filePath)
    {
        try
        {
            await Launcher.Default.OpenAsync(new OpenFileRequest
            {
                File = new ReadOnlyFile(filePath)
            });
        }
        catch
        {
            await Shell.Current.DisplayAlert("ファイル作成完了",
                $"PDFファイルが以下の場所に保存されました:\n{filePath}", "OK");
        }
    }

    // 以下、既存のメソッド群（変更なし）
    private async void OnSaveDataClicked(object? sender, EventArgs e)
    {
        try
        {
            await _dataService.SaveDataAsync();
            var summary = await _dataService.GetDataSummaryAsync();
            await Shell.Current.DisplayAlert("保存完了", $"データが正常に保存されました。\n\n{summary}", "OK");
        }
        catch (Exception ex)
        {
            await Shell.Current.DisplayAlert("保存エラー", $"データの保存中にエラーが発生しました:\n{ex.Message}", "OK");
        }
    }

    private async void OnAddEventClicked(object? sender, EventArgs e)
    {
        try
        {
            var eventEditPage = new EventEditPage();
            await Shell.Current.Navigation.PushModalAsync(new NavigationPage(eventEditPage));
        }
        catch (Exception ex)
        {
            await Shell.Current.DisplayAlert("エラー", $"画面を開くことができませんでした: {ex.Message}", "OK");
        }
    }

    private async void OnEditEventClicked(object? sender, EventArgs e)
    {
        try
        {
            if (_selectedEvent != null)
            {
                var eventEditPage = new EventEditPage(_selectedEvent);
                await Shell.Current.Navigation.PushModalAsync(new NavigationPage(eventEditPage));
            }
            else
            {
                await Shell.Current.DisplayAlert("選択エラー", "編集する行事を選択してください。\n行事のヘッダーをタップして選択してから、もう一度お試しください。", "OK");
            }
        }
        catch (Exception ex)
        {
            await Shell.Current.DisplayAlert("エラー", $"編集画面を開くことができませんでした: {ex.Message}", "OK");
        }
    }

    // 既存のBuildAttendanceGrid等のメソッドは変更なし（前回のコードを使用）
    private void BuildAttendanceGrid()
    {
        try
        {
            var members = _dataService.Members.ToList();
            var events = _dataService.GetEventsSortedByDate();

            if (members.Count == 0) return;

            BuildNamesGrid(members);
            BuildEventsGrid(members, events);
            BuildCountGrid(members);
            BuildAllowanceGrid(members);
        }
        catch (Exception ex)
        {
            MainThread.BeginInvokeOnMainThread(async () =>
            {
                await Shell.Current.DisplayAlert("エラー", $"出欠表の構築中にエラーが発生しました: {ex.Message}", "OK");
            });
        }
    }
    private void BuildEventsGrid(List<Member> members, List<Event> events)
    {
        EventsGrid.Children.Clear();
        EventsGrid.RowDefinitions.Clear();
        EventsGrid.ColumnDefinitions.Clear();

        if (events.Count == 0)
        {
            var noEventsLabel = new Label
            {
                Text = "「追加」ボタンから\n行事を追加してください",
                HorizontalOptions = LayoutOptions.Center,
                VerticalOptions = LayoutOptions.Center,
                TextColor = Colors.Gray,
                FontSize = 16,
                HorizontalTextAlignment = TextAlignment.Center,
                FontAttributes = FontAttributes.Italic,
                Margin = new Thickness(5)
            };
            EventsGrid.Children.Add(noEventsLabel);
            return;
        }

        // カラム定義
        foreach (var ev in events)
        {
            EventsGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(80, GridUnitType.Absolute) });
        }

        // 行定義
        EventsGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(50, GridUnitType.Absolute) });
        foreach (var member in members)
        {
            EventsGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(40, GridUnitType.Absolute) });
        }

        // ヘッダー行の構築（手当指数も表示）
        int colIndex = 0;
        foreach (var ev in events)
        {
            var eventButton = new Button
            {
                Text = $"{ev.Date:MM/dd}[{ev.AllowanceIndex}] \n{ev.EventName}",
                BackgroundColor = _selectedEvent?.Id == ev.Id
                    ? Color.FromArgb("#FF9F43")
                    : Color.FromArgb("#4ECDC4"),
                TextColor = Colors.White,
                FontSize = 14,
                FontAttributes = FontAttributes.Bold,
                CornerRadius = 8,
                Margin = new Thickness(2),
                Padding = new Thickness(4),
                LineBreakMode = LineBreakMode.WordWrap
            };

            var currentEvent = ev;
            eventButton.Clicked += (s, e) => OnEventHeaderClicked(currentEvent);

            EventsGrid.Children.Add(eventButton);
            Grid.SetColumn(eventButton, colIndex);
            Grid.SetRow(eventButton, 0);
            colIndex++;
        }

        // データ行の構築
        int rowIndex = 1;
        foreach (var member in members)
        {
            colIndex = 0;
            foreach (var ev in events)
            {
                var isAttending = ev.AttendingMemberIds.Contains(member.Id);
                var attendanceLabel = new Label
                {
                    Text = isAttending ? "出動" : "",
                    BackgroundColor = isAttending ? Color.FromArgb("#FF6B6B") : Colors.White,
                    TextColor = isAttending ? Colors.White : Colors.Black,
                    FontSize = 14,
                    FontAttributes = isAttending ? FontAttributes.Bold : FontAttributes.None,
                    HorizontalOptions = LayoutOptions.Fill,
                    VerticalOptions = LayoutOptions.Fill,
                    HorizontalTextAlignment = TextAlignment.Center,
                    VerticalTextAlignment = TextAlignment.Center
                };

                var border = new Border
                {
                    Content = attendanceLabel,
                    Stroke = Color.FromArgb("#DDD"),
                    StrokeThickness = 1,
                    Margin = new Thickness(1),
                    BackgroundColor = attendanceLabel.BackgroundColor
                };

                EventsGrid.Children.Add(border);
                Grid.SetColumn(border, colIndex);
                Grid.SetRow(border, rowIndex);
                colIndex++;
            }
            rowIndex++;
        }
    }

    private void BuildCountGrid(List<Member> members)
    {
        CountGrid.Children.Clear();
        CountGrid.RowDefinitions.Clear();

        // ヘッダー行
        CountGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(50, GridUnitType.Absolute) });
        var countHeader = CreateFixedHeaderLabel("出動\n回数");
        CountGrid.Children.Add(countHeader);
        Grid.SetRow(countHeader, 0);

        // データ行
        int rowIndex = 1;
        foreach (var member in members)
        {
            CountGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(40, GridUnitType.Absolute) });
            var attendanceCount = _dataService.GetAttendanceCount(member.Id);
            var countLabel = CreateFixedDataLabel(attendanceCount.ToString());
            CountGrid.Children.Add(countLabel);
            Grid.SetRow(countLabel, rowIndex);
            rowIndex++;
        }
    }

    private void BuildAllowanceGrid(List<Member> members)
    {
        AllowanceGrid.Children.Clear();
        AllowanceGrid.RowDefinitions.Clear();

        // ヘッダー行
        AllowanceGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(50, GridUnitType.Absolute) });
        var allowanceHeader = CreateFixedHeaderLabel("手当\n指数");
        AllowanceGrid.Children.Add(allowanceHeader);
        Grid.SetRow(allowanceHeader, 0);

        // データ行
        int rowIndex = 1;
        foreach (var member in members)
        {
            AllowanceGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(40, GridUnitType.Absolute) });
            var totalAllowance = _dataService.GetTotalAllowanceIndex(member.Id);
            var allowanceLabel = CreateFixedDataLabel(totalAllowance.ToString());
            AllowanceGrid.Children.Add(allowanceLabel);
            Grid.SetRow(allowanceLabel, rowIndex);
            rowIndex++;
        }
    }

    private Label CreateFixedHeaderLabel(string text)
    {
        return new Label
        {
            Text = text,
            BackgroundColor = Color.FromArgb("#4ECDC4"),
            TextColor = Colors.White,
            FontAttributes = FontAttributes.Bold,
            FontSize = 14,
            HorizontalOptions = LayoutOptions.Fill,
            VerticalOptions = LayoutOptions.Fill,
            HorizontalTextAlignment = TextAlignment.Center,
            VerticalTextAlignment = TextAlignment.Center,
            Padding = new Thickness(6),
            LineBreakMode = LineBreakMode.WordWrap
        };
    }

    private Border CreateFixedDataLabel(string text)
    {
        var label = new Label
        {
            Text = text,
            BackgroundColor = Color.FromArgb("#F8F9FA"),
            TextColor = Color.FromArgb("#2C3E50"),
            FontSize = 14,
            FontAttributes = FontAttributes.Bold,
            HorizontalOptions = LayoutOptions.Fill,
            VerticalOptions = LayoutOptions.Fill,
            HorizontalTextAlignment = TextAlignment.Center,
            VerticalTextAlignment = TextAlignment.Center
        };

        return new Border
        {
            Content = label,
            Stroke = Color.FromArgb("#DDD"),
            StrokeThickness = 1,
            Margin = new Thickness(1),
            BackgroundColor = Color.FromArgb("#F8F9FA")
        };
    }

    private void OnEventHeaderClicked(Event eventItem)
    {
        try
        {
            _selectedEvent = eventItem;
            BuildAttendanceGrid();

            var attendingCount = eventItem.AttendingMemberIds.Count;
            var totalMembers = _dataService.Members.Count;

            MainThread.BeginInvokeOnMainThread(async () =>
            {
                await Shell.Current.DisplayAlert("行事選択",
                    $"行事: {eventItem.EventName}\n" +
                    $"日付: {eventItem.Date:yyyy年MM月dd日}\n" +
                    $"手当指数: {eventItem.AllowanceIndex}\n" +
                    $"出動予定: {attendingCount}/{totalMembers}名\n\n" +
                    $"「編集」ボタンで編集できます。", "OK");
            });
        }
        catch (Exception ex)
        {
            MainThread.BeginInvokeOnMainThread(async () =>
            {
                await Shell.Current.DisplayAlert("エラー", $"行事選択中にエラーが発生しました: {ex.Message}", "OK");
            });
        }
    }

    protected override void OnNavigated(ShellNavigatedEventArgs args)
    {
        base.OnNavigated(args);
        BuildAttendanceGrid();
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        BuildAttendanceGrid();
    }
}
