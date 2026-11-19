package co.com.bdb.automation.service;

import co.com.bdb.automation.model.CredencialesAws;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Firma AWS Signature Version 4 (la misma que hace curl --aws-sigv4 o el SDK de AWS),
 * sin dependencias externas. Sirve para API Gateway con IAM (execute-api), S3, Lambda URL, etc.
 */
public class AwsSigV4Signer {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    /**
     * Devuelve los encabezados que hay que agregar a la peticion: X-Amz-Date, Authorization y,
     * segun el caso, X-Amz-Security-Token y X-Amz-Content-Sha256.
     *
     * @param encabezados encabezados de la peticion (solo se firman content-type y los x-amz-*)
     */
    public Map<String, String> firmar(String metodo, URI uri, Map<String, String> encabezados, byte[] cuerpo,
                                      CredencialesAws cred, String region, String servicio, Instant ahora) {
        String amzDate = FECHA_HORA.format(ahora);
        String fecha = FECHA.format(ahora);
        boolean esS3 = servicio.equals("s3");
        String hashCuerpo = hex(sha256(cuerpo == null ? new byte[0] : cuerpo));

        Map<String, String> agregados = new LinkedHashMap<>();
        agregados.put("X-Amz-Date", amzDate);
        if (cred.getSessionToken() != null) {
            agregados.put("X-Amz-Security-Token", cred.getSessionToken());
        }
        if (esS3) {
            agregados.put("X-Amz-Content-Sha256", hashCuerpo);
        }

        // Encabezados que se firman (ordenados, en minuscula)
        TreeMap<String, String> firmados = new TreeMap<>();
        firmados.put("host", host(uri));
        for (Map.Entry<String, String> h : encabezados.entrySet()) {
            String n = h.getKey().toLowerCase();
            if (n.equals("content-type") || n.startsWith("x-amz-")) {
                firmados.put(n, limpiar(h.getValue()));
            }
        }
        for (Map.Entry<String, String> h : agregados.entrySet()) {
            firmados.put(h.getKey().toLowerCase(), limpiar(h.getValue()));
        }
        StringBuilder canonHeaders = new StringBuilder();
        for (Map.Entry<String, String> h : firmados.entrySet()) {
            canonHeaders.append(h.getKey()).append(':').append(h.getValue()).append('\n');
        }
        String signedHeaders = String.join(";", firmados.keySet());

        String canonicalRequest = metodo.toUpperCase() + "\n"
                + rutaCanonica(uri.getRawPath(), esS3) + "\n"
                + consultaCanonica(uri.getRawQuery()) + "\n"
                + canonHeaders + "\n"
                + signedHeaders + "\n"
                + hashCuerpo;

        String alcance = fecha + "/" + region + "/" + servicio + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + alcance + "\n"
                + hex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));

        byte[] k = hmac(("AWS4" + cred.getSecretAccessKey()).getBytes(StandardCharsets.UTF_8), fecha);
        k = hmac(k, region);
        k = hmac(k, servicio);
        k = hmac(k, "aws4_request");
        String firma = hex(hmac(k, stringToSign));

        agregados.put("Authorization", "AWS4-HMAC-SHA256 Credential=" + cred.getAccessKeyId() + "/" + alcance
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + firma);
        return agregados;
    }

    /** "aws:amz:us-east-1:execute-api" -> [region, servicio]; si faltan se deducen del host. */
    public static String[] regionYServicio(String spec, URI uri, String regionPorDefecto) {
        String[] partes = spec == null ? new String[0] : spec.split(":");
        String region = partes.length > 2 && !partes[2].isEmpty() ? partes[2] : null;
        String servicio = partes.length > 3 && !partes[3].isEmpty() ? partes[3] : null;
        // ej. abc123.execute-api.us-east-1.amazonaws.com
        String[] h = uri.getHost().split("\\.");
        if (h.length >= 4 && uri.getHost().endsWith(".amazonaws.com")) {
            if (servicio == null) {
                servicio = h[h.length - 4];
            }
            if (region == null) {
                region = h[h.length - 3];
            }
        }
        if (region == null) {
            region = regionPorDefecto;
        }
        if (region == null || servicio == null) {
            throw new IllegalArgumentException("Indique region y servicio en --aws-sigv4, "
                    + "por ejemplo \"aws:amz:us-east-1:execute-api\".");
        }
        return new String[]{region, servicio};
    }

    // ------------------------------------------------------------------ canonicalizacion

    private static String host(URI uri) {
        int puerto = uri.getPort();
        boolean porDefecto = puerto == -1 || ("https".equalsIgnoreCase(uri.getScheme()) && puerto == 443)
                || ("http".equalsIgnoreCase(uri.getScheme()) && puerto == 80);
        return porDefecto ? uri.getHost() : uri.getHost() + ":" + puerto;
    }

    private static String limpiar(String v) {
        return v.trim().replaceAll("\\s+", " ");
    }

    static String rutaCanonica(String rawPath, boolean esS3) {
        String ruta = rawPath == null || rawPath.isEmpty() ? "/" : rawPath;
        if (esS3) {
            return ruta;
        }
        // Servicios distintos de S3: se normaliza y se vuelve a codificar (doble codificacion)
        return codificar(normalizar(ruta), "/");
    }

    private static String normalizar(String ruta) {
        boolean barraFinal = ruta.endsWith("/") && ruta.length() > 1;
        Deque<String> pila = new ArrayDeque<>();
        for (String seg : ruta.split("/")) {
            if (seg.isEmpty() || seg.equals(".")) {
                continue;
            }
            if (seg.equals("..")) {
                pila.pollLast();
            } else {
                pila.addLast(seg);
            }
        }
        String r = "/" + String.join("/", pila);
        return barraFinal && !r.equals("/") ? r + "/" : r;
    }

    static String consultaCanonica(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<String[]> pares = new ArrayList<>();
        for (String par : rawQuery.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            int igual = par.indexOf('=');
            String k = igual >= 0 ? par.substring(0, igual) : par;
            String v = igual >= 0 ? par.substring(igual + 1) : "";
            pares.add(new String[]{codificar(decodificar(k), ""), codificar(decodificar(v), "")});
        }
        pares.sort((a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
        List<String> partes = new ArrayList<>();
        for (String[] p : pares) {
            partes.add(p[0] + "=" + p[1]);
        }
        return String.join("&", partes);
    }

    private static String decodificar(String s) {
        return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    /** Codificacion RFC 3986 (A-Z a-z 0-9 - _ . ~ y los caracteres de 'seguros' quedan igual). */
    static String codificar(String s, String seguros) {
        StringBuilder sb = new StringBuilder();
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~' || seguros.indexOf(c) >= 0) {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", b & 0xff));
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ cripto

    static byte[] sha256(byte[] datos) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(datos);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] clave, String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x & 0xff));
        }
        return sb.toString();
    }
}
