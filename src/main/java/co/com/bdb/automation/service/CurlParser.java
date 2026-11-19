package co.com.bdb.automation.service;

import co.com.bdb.automation.model.PeticionCurl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Convierte un comando curl (copiado de Postman/Chrome en formato bash) en una PeticionCurl.
 * Admite saltos de linea con \ (bash), ^ (cmd) o ` (PowerShell), comillas simples y dobles,
 * $'...' y lineas de comentario que empiezan con #.
 */
public class CurlParser {

    /** Opciones de curl que no cambian la peticion y se ignoran. */
    private static final Set<String> IGNORADAS = Set.of("-s", "--silent", "-S", "--show-error", "-v", "--verbose",
            "-i", "--include", "-f", "--fail", "-g", "--globoff", "--compressed", "-#", "--progress-bar",
            "--http1.1", "--http2", "-N", "--no-buffer");
    /** Opciones que se ignoran pero traen un valor que hay que saltar. */
    private static final Set<String> IGNORADAS_CON_VALOR = Set.of("-o", "--output", "--connect-timeout", "--retry",
            "-A", "--user-agent", "-e", "--referer", "--cacert", "--cert", "--key", "-x", "--proxy", "-b", "--cookie");

    public PeticionCurl parsear(String texto, VariablesCurl variables, Path carpetaBase) {
        List<String> tokens = tokenizar(unirLineas(texto));
        if (tokens.isEmpty() || !(tokens.get(0).equals("curl") || tokens.get(0).equalsIgnoreCase("curl.exe"))) {
            throw new IllegalArgumentException("El archivo no empieza con 'curl'.");
        }
        List<String> resueltos = new ArrayList<>();
        for (String t : tokens.subList(1, tokens.size())) {
            resueltos.add(variables.resolver(t));
        }
        if (!variables.getFaltantes().isEmpty()) {
            throw new IllegalStateException("El curl usa variables que no estan definidas: "
                    + variables.getFaltantes() + ". Agreguelas al .env (NOMBRE=valor).");
        }

        PeticionCurl p = new PeticionCurl();
        List<String> datos = new ArrayList<>();
        for (int i = 0; i < resueltos.size(); i++) {
            String t = resueltos.get(i);
            String op = t;
            String valorPegado = null;
            if (t.startsWith("-") && !t.startsWith("--") && t.length() > 2
                    && "XHdumo".indexOf(t.charAt(1)) >= 0) {
                op = t.substring(0, 2);
                valorPegado = t.substring(2);
            } else if (t.startsWith("--") && t.contains("=")) {
                op = t.substring(0, t.indexOf('='));
                valorPegado = t.substring(t.indexOf('=') + 1);
            }

            switch (op) {
                case "-X":
                case "--request":
                    p.setMetodo(valorPegado != null ? valorPegado : valor(resueltos, ++i, op));
                    break;
                case "-H":
                case "--header":
                    agregarEncabezado(p, valorPegado != null ? valorPegado : valor(resueltos, ++i, op));
                    break;
                case "-d":
                case "--data":
                case "--data-ascii":
                case "--data-binary":
                    datos.add(leerDato(valorPegado != null ? valorPegado : valor(resueltos, ++i, op),
                            carpetaBase, !op.equals("--data-binary"), variables));
                    break;
                case "--data-raw":
                    datos.add(valorPegado != null ? valorPegado : valor(resueltos, ++i, op));
                    break;
                case "--json":
                    datos.add(leerDato(valorPegado != null ? valorPegado : valor(resueltos, ++i, op), carpetaBase, false, variables));
                    if (!p.encabezado("Content-Type").isPresent()) {
                        p.agregarEncabezado("Content-Type", "application/json");
                    }
                    if (!p.encabezado("Accept").isPresent()) {
                        p.agregarEncabezado("Accept", "application/json");
                    }
                    break;
                case "--url":
                    p.setUrl(valorPegado != null ? valorPegado : valor(resueltos, ++i, op));
                    break;
                case "-u":
                case "--user":
                    p.setUsuario(valorPegado != null ? valorPegado : valor(resueltos, ++i, op));
                    break;
                case "--aws-sigv4":
                    p.setAwsSigv4(valorPegado != null ? valorPegado : valor(resueltos, ++i, op));
                    break;
                case "-m":
                case "--max-time":
                    p.setTimeoutSegundos((int) Math.ceil(Double.parseDouble(
                            valorPegado != null ? valorPegado : valor(resueltos, ++i, op))));
                    break;
                case "-k":
                case "--insecure":
                    p.setInseguro(true);
                    break;
                case "-L":
                case "--location":
                    p.setSeguirRedirecciones(true);
                    break;
                case "-I":
                case "--head":
                    p.setMetodo("HEAD");
                    break;
                case "-F":
                case "--form":
                    throw new IllegalArgumentException("La opcion " + op + " (multipart) no esta soportada.");
                default:
                    if (IGNORADAS.contains(op)) {
                        break;
                    }
                    if (esGrupoDeBanderas(op)) { // ej. -sSL: se aplican -L y -k, el resto se ignora
                        p.setSeguirRedirecciones(p.isSeguirRedirecciones() || op.indexOf('L') > 0);
                        p.setInseguro(p.isInseguro() || op.indexOf('k') > 0);
                        break;
                    }
                    if (IGNORADAS_CON_VALOR.contains(op)) {
                        if (valorPegado == null) {
                            i++;
                        }
                        break;
                    }
                    if (t.startsWith("-")) {
                        throw new IllegalArgumentException("Opcion de curl no soportada: " + t);
                    }
                    if (p.getUrl() == null) {
                        p.setUrl(t);
                    } else {
                        throw new IllegalArgumentException("Texto inesperado en el curl: " + t);
                    }
            }
        }
        if (p.getUrl() == null) {
            throw new IllegalArgumentException("El curl no tiene URL.");
        }
        if (!datos.isEmpty()) {
            p.setCuerpo(String.join("&", datos));
            if (!p.encabezado("Content-Type").isPresent()) {
                p.agregarEncabezado("Content-Type", "application/x-www-form-urlencoded");
            }
        }
        return p;
    }

    /** -sSL y similares: varias banderas ignorables juntas. */
    private boolean esGrupoDeBanderas(String op) {
        if (!op.startsWith("-") || op.startsWith("--") || op.length() < 3) {
            return false;
        }
        for (char c : op.substring(1).toCharArray()) {
            if ("sSvifgLkN".indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    private String valor(List<String> tokens, int i, String op) {
        if (i >= tokens.size()) {
            throw new IllegalArgumentException("La opcion " + op + " no tiene valor.");
        }
        return tokens.get(i);
    }

    private void agregarEncabezado(PeticionCurl p, String h) {
        int dos = h.indexOf(':');
        if (dos > 0) {
            String nombre = h.substring(0, dos).trim();
            String valor = h.substring(dos + 1).trim();
            if (valor.isEmpty()) {
                p.quitarEncabezado(nombre); // "Nombre:" en curl quita el encabezado
            } else {
                p.agregarEncabezado(nombre, valor);
            }
        } else if (h.endsWith(";")) {
            p.agregarEncabezado(h.substring(0, h.length() - 1).trim(), ""); // "Nombre;" = encabezado vacio
        } else {
            throw new IllegalArgumentException("Encabezado invalido: " + h);
        }
    }

    /** -d @archivo.json lee el cuerpo de un archivo (relativo a la carpeta del curl) y reemplaza sus variables. */
    private String leerDato(String valor, Path carpetaBase, boolean quitarSaltos, VariablesCurl variables) {
        if (!valor.startsWith("@")) {
            return valor;
        }
        Path archivo = carpetaBase.resolve(valor.substring(1));
        try {
            String contenido = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
            contenido = variables.resolver(contenido);
            if (!variables.getFaltantes().isEmpty()) {
                throw new IllegalStateException("El cuerpo " + archivo.getFileName() + " usa variables que no estan "
                        + "definidas: " + variables.getFaltantes() + ". Agreguelas al .env (NOMBRE=valor).");
            }
            return quitarSaltos ? contenido.replace("\r", "").replace("\n", "") : contenido;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el cuerpo " + archivo, e);
        }
    }

    // ------------------------------------------------------------------ texto

    /** Quita comentarios y une las lineas que terminan en \ ^ o `. */
    String unirLineas(String texto) {
        StringBuilder sb = new StringBuilder();
        for (String linea : texto.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String l = linea.replaceAll("\\s+$", "");
            if (l.trim().startsWith("#")) {
                continue;
            }
            if (l.endsWith("\\") || l.endsWith("^") || l.endsWith("`")) {
                sb.append(l, 0, l.length() - 1).append(' ');
            } else {
                sb.append(l).append('\n');
            }
        }
        return sb.toString();
    }

    /** Separa en argumentos como lo haria bash. */
    List<String> tokenizar(String s) {
        List<String> tokens = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean hayToken = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                if (hayToken) {
                    tokens.add(actual.toString());
                    actual.setLength(0);
                    hayToken = false;
                }
                i++;
            } else if (c == '\'') {
                int fin = s.indexOf('\'', i + 1);
                if (fin < 0) {
                    throw new IllegalArgumentException("Comilla simple sin cerrar en el curl.");
                }
                actual.append(s, i + 1, fin);
                hayToken = true;
                i = fin + 1;
            } else if (c == '$' && i + 1 < s.length() && s.charAt(i + 1) == '\'') {
                i = ansiC(s, i + 2, actual);
                hayToken = true;
            } else if (c == '"') {
                i++;
                boolean cerrada = false;
                while (i < s.length()) {
                    char d = s.charAt(i);
                    if (d == '"') {
                        cerrada = true;
                        i++;
                        break;
                    }
                    if (d == '\\' && i + 1 < s.length() && "\"\\$`\n".indexOf(s.charAt(i + 1)) >= 0) {
                        if (s.charAt(i + 1) != '\n') {
                            actual.append(s.charAt(i + 1));
                        }
                        i += 2;
                    } else {
                        actual.append(d);
                        i++;
                    }
                }
                if (!cerrada) {
                    throw new IllegalArgumentException("Comilla doble sin cerrar en el curl.");
                }
                hayToken = true;
            } else if (c == '\\' && i + 1 < s.length()) {
                actual.append(s.charAt(i + 1));
                hayToken = true;
                i += 2;
            } else {
                actual.append(c);
                hayToken = true;
                i++;
            }
        }
        if (hayToken) {
            tokens.add(actual.toString());
        }
        return tokens;
    }

    /** Cadena $'...' de bash (la usa Chrome al copiar como cURL). */
    private int ansiC(String s, int i, StringBuilder out) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\'') {
                return i + 1;
            }
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case 'n': out.append('\n'); break;
                    case 't': out.append('\t'); break;
                    case 'r': out.append('\r'); break;
                    case 'u':
                        out.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                        i += 4;
                        break;
                    default: out.append(n);
                }
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        throw new IllegalArgumentException("Cadena $'...' sin cerrar en el curl.");
    }
}
