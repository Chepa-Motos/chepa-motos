namespace ChepaMotos.Helpers;

/// <summary>
/// Marca una View que sabe recargarse a sí misma sin perder estado local
/// (filtros aplicados, fecha seleccionada, scroll, etc.).
///
/// Lo implementan las 4 views principales del sidebar para que <c>MainLayout</c>
/// pueda invocarlas tras crear/cancelar facturas en lugar de reconstruir la
/// view entera (que perdería todo).
/// </summary>
public interface IRefreshable
{
    Task RefreshAsync();
}
