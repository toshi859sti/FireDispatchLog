using FireDepartmentApp.Models;
using FireDepartmentApp.Services;

namespace FireDepartmentApp.Views;

public partial class EventEditPage : ContentPage
{
    private readonly DataService _dataService;
    private readonly Event? _editingEvent;
    private readonly bool _isEditMode;
    private readonly Dictionary<int, Button> _memberButtons;

    public EventEditPage(Event? eventToEdit = null)
    {
        InitializeComponent();
        _dataService = DataService.Instance;
        _editingEvent = eventToEdit;
        _isEditMode = eventToEdit != null;
        _memberButtons = [];

        InitializePage();
        SetupEventHandlers();
    }

    private void InitializePage()
    {
        if (_isEditMode && _editingEvent != null)
        {
            DatePicker.Date = _editingEvent.Date;
            EventNameEntry.Text = _editingEvent.EventName;
            AllowanceIndexEntry.Text = _editingEvent.AllowanceIndex.ToString();
            DeleteButton.IsVisible = true;
            Title = "行事編集";
        }
        else
        {
            DatePicker.Date = DateTime.Today;
            DeleteButton.IsVisible = false;
            Title = "行事追加";
        }

        CreateMemberGrid();
    }

    private void SetupEventHandlers()
    {
        SaveButton.Clicked += OnSaveClicked;
        CancelButton.Clicked += OnCancelClicked;
        DeleteButton.Clicked += OnDeleteClicked;
    }

    private void CreateMemberGrid()
    {
        MembersGrid.Children.Clear();
        MembersGrid.RowDefinitions.Clear();
        _memberButtons.Clear();

        var members = _dataService.Members.ToList();
        int columnsPerRow = 4;
        int totalRows = (int)Math.Ceiling((double)members.Count / columnsPerRow);

        // 行定義を追加
        for (int i = 0; i < totalRows; i++)
        {
            MembersGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        }

        // 団員ボタンを配置
        for (int i = 0; i < members.Count; i++)
        {
            var member = members[i];
            var row = i / columnsPerRow;
            var col = i % columnsPerRow;

            var isAttending = _isEditMode && _editingEvent?.AttendingMemberIds.Contains(member.Id) == true;

            var button = new Button
            {
                Text = member.Name,
                FontSize = 12,
                CornerRadius = 12,
                Padding = new Thickness(8, 12),
                Margin = new Thickness(3),
                BackgroundColor = isAttending ? Color.FromArgb("#FF6B6B") : Color.FromArgb("#F8F9FA"),
                TextColor = isAttending ? Colors.White : Color.FromArgb("#2C3E50"),
                BorderColor = Color.FromArgb("#DDD"),
                BorderWidth = 1,
                FontAttributes = isAttending ? FontAttributes.Bold : FontAttributes.None
            };

            var memberId = member.Id;
            button.Clicked += (s, e) => OnMemberButtonClicked(memberId);

            _memberButtons[member.Id] = button;

            MembersGrid.Children.Add(button);
            Grid.SetRow(button, row);
            Grid.SetColumn(button, col);
        }
    }

    private void OnMemberButtonClicked(int memberId)
    {
        if (_memberButtons.TryGetValue(memberId, out var button))
        {
            // 現在の状態を反転
            var isCurrentlyAttending = button.BackgroundColor.Equals(Color.FromArgb("#FF6B6B"));

            if (isCurrentlyAttending)
            {
                // 出動 → 欠席
                button.BackgroundColor = Color.FromArgb("#F8F9FA");
                button.TextColor = Color.FromArgb("#2C3E50");
                button.FontAttributes = FontAttributes.None;
            }
            else
            {
                // 欠席 → 出動
                button.BackgroundColor = Color.FromArgb("#FF6B6B");
                button.TextColor = Colors.White;
                button.FontAttributes = FontAttributes.Bold;
            }

            // 軽微なアニメーション効果
            AnimateButton(button);
        }
    }

    private async void AnimateButton(Button button)
    {
        await button.ScaleTo(0.95, 100);
        await button.ScaleTo(1.0, 100);
    }

    private async void OnSaveClicked(object? sender, EventArgs e)
    {
        // 入力検証
        if (string.IsNullOrWhiteSpace(EventNameEntry.Text))
        {
            await DisplayAlert("入力エラー", "行事名を入力してください", "OK");
            EventNameEntry.Focus();
            return;
        }

        if (!int.TryParse(AllowanceIndexEntry.Text, out int allowanceIndex) || allowanceIndex < 0)
        {
            await DisplayAlert("入力エラー", "手当指数は0以上の数値で入力してください", "OK");
            AllowanceIndexEntry.Focus();
            return;
        }

        // 出動団員IDを収集
        var attendingMemberIds = new List<int>();
        foreach (var kvp in _memberButtons)
        {
            if (kvp.Value.BackgroundColor.Equals(Color.FromArgb("#FF6B6B")))
            {
                attendingMemberIds.Add(kvp.Key);
            }
        }

        try
        {
            // イベントオブジェクトを作成または更新
            if (_isEditMode && _editingEvent != null)
            {
                _editingEvent.Date = DatePicker.Date;
                _editingEvent.EventName = EventNameEntry.Text.Trim();
                _editingEvent.AllowanceIndex = allowanceIndex;
                _editingEvent.AttendingMemberIds = attendingMemberIds;

                _dataService.UpdateEvent(_editingEvent);

                await DisplayAlert("更新完了", "行事情報が更新されました", "OK");
            }
            else
            {
                var newEvent = new Event
                {
                    Date = DatePicker.Date,
                    EventName = EventNameEntry.Text.Trim(),
                    AllowanceIndex = allowanceIndex,
                    AttendingMemberIds = attendingMemberIds
                };

                _dataService.AddEvent(newEvent);

                await DisplayAlert("追加完了", "新しい行事が追加されました", "OK");
            }

            await Navigation.PopModalAsync();
        }
        catch (Exception ex)
        {
            await DisplayAlert("エラー", $"保存中にエラーが発生しました: {ex.Message}", "OK");
        }
    }

    private async void OnCancelClicked(object? sender, EventArgs e)
    {
        // 変更がある場合は確認ダイアログを表示
        if (HasUnsavedChanges())
        {
            var result = await DisplayAlert("確認", "変更を破棄してもよろしいですか？", "破棄", "戻る");
            if (!result)
                return;
        }

        await Navigation.PopModalAsync();
    }

    private async void OnDeleteClicked(object? sender, EventArgs e)
    {
        if (!_isEditMode || _editingEvent == null) return;

        var result = await DisplayAlert(
            "削除確認",
            $"行事「{_editingEvent.EventName}」を削除してもよろしいですか？\n\nこの操作は取り消せません。",
            "削除",
            "キャンセル");

        if (result)
        {
            try
            {
                _dataService.DeleteEvent(_editingEvent.Id);
                await DisplayAlert("削除完了", "行事が削除されました", "OK");
                await Navigation.PopModalAsync();
            }
            catch (Exception ex)
            {
                await DisplayAlert("エラー", $"削除中にエラーが発生しました: {ex.Message}", "OK");
            }
        }
    }

    private bool HasUnsavedChanges()
    {
        if (!_isEditMode)
        {
            // 新規追加の場合、何か入力があるかチェック
            return !string.IsNullOrWhiteSpace(EventNameEntry.Text) ||
                   !string.IsNullOrWhiteSpace(AllowanceIndexEntry.Text) ||
                   DatePicker.Date != DateTime.Today ||
                   _memberButtons.Values.Any(b => b.BackgroundColor.Equals(Color.FromArgb("#FF6B6B")));
        }
        else if (_editingEvent != null)
        {
            // 編集の場合、元のデータと比較
            var currentAttendingIds = _memberButtons.Where(kvp => kvp.Value.BackgroundColor.Equals(Color.FromArgb("#FF6B6B")))
                                                   .Select(kvp => kvp.Key)
                                                   .OrderBy(id => id)
                                                   .ToList();

            var originalAttendingIds = _editingEvent.AttendingMemberIds.OrderBy(id => id).ToList();

            return DatePicker.Date != _editingEvent.Date ||
                   EventNameEntry.Text?.Trim() != _editingEvent.EventName ||
                   AllowanceIndexEntry.Text != _editingEvent.AllowanceIndex.ToString() ||
                   !currentAttendingIds.SequenceEqual(originalAttendingIds);
        }

        return false;
    }
}