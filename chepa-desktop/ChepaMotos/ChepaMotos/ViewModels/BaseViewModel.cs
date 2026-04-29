using CommunityToolkit.Mvvm.ComponentModel;

namespace ChepaMotos.ViewModels;

/// <summary>
/// Base para los VMs con operaciones async cancelables al desnavegar.
///
/// Cada VM mantiene un único <see cref="CancellationTokenSource"/> que la View
/// cancela cuando el handler se vuelve null (ya no está montada en el árbol
/// visual). Esto evita dos clases de problemas:
/// <list type="bullet">
///   <item>requests que terminan después de salir de la pantalla y pisan UI
///         que el usuario ya no está mirando;</item>
///   <item>tráfico HTTP innecesario que ya no le sirve a nadie.</item>
/// </list>
///
/// En la <b>Fase 4</b> se expandirá con <c>IsBusy</c>, <c>LoadError</c> y
/// <c>ShowSkeleton</c> compartidos entre VMs.
/// </summary>
public abstract class BaseViewModel : ObservableObject, IDisposable
{
    private CancellationTokenSource? _cts;

    /// <summary>
    /// Devuelve un token vivo para la operación. Si el anterior fue cancelado
    /// (por desnavegación), crea uno nuevo enlazado al token externo recibido.
    /// Llama a esto al inicio de cada operación async.
    /// </summary>
    protected CancellationToken EnsureCancellationToken(CancellationToken external = default)
    {
        if (_cts is null || _cts.IsCancellationRequested)
        {
            _cts?.Dispose();
            _cts = CancellationTokenSource.CreateLinkedTokenSource(external);
        }
        return _cts.Token;
    }

    /// <summary>
    /// Cancela cualquier operación en vuelo. La View debe invocarlo desde
    /// <c>OnHandlerChanged</c> cuando el handler vuelve a null para evitar
    /// que requests pendientes actualicen UI ya desmontada.
    /// </summary>
    public void CancelOngoingOperation()
    {
        try
        {
            _cts?.Cancel();
        }
        catch (ObjectDisposedException)
        {
            // CTS ya disposed por Dispose() — nada que cancelar.
        }
    }

    public void Dispose()
    {
        _cts?.Cancel();
        _cts?.Dispose();
        _cts = null;
        GC.SuppressFinalize(this);
    }
}
