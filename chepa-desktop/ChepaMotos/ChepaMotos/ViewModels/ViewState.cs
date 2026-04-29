namespace ChepaMotos.ViewModels;

/// <summary>
/// Estado mutuamente excluyente de una vista. Garantiza que el XAML solo
/// muestre uno de spinner / banner de error / empty state / data.
///
/// La regla práctica:
/// <list type="bullet">
///   <item><see cref="Loading"/> y <see cref="Error"/> solo aplican en la
///         primera carga (cuando aún no hay datos pintados).</item>
///   <item>En recargas posteriores (con datos pintados), siempre se queda en
///         <see cref="Loaded"/> o <see cref="Empty"/> aunque haya un error
///         de recarga; el botón "↻" basta como feedback de que algo falló.</item>
/// </list>
/// </summary>
public enum ViewState
{
    /// <summary>Primera carga en curso, todavía no hay datos visibles.</summary>
    Loading,

    /// <summary>Primera carga falló. Banner bloqueante con botón "Reintentar".</summary>
    Error,

    /// <summary>Carga exitosa pero la colección quedó vacía.</summary>
    Empty,

    /// <summary>Hay datos para mostrar. Estado por defecto tras éxito.</summary>
    Loaded,
}
