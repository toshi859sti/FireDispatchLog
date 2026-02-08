using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace FireDepartmentApp.Models;

public class Member : INotifyPropertyChanged
{
    private string _position = string.Empty;
    private string _name = string.Empty;
    private string _phoneNumber = string.Empty;

    public int Id { get; set; }

    public string Position
    {
        get => _position;
        set
        {
            _position = value ?? string.Empty;
            OnPropertyChanged();
        }
    }

    public string Name
    {
        get => _name;
        set
        {
            _name = value ?? string.Empty;
            OnPropertyChanged();
        }
    }

    public string PhoneNumber
    {
        get => _phoneNumber;
        set
        {
            _phoneNumber = value ?? string.Empty;
            OnPropertyChanged();
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;

    protected void OnPropertyChanged([CallerMemberName] string? propertyName = null)
    {
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }
}