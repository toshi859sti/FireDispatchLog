namespace FireDepartmentApp.Services;

public static class PdfServiceFactory
{
    public static IPdfService CreatePdfService()
    {
#if ANDROID
        return new AndroidPdfService();
#else
        return new UnsupportedPdfService();
#endif
    }
}