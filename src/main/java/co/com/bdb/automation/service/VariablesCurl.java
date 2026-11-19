package co.com.bdb.automation.service;

import co.com.bdb.automation.client.Configuracion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reemplaza ${NOMBRE} o {{NOMBRE}} dentro de los curl.
 * Variables automaticas (un valor por peticion; si se repite en el curl, es el mismo valor):
 * UUID, FECHA (yyyyMMdd), HORA (HHmmss), FECHA_HORA (ISO), TIMESTAMP (epoch ms), CONTADOR (1..N).
 * Cualquier otra variable se busca en la Configuracion (.env, -D, variables de entorno).
 */
public class VariablesCurl {

    private static final Pattern MARCADOR = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)}|\\{\\{([A-Za-z0-9_.]+)}}");

    private final Configuracion config;
    private final Map<String, String> automaticas = new HashMap<>();
    private final Set<String> faltantes = new LinkedHashSet<>();

    public VariablesCurl(Configuracion config, int contador) {
        this.config = config;
        LocalDateTime ahora = LocalDateTime.now();
        automaticas.put("UUID", UUID.randomUUID().toString());
        automaticas.put("FECHA", ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        automaticas.put("HORA", ahora.format(DateTimeFormatter.ofPattern("HHmmss")));
        automaticas.put("FECHA_HORA", ahora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        automaticas.put("TIMESTAMP", String.valueOf(System.currentTimeMillis()));
        automaticas.put("CONTADOR", String.valueOf(contador));
    }

    public String resolver(String texto) {
        Matcher m = MARCADOR.matcher(texto);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String nombre = m.group(1) != null ? m.group(1) : m.group(2);
            String valor = config.obtener(nombre).orElse(automaticas.get(nombre));
            if (valor == null) {
                faltantes.add(nombre);
                valor = m.group();
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(valor));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public Set<String> getFaltantes() {
        return faltantes;
    }
}
