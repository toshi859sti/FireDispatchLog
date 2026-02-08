namespace FireDepartmentApp.Services;

public class UnsupportedPdfService : IPdfService
{
    public bool IsSupported => false;

    public Task<string> GenerateAttendancePdfAsync()
    {
        throw new PlatformNotSupportedException("PDF生成はAndroidプラットフォームでのみ対応しています。");
    }

    public Task<string> GenerateMemberListPdfAsync()
    {
        throw new PlatformNotSupportedException("PDF生成はAndroidプラットフォームでのみ対応しています。");
    }
}