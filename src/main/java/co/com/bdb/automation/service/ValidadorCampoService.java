package co.com.bdb.automation.service;

import co.com.bdb.automation.model.CampoCtl;
import co.com.bdb.automation.model.DefinicionResdwi;
import co.com.bdb.automation.respuesta.ValidacionCampo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Valida un campo de detalle simulando lo que haria SQL*Loader / Oracle al cargarlo. */
public class ValidadorCampoService {

    private static final DateTimeFormatter YYYYMMDD =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MMDDYY =
            DateTimeFormatter.ofPattern("MMdduu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ValidacionCampo validar(CampoCtl campo, String crudo) {
        String nombre = campo.getNombre();
        int longitud = campo.getLongitud();

        // 1) Registro mas corto que el campo
        if (crudo.length() < longitud) {
            return ValidacionCampo.noCumple("INCOMPLETO",
                    "El registro termina antes: llegan " + crudo.length() + " de " + longitud + " bytes.");
        }

        // 2) Bytes nulos: se revisan ANTES de cualquier limpieza
        int nulos = Texto.contar(crudo, Texto.NUL);
        if (nulos > 0) {
            return ValidacionCampo.noCumple("ERROR", "Contiene " + nulos + " byte(s) nulo(s) 0x00. SQL*Loader no los "
                    + "trata como espacios; deben viajar espacios (0x20) o ceros segun el tipo.");
        }

        String limpio = Texto.strip(crudo);

        // 3) Hueco no mapeado en el .ctl: solo se informa su contenido
        if (campo.esHueco()) {
            if (!limpio.isEmpty()) {
                return ValidacionCampo.cumple("NO SE CARGA", "Posiciones sin columna en el .ctl; traen datos ('"
                        + Texto.recortar(limpio, 30) + "') que NO llegan a la ODS. Confirmar si deben mapearse.");
            }
            return ValidacionCampo.cumple("NO SE CARGA", "Posiciones sin columna en el .ctl (relleno en blanco).");
        }

        // 4) Reglas por tipo
        if (nombre.equals("ATTXN_SUB_FUENTE")) {
            if (!crudo.equals(DefinicionResdwi.SUBFUENTE_ESPERADA)) {
                return ValidacionCampo.noCumple("'" + crudo + "'", "Subfuente '" + crudo + "' invalida; debe ser '"
                        + DefinicionResdwi.SUBFUENTE_ESPERADA + "'.");
            }
            return ValidacionCampo.cumple("'" + crudo + "'",
                    "Subfuente CICS TS '" + DefinicionResdwi.SUBFUENTE_ESPERADA + "' correcta.");
        }

        if (nombre.equals("ATTXN_COD_CAJERO")) {
            if (crudo.contains("MODELXML")) {
                return ValidacionCampo.cumple("'MODELXML'", "Valor 'MODELXML' valido para transacciones REST CICS TS.");
            }
            if (limpio.isEmpty()) {
                return ValidacionCampo.cumple("NULL", "Cajero en blanco.");
            }
            return ValidacionCampo.cumple("'" + limpio + "'", "Cajero informado.");
        }

        if (DefinicionResdwi.FECHAS_YYYYMMDD.contains(nombre)) {
            return validarFecha(limpio, "YYYYMMDD", YYYYMMDD, 8);
        }
        if (nombre.equals("ATTXN_FECHA_ORIGEN")) {
            return validarFecha(limpio, "MMDDYY", MMDDYY, 6);
        }
        if (DefinicionResdwi.NUMERICOS_OBLIGATORIOS.contains(nombre)) {
            return validarNumero(limpio, 100.0, false);
        }
        if (DefinicionResdwi.NUMERICOS_CON_NULL.containsKey(nombre)) {
            return validarNumero(limpio, DefinicionResdwi.NUMERICOS_CON_NULL.get(nombre), true);
        }
        if (DefinicionResdwi.ENTEROS_EXTERNOS.contains(nombre)) {
            if (!limpio.isEmpty() && !Texto.esNumerico(limpio)) {
                return ValidacionCampo.noCumple("ORA-01722", "INTEGER EXTERNAL con contenido no numerico '" + limpio + "'.");
            }
            return limpio.isEmpty() ? ValidacionCampo.cumple("NULL", "En blanco: NULL.")
                    : ValidacionCampo.cumple(limpio, "Entero valido.");
        }

        // 5) Obligatorio en blanco (los DECODE convierten el blanco en '0', se excluyen)
        if (campo.isObligatorio() && limpio.isEmpty() && !campo.getRegla().toUpperCase().startsWith("DECODE")) {
            return ValidacionCampo.noCumple("NULL", "Campo obligatorio en blanco.");
        }

        return ValidacionCampo.cumple(limpio.isEmpty() ? "NULL" : "'" + limpio + "'", "Campo alineado.");
    }

    ValidacionCampo validarFecha(String v, String mascara, DateTimeFormatter formato, int largo) {
        if (v.isEmpty()) {
            return ValidacionCampo.cumple("NULL", "Fecha en blanco: se carga como NULL.");
        }
        if (v.chars().allMatch(c -> c == '0')) {
            return ValidacionCampo.noCumple("ORA-01861",
                    "Fecha en ceros ('" + v + "'): debe viajar en espacios para cargar NULL.");
        }
        try {
            if (v.length() != largo || !Texto.esNumerico(v)) {
                throw new DateTimeParseException("formato", v, 0);
            }
            LocalDate d = LocalDate.parse(v, formato);
            return ValidacionCampo.cumple(d.format(DD_MM_YYYY), "Fecha valida (" + mascara + ").");
        } catch (DateTimeParseException e) {
            return ValidacionCampo.noCumple("ORA-01861", "'" + v + "' no cumple la mascara " + mascara + ".");
        }
    }

    ValidacionCampo validarNumero(String v, double divisor, boolean permiteNull) {
        if (v.isEmpty()) {
            if (permiteNull) {
                return ValidacionCampo.cumple("NULL", "En blanco: se carga como NULL (DECODE).");
            }
            return ValidacionCampo.noCumple("ORA-01722", "Campo numerico en blanco: debe viajar en ceros.");
        }
        if (!Texto.esNumerico(v)) {
            return ValidacionCampo.noCumple("ORA-01722", "Contenido no numerico '" + v + "'.");
        }
        BigDecimal resultado = new BigDecimal(v).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_EVEN);
        String formateado = String.format(Locale.US, "%,.2f", resultado);
        return ValidacionCampo.cumple(formateado, "Valor numerico: " + formateado);
    }
}
