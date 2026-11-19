package co.com.bdb.automation.respuesta;

/** Una fila de la hoja "Resumen Ejecutivo". */
public class FilaResumen {

    private final String metrica;
    private final String obtenido;
    private final String esperado;
    private final EstadoQa estado;
    private final String observacion;

    public FilaResumen(String metrica, Object obtenido, Object esperado, EstadoQa estado, String observacion) {
        this.metrica = metrica;
        this.obtenido = String.valueOf(obtenido);
        this.esperado = String.valueOf(esperado);
        this.estado = estado;
        this.observacion = observacion;
    }

    public String getMetrica() { return metrica; }
    public String getObtenido() { return obtenido; }
    public String getEsperado() { return esperado; }
    public EstadoQa getEstado() { return estado; }
    public String getObservacion() { return observacion; }

    @Override
    public String toString() {
        return metrica + " | " + obtenido + " | " + estado + " | " + observacion;
    }
}
