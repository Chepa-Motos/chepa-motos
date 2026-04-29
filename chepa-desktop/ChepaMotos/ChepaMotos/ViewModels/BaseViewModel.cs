using ChepaMotos.Models;
using CommunityToolkit.Mvvm.ComponentModel;

namespace ChepaMotos.ViewModels;

/// <summary>
/// Base para todos los VMs principales. Centraliza:
/// <list type="bullet">
///   <item><see cref="IsBusy"/> / <see cref="LoadError"/> / <see cref="ShowSkeleton"/>
///         para los estados de carga.</item>
///   <item>Cancelación cooperativa por desnavegación
///         (<see cref="CancelOngoingOperation"/>).</item>
///   <item><see cref="ExecuteLoadAsync"/> que envuelve el patrón try/catch
///         común y traduce excepciones a <see cref="LoadError"/>.</item>
/// </list>
///
/// El <see cref="LoginViewModel"/> no hereda de aquí porque su modelo de error
/// es distinto (banner inline, no banner de carga).
/// </summary>
public abstract partial class BaseViewModel : ObservableObject, IDisposable
{
    private CancellationTokenSource? _cts;
    private bool _hasLoadedOnce;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsNotBusy))]
    [NotifyPropertyChangedFor(nameof(ShowSkeleton))]
    private bool _isBusy;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(HasLoadError))]
    [NotifyPropertyChangedFor(nameof(ShowSkeleton))]
    private string? _loadError;

    public bool IsNotBusy => !IsBusy;
    public bool HasLoadError => !string.IsNullOrEmpty(LoadError);

    /// <summary>
    /// Solo mostramos el skeleton/spinner durante la primera carga; en recargas
    /// posteriores el contenido ya está pintado y un overlay es ruidoso.
    /// </summary>
    public bool ShowSkeleton => IsBusy && !_hasLoadedOnce && !HasLoadError;

    /// <summary>
    /// Devuelve un token vivo para la operación. Si el anterior fue cancelado
    /// (por desnavegación), crea uno nuevo enlazado al token externo recibido.
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
    /// <c>OnHandlerChanged</c> cuando el handler vuelve a null.
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

    /// <summary>
    /// Envuelve el patrón típico de carga: pone <see cref="IsBusy"/>=true,
    /// limpia <see cref="LoadError"/>, ejecuta <paramref name="operation"/> con
    /// un token cancelable, traduce excepciones a <see cref="LoadError"/> con
    /// mensajes en español, y al final restaura <see cref="IsBusy"/>.
    ///
    /// Si <see cref="IsBusy"/> ya está activo cuando se llama, la operación se
    /// salta (evita que el botón "↻" dispare cargas concurrentes).
    /// </summary>
    /// <returns><c>true</c> si la operación completó con éxito; <c>false</c> si
    /// hubo error o cancelación.</returns>
    protected async Task<bool> ExecuteLoadAsync(
        Func<CancellationToken, Task> operation,
        CancellationToken external = default)
    {
        if (IsBusy) return false;

        var token = EnsureCancellationToken(external);
        IsBusy = true;
        LoadError = null;

        try
        {
            await operation(token);
            _hasLoadedOnce = true;
            return true;
        }
        catch (OperationCanceledException) when (token.IsCancellationRequested)
        {
            return false;
        }
        catch (ApiException ex)
        {
            LoadError = ex.Message;
            return false;
        }
        catch (HttpRequestException)
        {
            LoadError = "No se pudo conectar al servidor. Verifica que esté encendido.";
            return false;
        }
        catch (TaskCanceledException)
        {
            LoadError = "El servidor tardó demasiado en responder";
            return false;
        }
        finally
        {
            IsBusy = false;
            OnPropertyChanged(nameof(ShowSkeleton));
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
