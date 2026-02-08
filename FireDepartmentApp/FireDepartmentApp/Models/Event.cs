using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace FireDepartmentApp.Models;

public class Event : INotifyPropertyChanged
{
    private DateTime _date = DateTime.Today;
    private string _eventName = string.Empty;
    private int _allowanceIndex;

    public int Id { get; set; }

    public DateTime Date
    {
        get => _date;
        set
        {
            _date = value;
            OnPropertyChanged();
        }
    }

    public string EventName
    {
        get => _eventName;
        set
        {
            _eventName = value ?? string.Empty;
            OnPropertyChanged();
        }
    }

    public int AllowanceIndex
    {
        get => _allowanceIndex;
        set
        {
            _allowanceIndex = value;
            OnPropertyChanged();
        }
    }

    public List<int> AttendingMemberIds { get; set; } = [];

    public event PropertyChangedEventHandler? PropertyChanged;

    protected void OnPropertyChanged([CallerMemberName] string? propertyName = null)
    {
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }
}