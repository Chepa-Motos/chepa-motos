using ChepaMotos.Models;
using CommunityToolkit.Mvvm.ComponentModel;

namespace ChepaMotos.ViewModels;

/// <summary>
/// Base para todos los VMs principales. Centraliza:
/// <list type="bullet">
///   <item><see cref="IsBusy"/> / <see cref="LoadError"/> / <see cref="IsCollectionEmpty"/>
///         como inputs.</item>
///   <item><see cref="State"/> + <see cref="IsLoading"/>/<see cref="IsError"/>/<see cref="IsEmpty"/>/<see cref="IsLoaded"/>
///         como estados mutuamente excluyentes para el XAML — solo uno es
///         verdadero a la vez, así nunca se ven dos overlays simultáneos.</item>
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
    [NotifyPropertyChangedFor(nameof(State))]
    [NotifyPropertyChangedFor(nameof(IsLoading))]
    [NotifyPropertyChangedFor(nameof(IsError))]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsLoaded))]
    private bool _isBusy;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(HasLoadError))]
    [NotifyPropertyChangedFor(nameof(State))]
    [NotifyPropertyChangedFor(nameof(IsLoading))]
    [NotifyPropertyChangedFor(nameof(IsError))]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsLoaded))]
    private string? _loadError;

    /// <summary>Las subclases setean esto al refrescar su colección de datos.</summary>
    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(State))]
    [NotifyPropertyChangedFor(nameof(IsLoading))]
    [NotifyPropertyChangedFor(nameof(IsError))]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsLoaded))]
    private bool _isCollectionEmpty;

    public bool IsNotBusy => !IsBusy;
    public bool HasLoadError => !string.IsNullOrEmpty(LoadError);

    /// <summary>
    /// Estado mutuamente excluyente. <c>Loading</c> y <c>Error</c> solo aplican
    /// en la primera carga; en recargas siempre quedamos en <c>Loaded</c> o
    /// <c>Empty</c> aunque haya un error de recarga (los datos viejos siguen
    /// visibles, el botón "↻" comunica que algo falló).
    /// </summary>
    public ViewState State
    {
        get
        {
            if (!_hasLoadedOnce)
            {
                if (HasLoadError) return ViewState.Error;
                if (IsBusy) return ViewState.Loading;
            }
            return IsCollectionEmpty ? ViewState.Empty : ViewState.Loaded;
        }
    }

    public bool IsLoading => State == ViewState.Loading;
    public bool IsError => State == ViewState.Error;
    public bool IsEmpty => State == ViewState.Empty;
    public bool IsLoaded => State == ViewState.Loaded;

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
            // _hasLoadedOnce no es ObservableProperty, así que renotificamos
            // los estados derivados manualmente — al pasar de "primera carga"
            // a "ya hay datos", la matriz cambia.
            NotifyStateRecomputed();
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
        }
    }

    private void NotifyStateRecomputed()
    {
        OnPropertyChanged(nameof(State));
        OnPropertyChanged(nameof(IsLoading));
        OnPropertyChanged(nameof(IsError));
        OnPropertyChanged(nameof(IsEmpty));
        OnPropertyChanged(nameof(IsLoaded));
    }

    public void Dispose()
    {
        _cts?.Cancel();
        _cts?.Dispose();
        _cts = null;
        GC.SuppressFinalize(this);
    }
}
