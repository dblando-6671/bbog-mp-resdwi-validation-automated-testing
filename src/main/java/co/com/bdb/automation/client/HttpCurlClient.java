package co.com.bdb.automation.client;

import co.com.bdb.automation.model.CredencialesAws;
import co.com.bdb.automation.model.PeticionCurl;
import co.com.bdb.automation.respuesta.RespuestaHttp;
import co.com.bdb.automation.service.AwsSigV4Signer;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/** Envia una PeticionCurl con el HttpClient de Java (no necesita curl instalado). */
public class HttpCurlClient {

    /** Encabezados que el HttpClient de Java calcula solo y no deja fijar. */
    private static final Set<String> RESTRINGIDOS = Set.of("host", "connection", "content-length", "expect", "upgrade");
    private static final Pattern CHARSET = Pattern.compile("(?i)charset=\"?([^;\" ]+)");

    private final Configuracion config;
    private final AwsSigV4Signer firmador = new AwsSigV4Signer();
    private Map<String, String> ultimosEncabezadosAgregados = new LinkedHashMap<>();

    public HttpCurlClient(Configuracion config) {
        this.config = config;
    }

    public RespuestaHttp enviar(PeticionCurl p) {
        URI uri = URI.create(p.getUrl());
        String metodo = p.getMetodoEfectivo();
        byte[] cuerpo = p.getCuerpo() == null ? null : p.getCuerpo().getBytes(StandardCharsets.UTF_8);
        int timeout = p.getTimeoutSegundos() != null ? p.getTimeoutSegundos()
                : config.entero("CURL_TIMEOUT_SEGUNDOS", 60);

        HttpRequest.Builder req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(timeout))
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(cuerpo));

        Map<String, String> paraFirmar = new LinkedHashMap<>();
        for (String[] h : p.getEncabezados()) {
            if (RESTRINGIDOS.contains(h[0].toLowerCase())) {
                continue;
            }
            req.header(h[0], h[1]);
            paraFirmar.put(h[0], h[1]);
        }

        ultimosEncabezadosAgregados = new LinkedHashMap<>();
        if (p.getAwsSigv4() != null) {
            String[] rs = AwsSigV4Signer.regionYServicio(p.getAwsSigv4(), uri, config.obtener("AWS_REGION").orElse(null));
            Map<String, String> firma = firmador.firmar(metodo, uri, paraFirmar, cuerpo, credenciales(p),
                    rs[0], rs[1], Instant.now());
            firma.forEach(req::header);
            ultimosEncabezadosAgregados.putAll(firma);
        } else if (p.getUsuario() != null) {
            String basic = "Basic " + Base64.getEncoder()
                    .encodeToString(p.getUsuario().getBytes(StandardCharsets.UTF_8));
            req.header("Authorization", basic);
            ultimosEncabezadosAgregados.put("Authorization", basic);
        }

        HttpClient cliente = construirCliente(p.isSeguirRedirecciones());
        long inicio = System.nanoTime();
        try {
            HttpResponse<byte[]> r = cliente.send(req.build(), HttpResponse.BodyHandlers.ofByteArray());
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            RespuestaHttp respuesta = new RespuestaHttp(r.statusCode(), r.headers().map(),
                    decodificar(r.body(), r.headers().map()), ms);
            if (p.isInseguro()) {
                respuesta.getAdvertencias().add("Se ignoro -k/--insecure: el certificado del servidor si se valida. "
                        + "Si el ambiente usa una CA interna, agreguela al truststore de Java.");
            }
            return respuesta;
        } catch (SSLException e) {
            throw new IllegalStateException("Error TLS con " + uri.getHost() + ": " + e.getMessage()
                    + ". Si el ambiente usa un certificado interno, importelo al truststore de Java "
                    + "(-Djavax.net.ssl.trustStore=...).", e);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo conectar con " + uri + ": " + e.getMessage()
                    + " (revise VPN, proxy o la URL).", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio interrumpido", e);
        }
    }

    /** Encabezados que se agregaron al enviar (firma AWS o Basic), para el reporte. */
    public Map<String, String> getUltimosEncabezadosAgregados() {
        return ultimosEncabezadosAgregados;
    }

    private CredencialesAws credenciales(PeticionCurl p) {
        if (config.tiene("AWS_ACCESS_KEY_ID")) {
            return CredencialesAws.desde(config);
        }
        if (p.getUsuario() != null && p.getUsuario().contains(":")) { // curl --user KEY:SECRET
            String[] u = p.getUsuario().split(":", 2);
            return new CredencialesAws(u[0], u[1], config.obtener("AWS_SESSION_TOKEN").orElse(null));
        }
        return CredencialesAws.desde(config); // lanza el error con la variable que falta
    }

    private HttpClient construirCliente(boolean redirecciones) {
        HttpClient.Builder b = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(config.entero("CURL_TIMEOUT_SEGUNDOS", 60)))
                .followRedirects(redirecciones ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
        // Proxy propio de la automatizacion (CURL_PROXY=host:puerto). Si no se indica, Java usa
        // -Dhttps.proxyHost / -Dhttp.proxyHost como siempre. localhost nunca pasa por el proxy.
        config.obtener("CURL_PROXY").ifPresent(proxy -> {
            URI u = URI.create(proxy.contains("://") ? proxy : "http://" + proxy);
            b.proxy(new ProxyExceptoLocal(new InetSocketAddress(u.getHost(), u.getPort() == -1 ? 80 : u.getPort())));
        });
        return b.build();
    }

    /** Usa el proxy indicado para todo menos localhost / 127.0.0.1. */
    private static final class ProxyExceptoLocal extends ProxySelector {
        private final java.net.Proxy proxy;

        ProxyExceptoLocal(InetSocketAddress direccion) {
            this.proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, direccion);
        }

        @Override
        public List<java.net.Proxy> select(URI uri) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            boolean local = host.equals("localhost") || host.startsWith("127.") || host.equals("[::1]");
            return List.of(local ? java.net.Proxy.NO_PROXY : proxy);
        }

        @Override
        public void connectFailed(URI uri, java.net.SocketAddress sa, IOException ioe) {
            // nada: el error se informa en el envio
        }
    }

    private static String decodificar(byte[] cuerpo, Map<String, List<String>> encabezados) {
        String encoding = primero(encabezados, "content-encoding");
        try {
            if ("gzip".equalsIgnoreCase(encoding)) {
                try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(cuerpo))) {
                    cuerpo = in.readAllBytes();
                }
            } else if ("deflate".equalsIgnoreCase(encoding)) {
                try (InputStream in = new InflaterInputStream(new ByteArrayInputStream(cuerpo))) {
                    cuerpo = in.readAllBytes();
                }
            }
        } catch (IOException e) {
            // se deja el cuerpo tal cual
        }
        Charset cs = StandardCharsets.UTF_8;
        String tipo = primero(encabezados, "content-type");
        if (tipo != null) {
            Matcher m = CHARSET.matcher(tipo);
            if (m.find()) {
                try {
                    cs = Charset.forName(m.group(1));
                } catch (RuntimeException ignorado) {
                    cs = StandardCharsets.UTF_8;
                }
            }
        }
        return new String(cuerpo, cs);
    }

    private static String primero(Map<String, List<String>> encabezados, String nombre) {
        for (Map.Entry<String, List<String>> e : encabezados.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(nombre) && !e.getValue().isEmpty()) {
                return e.getValue().get(0);
            }
        }
        return null;
    }
}
