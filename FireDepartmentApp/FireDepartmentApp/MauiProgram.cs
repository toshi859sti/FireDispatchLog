using FireDepartmentApp.Services;
using Microsoft.Extensions.Logging;

namespace FireDepartmentApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

        // PDFサービスの登録
#if ANDROID
        builder.Services.AddSingleton<IPdfService, Platforms.Android.AndroidPdfService>();
#else
        builder.Services.AddSingleton<IPdfService, DefaultPdfService>();
#endif

#if DEBUG
        builder.Services.AddLogging(logging =>
        {
            logging.AddDebug();
        });
#endif

        return builder.Build();
    }
}