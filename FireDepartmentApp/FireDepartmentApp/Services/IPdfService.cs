namespace FireDepartmentApp.Services;

public interface IPdfService
{
    Task<string> GenerateAttendancePdfAsync();
    Task<string> GenerateMemberListPdfAsync();
    bool IsSupported { get; }
}