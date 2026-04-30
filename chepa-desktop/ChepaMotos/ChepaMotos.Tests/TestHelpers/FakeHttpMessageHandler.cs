using System.Net;
using System.Text;

namespace ChepaMotos.Tests.TestHelpers;

/// <summary>
/// Handler para mockear <see cref="HttpClient"/>. Las respuestas se encolan
/// en orden y se entregan FIFO. Si se encola una excepción, se lanza al
/// llegar su turno (sirve para simular caídas de red o timeouts).
/// </summary>
public sealed class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly Queue<Func<HttpRequestMessage, HttpResponseMessage>> _responses = new();

    /// <summary>Lista de requests recibidas para hacer aserciones (URL, body, headers).</summary>
    public List<HttpRequestMessage> Requests { get; } = new();

    public void EnqueueJson(HttpStatusCode statusCode, string json)
    {
        _responses.Enqueue(_ => new HttpResponseMessage(statusCode)
        {
            Content = new StringContent(json, Encoding.UTF8, "application/json"),
        });
    }

    public void EnqueueException(Exception ex)
    {
        _responses.Enqueue(_ => throw ex);
    }

    protected override Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        Requests.Add(request);
        if (_responses.Count == 0)
            throw new InvalidOperationException(
                $"No hay respuesta encolada para {request.Method} {request.RequestUri}");

        var producer = _responses.Dequeue();
        return Task.FromResult(producer(request));
    }
}

/// <summary>
/// <see cref="IHttpClientFactory"/> que devuelve siempre el mismo HttpClient
/// envolviendo el handler dado. El nombre del cliente se ignora.
/// </summary>
public sealed class FakeHttpClientFactory : IHttpClientFactory
{
    private readonly HttpClient _client;

    public FakeHttpClientFactory(FakeHttpMessageHandler handler, string baseUrl = "http://test.local/api/")
    {
        _client = new HttpClient(handler)
        {
            BaseAddress = new Uri(baseUrl),
        };
    }

    public HttpClient CreateClient(string name) => _client;
}
