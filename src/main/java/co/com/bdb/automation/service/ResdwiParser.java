package co.com.bdb.automation.service;

import co.com.bdb.automation.model.ArchivoResdwi;
import co.com.bdb.automation.model.DefinicionResdwi;
import co.com.bdb.automation.model.LineaArchivo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Separa el contenido del archivo en registros de detalle y trailer CNTRL. */
public class ResdwiParser {

    private static final Pattern SALTO = Pattern.compile("\r\n|\n|\r");

    public ArchivoResdwi parsear(String nombreArchivo, String contenido) {
        String tipoSalto = contenido.contains(Texto.CRLF) ? "CR+LF (Windows)"
                : (contenido.contains(Texto.LF) ? "LF (Unix)" : "Sin saltos");

        List<LineaArchivo> detalle = new ArrayList<>();
        LineaArchivo trailer = null;
        int numero = 0;
        for (String linea : SALTO.split(contenido, -1)) {
            if (esVacia(linea)) {
                continue;
            }
            numero++;
            LineaArchivo l = new LineaArchivo(numero, linea);
            if (linea.startsWith(DefinicionResdwi.TRAILER_ID)) {
                trailer = l;
            } else {
                detalle.add(l);
            }
        }
        return new ArchivoResdwi(nombreArchivo, tipoSalto, detalle, trailer);
    }

    /** Linea con solo espacios o tabulaciones (o nada). */
    private boolean esVacia(String linea) {
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }
}
