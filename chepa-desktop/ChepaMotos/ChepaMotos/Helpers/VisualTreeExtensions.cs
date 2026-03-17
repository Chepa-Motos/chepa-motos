namespace ChepaMotos.Helpers;

public static class VisualTreeExtensions
{
    public static Page? FindParentPage(this Element element)
    {
        var current = element.Parent;
        while (current is not null)
        {
            if (current is Page page) return page;
            current = current.Parent;
        }

        return null;
    }
}