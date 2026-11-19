package co.com.bdb.automation.respuesta;

import co.com.bdb.automation.model.PeticionCurl;

/** Peticion enviada (descrita con credenciales ocultas) y su respuesta. */
public class ResultadoCurl {

    private final int numero;
    private final PeticionCurl peticion;
    private final String descripcionPeticion;
    private final RespuestaHttp respuesta;

    public ResultadoCurl(int numero, PeticionCurl peticion, String descripcionPeticion, RespuestaHttp respuesta) {
        this.numero = numero;
        this.peticion = peticion;
        this.descripcionPeticion = descripcionPeticion;
        this.respuesta = respuesta;
    }

    public int getNumero() { return numero; }
    public PeticionCurl getPeticion() { return peticion; }
    public String getDescripcionPeticion() { return descripcionPeticion; }
    public RespuestaHttp getRespuesta() { return respuesta; }

    /** Texto para el reporte: peticion + respuesta. */
    public String resumen() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Peticion #").append(numero).append(" (").append(peticion.getNombre()).append(") ===\n")
                .append(descripcionPeticion).append("\n=== Respuesta: ").append(respuesta).append(" ===\n")
                .append(respuesta.getCuerpo()).append('\n');
        for (String a : respuesta.getAdvertencias()) {
            sb.append("ADVERTENCIA: ").append(a).append('\n');
        }
        return sb.toString();
    }
}
