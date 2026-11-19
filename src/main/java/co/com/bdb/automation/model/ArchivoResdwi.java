package co.com.bdb.automation.model;

import java.util.ArrayList;
import java.util.List;

/** El archivo ya separado en registros de detalle y trailer CNTRL. */
public class ArchivoResdwi {

    private final String nombre;
    private final String tipoSalto;
    private final List<LineaArchivo> detalle;
    private final LineaArchivo trailer;

    public ArchivoResdwi(String nombre, String tipoSalto, List<LineaArchivo> detalle, LineaArchivo trailer) {
        this.nombre = nombre;
        this.tipoSalto = tipoSalto;
        this.detalle = detalle;
        this.trailer = trailer;
    }

    /** Detalle + trailer (si existe), en el orden del archivo. */
    public List<LineaArchivo> getTodos() {
        List<LineaArchivo> todos = new ArrayList<>(detalle);
        if (trailer != null) {
            todos.add(trailer);
        }
        return todos;
    }

    public String getNombre() { return nombre; }
    public String getTipoSalto() { return tipoSalto; }
    public List<LineaArchivo> getDetalle() { return detalle; }
    public LineaArchivo getTrailer() { return trailer; }
}
