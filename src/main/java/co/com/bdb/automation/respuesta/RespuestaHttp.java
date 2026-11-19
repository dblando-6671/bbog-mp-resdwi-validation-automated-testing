package co.com.bdb.automation.respuesta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Respuesta de una peticion enviada desde un curl. */
public class RespuestaHttp {

    private final int codigo;
    private final Map<String, List<String>> encabezados;
    private final String cuerpo;
    private final long milisegundos;
    private final List<String> advertencias = new ArrayList<>();

    public RespuestaHttp(int codigo, Map<String, List<String>> encabezados, String cuerpo, long milisegundos) {
        this.codigo = codigo;
        this.encabezados = encabezados;
        this.cuerpo = cuerpo;
        this.milisegundos = milisegundos;
    }

    public int getCodigo() { return codigo; }
    public Map<String, List<String>> getEncabezados() { return encabezados; }
    public String getCuerpo() { return cuerpo; }
    public long getMilisegundos() { return milisegundos; }
    public List<String> getAdvertencias() { return advertencias; }

    @Override
    public String toString() {
        return "HTTP " + codigo + " (" + milisegundos + " ms)";
    }
}
