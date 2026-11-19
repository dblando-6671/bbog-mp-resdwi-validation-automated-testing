package co.com.bdb.automation.respuesta;

/** Resultado de validar un campo: estado, como lo veria Oracle y el diagnostico. */
public class ValidacionCampo {

    private final EstadoQa estado;
    private final String valorOracle;
    private final String observacion;

    public ValidacionCampo(EstadoQa estado, String valorOracle, String observacion) {
        this.estado = estado;
        this.valorOracle = valorOracle;
        this.observacion = observacion;
    }

    public static ValidacionCampo cumple(String valorOracle, String observacion) {
        return new ValidacionCampo(EstadoQa.CUMPLE, valorOracle, observacion);
    }

    public static ValidacionCampo noCumple(String valorOracle, String observacion) {
        return new ValidacionCampo(EstadoQa.NO_CUMPLE, valorOracle, observacion);
    }

    public EstadoQa getEstado() { return estado; }
    public String getValorOracle() { return valorOracle; }
    public String getObservacion() { return observacion; }
}
