using System.Collections.Concurrent;
using Microsoft.Extensions.Logging;

namespace ChepaMotos.Services.Logging;

/// <summary>
/// Logger casero a archivo plano. Escribe a
/// <c>%LOCALAPPDATA%\ChepaMotos\logs\chepa-{yyyyMMdd}.log</c> con rotación
/// diaria automática. Append-only, thread-safe via lock.
///
/// Suficiente para diagnosticar bugs en producción sin levantar Visual Studio:
/// el cliente abre la carpeta y manda el archivo del día. Si crece el volumen
/// de logs, mejor migrar a Serilog con buffering + flushing periódico.
/// </summary>
public sealed class FileLoggerProvider : ILoggerProvider
{
    private readonly string _logDirectory;
    private readonly ConcurrentDictionary<string, FileLogger> _loggers = new();
    private readonly object _writeLock = new();

    public FileLoggerProvider(string logDirectory)
    {
        _logDirectory = logDirectory;
        try
        {
            Directory.CreateDirectory(_logDirectory);
        }
        catch
        {
            // Si no podemos crear el directorio (permisos, disco lleno), el
            // WriteLine va a swallow individualmente — la app no se rompe.
        }
    }

    public ILogger CreateLogger(string categoryName)
        => _loggers.GetOrAdd(categoryName, name => new FileLogger(name, this));

    internal void WriteLine(string line)
    {
        try
        {
            var path = Path.Combine(_logDirectory, $"chepa-{DateTime.Now:yyyyMMdd}.log");
            lock (_writeLock)
            {
                File.AppendAllText(path, line + Environment.NewLine);
            }
        }
        catch
        {
            // El logging no debe romper la app — si el disco está lleno, los
            // permisos cambiaron, etc., simplemente perdemos esta línea.
        }
    }

    public void Dispose()
    {
        // Sin recursos persistentes (lock interno por escritura).
        _loggers.Clear();
    }
}

internal sealed class FileLogger : ILogger
{
    private readonly string _category;
    private readonly FileLoggerProvider _provider;

    public FileLogger(string category, FileLoggerProvider provider)
    {
        _category = category;
        _provider = provider;
    }

    public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

    public bool IsEnabled(LogLevel level) => level >= LogLevel.Information;

    public void Log<TState>(
        LogLevel level,
        EventId eventId,
        TState state,
        Exception? exception,
        Func<TState, Exception?, string> formatter)
    {
        if (!IsEnabled(level)) return;

        var message = formatter(state, exception);
        var line = $"{DateTime.Now:yyyy-MM-dd HH:mm:ss.fff} [{LevelTag(level)}] [{_category}] {message}";
        if (exception is not null)
            line += Environment.NewLine + exception;

        _provider.WriteLine(line);
    }

    private static string LevelTag(LogLevel level) => level switch
    {
        LogLevel.Trace => "TRC",
        LogLevel.Debug => "DBG",
        LogLevel.Information => "INF",
        LogLevel.Warning => "WRN",
        LogLevel.Error => "ERR",
        LogLevel.Critical => "CRT",
        _ => "???",
    };
}
