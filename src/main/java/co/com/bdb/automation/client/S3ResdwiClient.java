package co.com.bdb.automation.client;

import co.com.bdb.automation.model.CredencialesAws;
import co.com.bdb.automation.service.AwsSigV4Signer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descarga el RESDWI.TXT de S3 con las credenciales del .env (firma SigV4, sin SDK).
 * Si el proyecto ya usa S3ClientBdb (AWS SDK), se puede reemplazar por ese cliente:
 * lo unico que debe devolver es el contenido como String ISO-8859-1.
 */
public class S3ResdwiClient {

    private static final Pattern CODIGO_ERROR = Pattern.compile("<Code>([^<]+)</Code>");

    private final Configuracion config;
    private final AwsSigV4Signer firmador = new AwsSigV4Signer();

    public S3ResdwiClient(Configuracion config) {
        this.config = config;
    }

    public String descargar(String bucket, String key) {
        String region = config.requerido("AWS_REGION");
        URI uri = construirUri(bucket, key, region);
        Map<String, String> firma = firmador.firmar("GET", uri, Collections.emptyMap(), null,
                CredencialesAws.desde(config), region, "s3", Instant.now());

        HttpRequest.Builder req = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET();
        firma.forEach(req::header);
        try {
            HttpResponse<byte[]> r = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
                    .send(req.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() != 200) {
                String cuerpo = new String(r.body(), StandardCharsets.UTF_8);
                Matcher m = CODIGO_ERROR.matcher(cuerpo);
                String codigo = m.find() ? m.group(1) : "HTTP " + r.statusCode();
                throw new IllegalStateException("S3 respondio " + r.statusCode() + " (" + codigo + ") al descargar s3://"
                        + bucket + "/" + key + ". " + pista(codigo));
            }
            return new String(r.body(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo conectar con S3 (" + uri.getHost() + "): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Descarga interrumpida", e);
        }
    }

    private URI construirUri(String bucket, String key, String region) {
        String keyCodificada = codificarKey(key);
        String endpoint = config.obtener("RESDWI_S3_ENDPOINT").orElse(null);
        if (endpoint != null) { // endpoint de VPC o personalizado: estilo path
            return URI.create(endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + keyCodificada);
        }
        if (bucket.contains(".")) {
            return URI.create("https://s3." + region + ".amazonaws.com/" + bucket + "/" + keyCodificada);
        }
        return URI.create("https://" + bucket + ".s3." + region + ".amazonaws.com/" + keyCodificada);
    }

    private static String codificarKey(String key) {
        StringBuilder sb = new StringBuilder();
        for (byte b : key.replaceAll("^/+", "").getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || "-_.~/".indexOf(c) >= 0) {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", b & 0xff));
            }
        }
        return sb.toString();
    }

    private static String pista(String codigo) {
        switch (codigo) {
            case "ExpiredToken":
            case "TokenRefreshRequired":
                return "Las credenciales temporales vencieron: copie unas nuevas al .env.";
            case "InvalidAccessKeyId":
            case "SignatureDoesNotMatch":
                return "Revise AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY y AWS_SESSION_TOKEN en el .env.";
            case "AccessDenied":
                return "El rol no tiene permiso s3:GetObject sobre ese archivo (o el archivo no existe).";
            case "NoSuchKey":
                return "El archivo no existe con esa ruta; revise RESDWI_S3_KEY.";
            case "PermanentRedirect":
            case "AuthorizationHeaderMalformed":
                return "La region del bucket no coincide con AWS_REGION.";
            default:
                return "";
        }
    }
}
