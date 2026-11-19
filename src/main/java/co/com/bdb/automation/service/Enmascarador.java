package co.com.bdb.automation.service;

import co.com.bdb.automation.model.PeticionCurl;

import java.util.regex.Pattern;

/** Oculta credenciales antes de escribirlas en logs o reportes. */
public final class Enmascarador {

    private static final Pattern SENSIBLE = Pattern.compile(
            "(?i).*(authorization|api-?key|token|secret|password|passwd|cookie|credential|x-amz-security).*");

    private Enmascarador() {
    }

    public static boolean esSensible(String nombreEncabezado) {
        return SENSIBLE.matcher(nombreEncabezado).matches();
    }

    public static String ocultar(String valor) {
        if (valor == null || valor.length() <= 8) {
            return "****";
        }
        return valor.substring(0, 4) + "****" + valor.substring(valor.length() - 4);
    }

    /** Descripcion de la peticion apta para reportes. */
    public static String describir(PeticionCurl p, java.util.Map<String, String> encabezadosAgregados) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getMetodoEfectivo()).append(' ').append(p.getUrl()).append('\n');
        for (String[] h : p.getEncabezados()) {
            sb.append(h[0]).append(": ").append(esSensible(h[0]) ? ocultar(h[1]) : h[1]).append('\n');
        }
        if (encabezadosAgregados != null) {
            for (java.util.Map.Entry<String, String> h : encabezadosAgregados.entrySet()) {
                sb.append(h.getKey()).append(": ")
                        .append(esSensible(h.getKey()) ? ocultar(h.getValue()) : h.getValue()).append('\n');
            }
        }
        if (p.getCuerpo() != null) {
            sb.append('\n').append(p.getCuerpo()).append('\n');
        }
        return sb.toString();
    }
}
