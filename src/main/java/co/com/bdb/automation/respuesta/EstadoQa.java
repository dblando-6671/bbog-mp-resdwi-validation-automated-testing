package co.com.bdb.automation.respuesta;

public enum EstadoQa {
    CUMPLE("CUMPLE"),
    NO_CUMPLE("NO CUMPLE"),
    INFO("INFO");

    private final String texto;

    EstadoQa(String texto) {
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }

    public static EstadoQa desdeTexto(String texto) {
        for (EstadoQa e : values()) {
            if (e.texto.equalsIgnoreCase(texto.trim())) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado QA desconocido: " + texto);
    }

    @Override
    public String toString() {
        return texto;
    }
}
