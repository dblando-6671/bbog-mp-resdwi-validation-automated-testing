package co.com.bdb.automation.model;

/**
 * Un campo del archivo plano segun el control SQL*Loader (ODS_IDIA_ATTXN.ctl).
 * Las posiciones empiezan en 1 e incluyen el ultimo byte, igual que POSITION(inicio:fin).
 */
public class CampoCtl {

    private final int numero;
    private final String nombre;
    private final int inicio;
    private final int fin;
    private final int longitud;
    private final String regla;
    private final boolean obligatorio;

    public CampoCtl(int numero, String nombre, int inicio, int fin, int longitud, String regla, boolean obligatorio) {
        this.numero = numero;
        this.nombre = nombre;
        this.inicio = inicio;
        this.fin = fin;
        this.longitud = longitud;
        this.regla = regla;
        this.obligatorio = obligatorio;
    }

    /** Extrae el valor crudo del campo; si el registro es mas corto devuelve lo que alcance. */
    public String extraer(String registro) {
        int desde = Math.min(inicio - 1, registro.length());
        int hasta = Math.min(fin, registro.length());
        return registro.substring(desde, hasta);
    }

    public boolean esHueco() {
        return nombre.startsWith("HUECO");
    }

    public String getPosicion() {
        return String.format("%03d:%03d", inicio, fin);
    }

    public int getNumero() { return numero; }
    public String getNombre() { return nombre; }
    public int getInicio() { return inicio; }
    public int getFin() { return fin; }
    public int getLongitud() { return longitud; }
    public String getRegla() { return regla; }
    public boolean isObligatorio() { return obligatorio; }
}
