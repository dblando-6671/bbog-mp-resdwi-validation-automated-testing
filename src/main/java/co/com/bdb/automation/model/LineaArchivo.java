package co.com.bdb.automation.model;

/** Una linea no vacia del archivo, con su numero de linea (empieza en 1). */
public class LineaArchivo {

    private final int numero;
    private final String contenido;

    public LineaArchivo(int numero, String contenido) {
        this.numero = numero;
        this.contenido = contenido;
    }

    public int getNumero() { return numero; }
    public String getContenido() { return contenido; }
}
