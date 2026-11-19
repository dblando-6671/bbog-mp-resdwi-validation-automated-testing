package co.com.bdb.automation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Una peticion HTTP obtenida a partir de un comando curl. */
public class PeticionCurl {

    private String nombre;
    private String metodo;
    private String url;
    private final List<String[]> encabezados = new ArrayList<>();
    private String cuerpo;
    private String usuario;
    private String awsSigv4;
    private boolean inseguro;
    private boolean seguirRedirecciones;
    private Integer timeoutSegundos;

    public void agregarEncabezado(String nombre, String valor) {
        encabezados.add(new String[]{nombre, valor});
    }

    public void quitarEncabezado(String nombre) {
        encabezados.removeIf(h -> h[0].equalsIgnoreCase(nombre));
    }

    public Optional<String> encabezado(String nombre) {
        return encabezados.stream().filter(h -> h[0].equalsIgnoreCase(nombre)).map(h -> h[1]).findFirst();
    }

    /** Metodo efectivo: el de -X, o POST si hay cuerpo, o GET. */
    public String getMetodoEfectivo() {
        if (metodo != null) {
            return metodo.toUpperCase();
        }
        return cuerpo != null ? "POST" : "GET";
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String[]> getEncabezados() { return encabezados; }
    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getAwsSigv4() { return awsSigv4; }
    public void setAwsSigv4(String awsSigv4) { this.awsSigv4 = awsSigv4; }
    public boolean isInseguro() { return inseguro; }
    public void setInseguro(boolean inseguro) { this.inseguro = inseguro; }
    public boolean isSeguirRedirecciones() { return seguirRedirecciones; }
    public void setSeguirRedirecciones(boolean s) { this.seguirRedirecciones = s; }
    public Integer getTimeoutSegundos() { return timeoutSegundos; }
    public void setTimeoutSegundos(Integer t) { this.timeoutSegundos = t; }
}
