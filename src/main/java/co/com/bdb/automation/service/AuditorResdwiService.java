package co.com.bdb.automation.service;

import co.com.bdb.automation.model.ArchivoResdwi;
import co.com.bdb.automation.model.CampoCtl;
import co.com.bdb.automation.model.DefinicionResdwi;
import co.com.bdb.automation.model.LineaArchivo;
import co.com.bdb.automation.respuesta.EstadoQa;
import co.com.bdb.automation.respuesta.FilaDetalle;
import co.com.bdb.automation.respuesta.FilaResumen;
import co.com.bdb.automation.respuesta.ResultadoAuditoria;
import co.com.bdb.automation.respuesta.ValidacionCampo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static co.com.bdb.automation.model.DefinicionResdwi.LONGITUD_ESPERADA;
import static co.com.bdb.automation.respuesta.EstadoQa.CUMPLE;
import static co.com.bdb.automation.respuesta.EstadoQa.INFO;
import static co.com.bdb.automation.respuesta.EstadoQa.NO_CUMPLE;

/** Auditoria completa del RESDWI.TXT: validaciones de archivo, trailer y campo a campo. */
public class AuditorResdwiService {

    private static final DateTimeFormatter YYYYMMDD =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private final ResdwiParser parser = new ResdwiParser();
    private final ValidadorCampoService validador = new ValidadorCampoService();

    /**
     * @param esperadas transacciones inyectadas; null si no se quiere validar el conteo
     */
    public ResultadoAuditoria auditar(String nombreArchivo, String contenido, Integer esperadas) {
        ArchivoResdwi archivo = parser.parsear(nombreArchivo, contenido);
        List<LineaArchivo> detalle = archivo.getDetalle();
        ResultadoAuditoria r = new ResultadoAuditoria(nombreArchivo, detalle.size());

        if (detalle.isEmpty()) {
            r.agregarResumen(new FilaResumen("Generacion del archivo", "Archivo vacio (0 registros de detalle)",
                    "Archivo con registros", NO_CUMPLE,
                    "El proceso no escribio registros. Revisar SYSOUT del job (ej. status 39 en el OPEN)."));
        } else {
            validarArchivo(archivo, esperadas, r);
        }

        validarTrailer(archivo, r);
        validarCampos(detalle, r);

        if (!detalle.isEmpty()) {
            int errores = r.getTotalErroresCampo();
            r.agregarResumen(new FilaResumen("Campos con error (hoja de detalle)",
                    errores + " validaciones fallidas en " + r.getHallazgosPorCampo().size() + " campo(s)", "0",
                    errores == 0 ? CUMPLE : NO_CUMPLE, "Ver hoja 'Hallazgos por Campo'."));
        }
        return r;
    }

    private void validarArchivo(ArchivoResdwi archivo, Integer esperadas, ResultadoAuditoria r) {
        List<LineaArchivo> detalle = archivo.getDetalle();
        List<LineaArchivo> todos = archivo.getTodos();
        int total = detalle.size();

        // 1. Conteo de detalle vs transacciones inyectadas
        if (esperadas != null) {
            boolean ok = total == esperadas;
            r.agregarResumen(new FilaResumen("Registros de detalle", total, esperadas + " transacciones inyectadas",
                    ok ? CUMPLE : NO_CUMPLE, ok ? "Cantidad de detalles igual a lo inyectado."
                    : "Diferencia de " + (total - esperadas) + " registro(s) frente a lo inyectado."));
        } else {
            r.agregarResumen(new FilaResumen("Registros de detalle", total, "No informado", INFO,
                    "Indique las transacciones inyectadas para validar contra lo inyectado."));
        }

        // 2. Longitud de registro
        Map<Integer, Long> longitudes = todos.stream().collect(Collectors.groupingBy(
                l -> l.getContenido().length(), TreeMap::new, Collectors.counting()));
        boolean longOk = longitudes.size() == 1 && longitudes.containsKey(LONGITUD_ESPERADA);
        String detalleLong = longitudes.entrySet().stream()
                .map(e -> e.getKey() + " bytes x " + e.getValue() + " reg.").collect(Collectors.joining(", "));
        r.agregarResumen(new FilaResumen("Longitud de registro", detalleLong, LONGITUD_ESPERADA + " bytes fijos",
                longOk ? CUMPLE : NO_CUMPLE, longOk ? "Todos los registros miden la longitud del .ctl."
                : "Hay registros con longitud distinta a " + LONGITUD_ESPERADA + " bytes (sin contar el salto de linea)."));

        // 3. Bytes sobrantes despues de la posicion 819
        int maxLong = ((TreeMap<Integer, Long>) longitudes).lastKey();
        if (maxLong > LONGITUD_ESPERADA) {
            r.agregarResumen(new FilaResumen("Bytes sobrantes",
                    "Posiciones " + (LONGITUD_ESPERADA + 1) + " a " + maxLong + " (" + (maxLong - LONGITUD_ESPERADA) + " bytes)",
                    "Sin bytes despues de la 819", NO_CUMPLE,
                    "El registro excede el diseno; ajustar FD del programa y LRECL del JCL a 819."));
        }

        // 4. Subfuente
        long con337 = detalle.stream()
                .filter(l -> l.getContenido().startsWith(DefinicionResdwi.SUBFUENTE_ESPERADA)).count();
        r.agregarResumen(new FilaResumen("Subfuente transaccional (001:003)",
                con337 + " de " + total + " registros con 337", total + " con 337",
                con337 == total ? CUMPLE : NO_CUMPLE, "Constante numerica de CICS TS."));

        // 5. Bytes nulos (posiciones exactas)
        long nulosTotal = todos.stream().mapToLong(l -> Texto.contar(l.getContenido(), Texto.NUL)).sum();
        if (nulosTotal > 0) {
            List<String> conNulos = todos.stream().map(LineaArchivo::getContenido)
                    .filter(c -> c.indexOf(Texto.NUL) >= 0).collect(Collectors.toList());
            int primera = conNulos.stream().mapToInt(c -> c.indexOf(Texto.NUL) + 1).min().orElse(0);
            int ultima = conNulos.stream().mapToInt(c -> c.lastIndexOf(Texto.NUL) + 1).max().orElse(0);
            r.agregarResumen(new FilaResumen("Relleno con bytes nulos (0x00)",
                    String.format("%,d nulos en %d registros (pos. %d a %d)", nulosTotal, conNulos.size(), primera, ultima),
                    "0 nulos; espacios ASCII (0x20)", NO_CUMPLE,
                    "Buffer de host con LOW-VALUES. Inicializar el registro con espacios antes de escribir."));
        } else {
            r.agregarResumen(new FilaResumen("Relleno con bytes nulos (0x00)", "0 nulos", "0 nulos", CUMPLE,
                    "Sin LOW-VALUES en el archivo."));
        }

        // 6. Secuencias duplicadas
        Map<String, Long> secuencias = detalle.stream().collect(Collectors.groupingBy(
                l -> Texto.strip(subcadena(l.getContenido(), 3, 23)), LinkedHashMap::new, Collectors.counting()));
        List<String> duplicadas = secuencias.entrySet().stream()
                .filter(e -> e.getValue() > 1 && !e.getKey().isEmpty())
                .map(Map.Entry::getKey).collect(Collectors.toList());
        r.agregarResumen(new FilaResumen("Secuencia unica (004:023)", duplicadas.size() + " secuencia(s) repetida(s)",
                "0 repetidas", duplicadas.isEmpty() ? CUMPLE : NO_CUMPLE, duplicadas.isEmpty() ? "Sin duplicados."
                : "Repetidas: " + String.join(", ", duplicadas.subList(0, Math.min(5, duplicadas.size())))));

        // 7. Salto de linea (informativo)
        r.agregarResumen(new FilaResumen("Salto de linea", archivo.getTipoSalto(), "Segun acuerdo con ODS", INFO,
                "Verificar que el cargador espera este formato."));
    }

    private void validarTrailer(ArchivoResdwi archivo, ResultadoAuditoria r) {
        LineaArchivo lt = archivo.getTrailer();
        int total = archivo.getDetalle().size();
        if (lt == null) {
            if (total > 0) {
                r.agregarResumen(new FilaResumen("Trailer de control (CNTRL)", "No encontrado",
                        "Registro CNTRL al final", NO_CUMPLE,
                        "El archivo no trae registro de control (WHEN (1:5) = 'CNTRL')."));
            }
            return;
        }
        String t = lt.getContenido();
        int[] pf = DefinicionResdwi.TRAILER_FECHA;
        int[] pn = DefinicionResdwi.TRAILER_NOMBRE;
        int[] pv = DefinicionResdwi.TRAILER_VALOR;
        String fechaRaw = subcadena(t, pf[0] - 1, pf[1]);
        String nombreRaw = subcadena(t, pn[0] - 1, pn[1]);
        String valorRaw = subcadena(t, pv[0] - 1, pv[1]);

        // DET_FEC_ORIGEN
        String mFecha = String.format("Trailer - DET_FEC_ORIGEN (%03d:%03d)", pf[0], pf[1]);
        try {
            LocalDate d = LocalDate.parse(fechaRaw, YYYYMMDD);
            r.agregarResumen(new FilaResumen(mFecha, fechaRaw, "Fecha YYYYMMDD", CUMPLE,
                    "Fecha valida: " + d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "."));
        } catch (DateTimeParseException e) {
            r.agregarResumen(new FilaResumen(mFecha, fechaRaw, "Fecha YYYYMMDD", NO_CUMPLE,
                    "No cumple la mascara YYYYMMDD (ORA-01861)."));
        }

        // DET_NM_ARCHIVO
        String nombre = Texto.strip(nombreRaw);
        boolean nombreOk = nombre.equals(DefinicionResdwi.NOMBRE_ARCHIVO_ESPERADO) && nombreRaw.indexOf(Texto.NUL) < 0;
        r.agregarResumen(new FilaResumen(String.format("Trailer - DET_NM_ARCHIVO (%03d:%03d)", pn[0], pn[1]),
                nombre.isEmpty() ? "EN BLANCO" : nombre, DefinicionResdwi.NOMBRE_ARCHIVO_ESPERADO,
                nombreOk ? CUMPLE : NO_CUMPLE,
                nombreOk ? "Nombre de archivo correcto." : "El nombre no coincide o trae bytes nulos."));

        // DET_VALOR_ORIGEN = cantidad de registros de detalle
        String mValor = String.format("Trailer - DET_VALOR_ORIGEN (%03d:%03d)", pv[0], pv[1]);
        String v = Texto.strip(valorRaw);
        if (valorRaw.indexOf(Texto.NUL) >= 0 || v.isEmpty()) {
            r.agregarResumen(new FilaResumen(mValor, v.isEmpty() ? "EN BLANCO" : "Con nulos", total, NO_CUMPLE,
                    "El trailer no informa la cantidad de registros."));
        } else if (!Texto.esNumerico(v)) {
            r.agregarResumen(new FilaResumen(mValor, valorRaw, total, NO_CUMPLE,
                    "INTEGER EXTERNAL con contenido no numerico (ORA-01722)."));
        } else {
            long valor = Long.parseLong(v);
            boolean ok = valor == total;
            r.agregarResumen(new FilaResumen(mValor, valor, total + " registros de detalle", ok ? CUMPLE : NO_CUMPLE,
                    ok ? "Coincide con la cantidad de registros de detalle."
                            : "El trailer reporta " + valor + " y el archivo tiene " + total + " detalles."));
        }

        // Resto del trailer: no se carga, pero no debe traer nulos
        String resto = subcadena(t, pv[1], LONGITUD_ESPERADA);
        if (resto.indexOf(Texto.NUL) >= 0) {
            r.agregarResumen(new FilaResumen(String.format("Trailer - relleno (%03d:%d)", pv[1] + 1, LONGITUD_ESPERADA),
                    Texto.contar(resto, Texto.NUL) + " nulos", "Sin nulos", NO_CUMPLE,
                    "Posiciones no cargadas por el .ctl, pero con LOW-VALUES."));
        }
    }

    private void validarCampos(List<LineaArchivo> detalle, ResultadoAuditoria r) {
        CampoCtl campoMonto = DefinicionResdwi.campo(DefinicionResdwi.CAMPO_SUMA_MONTO);
        for (LineaArchivo linea : detalle) {
            String raw = linea.getContenido();
            String secuencia = Texto.strip(subcadena(raw, 3, 23).replace(String.valueOf(Texto.NUL), ""));
            String montoTxt = Texto.strip(campoMonto.extraer(raw));
            Double monto = Texto.esNumerico(montoTxt) ? Double.parseDouble(montoTxt) / 100.0 : null;

            for (CampoCtl campo : DefinicionResdwi.CAMPOS) {
                String crudo = campo.extraer(raw);
                ValidacionCampo v = validador.validar(campo, crudo);
                r.agregarDetalle(new FilaDetalle(linea.getNumero(), secuencia, monto, campo, crudo, v));
            }
        }
    }

    /** substring de Python: no falla si el texto es mas corto. */
    private static String subcadena(String s, int desde, int hasta) {
        int d = Math.min(desde, s.length());
        int h = Math.min(hasta, s.length());
        return s.substring(d, Math.max(d, h));
    }

    /** Usado por las pruebas para saber el estado de una metrica. */
    public static EstadoQa estadoDe(ResultadoAuditoria r, String metrica) {
        return r.buscarValidacion(metrica).map(FilaResumen::getEstado)
                .orElseThrow(() -> new AssertionError("No existe la validacion '" + metrica + "'. Disponibles: "
                        + r.getResumen().stream().map(FilaResumen::getMetrica).collect(Collectors.joining(" | "))));
    }
}
