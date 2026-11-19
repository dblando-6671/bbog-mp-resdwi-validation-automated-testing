package co.com.bdb.automation.respuesta;

import co.com.bdb.automation.model.CampoCtl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Todo lo que produce la auditoria: resumen, detalle campo a campo y hallazgos agrupados. */
public class ResultadoAuditoria {

    private final String nombreArchivo;
    private final int registrosDetalle;
    private final List<FilaResumen> resumen = new ArrayList<>();
    private final List<FilaDetalle> detalle = new ArrayList<>();
    private final Map<CampoCtl, List<FilaDetalle>> hallazgosPorCampo =
            new TreeMap<>(Comparator.comparingInt(CampoCtl::getNumero));

    public ResultadoAuditoria(String nombreArchivo, int registrosDetalle) {
        this.nombreArchivo = nombreArchivo;
        this.registrosDetalle = registrosDetalle;
    }

    public void agregarResumen(FilaResumen fila) {
        resumen.add(fila);
    }

    public void agregarDetalle(FilaDetalle fila) {
        detalle.add(fila);
        if (fila.getValidacion().getEstado() == EstadoQa.NO_CUMPLE) {
            hallazgosPorCampo.computeIfAbsent(fila.getCampo(), k -> new ArrayList<>()).add(fila);
        }
    }

    /** Busca una validacion del resumen por el inicio de su nombre (ej. "Trailer - DET_VALOR_ORIGEN"). */
    public Optional<FilaResumen> buscarValidacion(String inicioMetrica) {
        return resumen.stream()
                .filter(f -> f.getMetrica().toLowerCase().startsWith(inicioMetrica.toLowerCase()))
                .findFirst();
    }

    /** NO CUMPLE si al menos una validacion del resumen falla. */
    public EstadoQa getResultadoGeneral() {
        return contar(EstadoQa.NO_CUMPLE) > 0 ? EstadoQa.NO_CUMPLE : EstadoQa.CUMPLE;
    }

    public long contar(EstadoQa estado) {
        return resumen.stream().filter(f -> f.getEstado() == estado).count();
    }

    public int getTotalErroresCampo() {
        return hallazgosPorCampo.values().stream().mapToInt(List::size).sum();
    }

    public String getNombreArchivo() { return nombreArchivo; }
    public int getRegistrosDetalle() { return registrosDetalle; }
    public List<FilaResumen> getResumen() { return resumen; }
    public List<FilaDetalle> getDetalle() { return detalle; }
    public Map<CampoCtl, List<FilaDetalle>> getHallazgosPorCampo() { return hallazgosPorCampo; }
}
