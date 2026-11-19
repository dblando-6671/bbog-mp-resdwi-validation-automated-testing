package co.com.bdb.automation.definitions;

import co.com.bdb.automation.client.Configuracion;
import co.com.bdb.automation.respuesta.ResultadoCurl;
import co.com.bdb.automation.service.CurlService;
import co.com.bdb.automation.utilidades.ServidorEco;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps para enviar los curl de src/test/resources/curl. */
public class CurlDefinitions {

    private static final Path CARPETA_RESPUESTAS = Paths.get("target", "respuestas-curl");

    private final Contexto contexto;
    private final Configuracion config;
    private List<ResultadoCurl> resultados = new ArrayList<>();

    public CurlDefinitions(Contexto contexto) {
        this.contexto = contexto;
        this.config = contexto.getConfiguracion();
    }

    // ------------------------------------------------------------------ preparacion (pruebas locales)

    @Dado("que tengo un servidor de prueba local")
    public void queTengoUnServidorDePruebaLocal() {
        ServidorEco servidor = new ServidorEco();
        contexto.setServidorEco(servidor);
        config.fijar("MOCK_URL", servidor.getUrl());
        config.fijar("API_KEY_PRUEBA", "clave-de-prueba-123456");
    }

    @Dado("que uso credenciales AWS de prueba")
    public void queUsoCredencialesAwsDePrueba() {
        config.fijar("AWS_ACCESS_KEY_ID", "AKIDPRUEBA");
        config.fijar("AWS_SECRET_ACCESS_KEY", "secreto-de-prueba");
        config.fijar("AWS_SESSION_TOKEN", "token-de-prueba");
        config.fijar("AWS_REGION", "us-east-1");
    }

    // ------------------------------------------------------------------ envio

    @Cuando("envío la petición curl {string}")
    public void envioLaPeticionCurl(String archivo) throws IOException {
        resultados = new ArrayList<>();
        resultados.add(new CurlService(config).enviar(archivo, 1));
        reportar(archivo);
    }

    @Cuando("envío la petición curl {string} {int} veces")
    public void envioLaPeticionCurlVariasVeces(String archivo, int veces) throws IOException {
        resultados = new CurlService(config).enviarVarias(archivo, veces);
        reportar(archivo);
    }

    @Cuando("envío la petición curl {string} las veces indicadas en el .env")
    public void envioLaPeticionCurlSegunEnv(String archivo) throws IOException {
        envioLaPeticionCurlVariasVeces(archivo, Integer.parseInt(config.requerido("CURL_REPETICIONES")));
    }

    // ------------------------------------------------------------------ validaciones de la respuesta

    @Entonces("el código de respuesta debe ser {int}")
    public void elCodigoDeRespuestaDebeSer(int codigo) {
        ResultadoCurl ultimo = ultimo();
        assertThat(ultimo.getRespuesta().getCodigo())
                .as("Codigo HTTP. Respuesta:%n%s", ultimo.getRespuesta().getCuerpo()).isEqualTo(codigo);
    }

    @Entonces("todas las respuestas deben tener código {int}")
    public void todasLasRespuestasDebenTenerCodigo(int codigo) {
        List<String> fallidas = resultados.stream().filter(r -> r.getRespuesta().getCodigo() != codigo)
                .map(r -> "#" + r.getNumero() + " -> HTTP " + r.getRespuesta().getCodigo() + ": "
                        + recortar(r.getRespuesta().getCuerpo(), 200))
                .collect(Collectors.toList());
        assertThat(fallidas).as("%d de %d peticiones no respondieron %d", fallidas.size(), resultados.size(), codigo)
                .isEmpty();
    }

    @Y("la respuesta debe contener {string}")
    public void laRespuestaDebeContener(String texto) {
        assertThat(ultimo().getRespuesta().getCuerpo()).contains(texto);
    }

    // ------------------------------------------------------------------ validaciones en el servidor local

    @Y("el servidor de prueba recibió {int} peticiones")
    public void elServidorRecibioPeticiones(int n) {
        assertThat(servidor().getRecibidas()).hasSize(n);
    }

    @Y("el servidor de prueba recibió el encabezado {string} con valor {string}")
    public void elServidorRecibioElEncabezadoConValor(String nombre, String valor) {
        assertThat(servidor().ultima().encabezado(nombre)).as("Encabezado %s", nombre).isEqualTo(valor);
    }

    @Y("el servidor de prueba recibió el encabezado {string}")
    public void elServidorRecibioElEncabezado(String nombre) {
        assertThat(servidor().ultima().encabezado(nombre)).as("Encabezado %s", nombre).isNotBlank();
    }

    @Y("el servidor de prueba recibió el método {string} y un cuerpo que contiene {string}")
    public void elServidorRecibioMetodoYCuerpo(String metodo, String texto) {
        ServidorEco.Recibida r = servidor().ultima();
        assertThat(r.metodo).isEqualTo(metodo);
        assertThat(r.cuerpo).contains(texto);
    }

    @Y("cada petición recibida tiene un {string} distinto")
    public void cadaPeticionTieneUnEncabezadoDistinto(String nombre) {
        Set<String> valores = new HashSet<>();
        for (ServidorEco.Recibida r : servidor().getRecibidas()) {
            valores.add(r.encabezado(nombre));
        }
        assertThat(valores).as("Valores distintos de %s", nombre).hasSize(servidor().getRecibidas().size());
    }

    @Y("la petición recibida está firmada con AWS SigV4")
    public void laPeticionRecibidaEstaFirmada() {
        ServidorEco.Recibida r = servidor().ultima();
        assertThat(r.encabezado("authorization")).startsWith("AWS4-HMAC-SHA256 Credential=AKIDPRUEBA/")
                .contains("/us-east-1/execute-api/aws4_request").contains("Signature=");
        assertThat(r.encabezado("x-amz-date")).isNotBlank();
        assertThat(r.encabezado("x-amz-security-token")).isEqualTo("token-de-prueba");
    }

    // ------------------------------------------------------------------ apoyo

    private ResultadoCurl ultimo() {
        assertThat(resultados).as("No se ha enviado ningun curl en este escenario").isNotEmpty();
        return resultados.get(resultados.size() - 1);
    }

    private ServidorEco servidor() {
        assertThat(contexto.getServidorEco()).as("Falta el paso 'que tengo un servidor de prueba local'").isNotNull();
        return contexto.getServidorEco();
    }

    /** Adjunta peticion/respuesta al reporte (credenciales ocultas) y las guarda en target/respuestas-curl. */
    private void reportar(String archivo) throws IOException {
        Files.createDirectories(CARPETA_RESPUESTAS);
        String base = archivo.replaceAll("[^A-Za-z0-9_.-]+", "_");
        StringBuilder todo = new StringBuilder();
        for (ResultadoCurl r : resultados) {
            todo.append(r.resumen()).append('\n');
        }
        Path salida = CARPETA_RESPUESTAS.resolve(base + "_" + System.currentTimeMillis() + ".txt");
        Files.write(salida, todo.toString().getBytes(StandardCharsets.UTF_8));

        long ok = resultados.stream().filter(r -> r.getRespuesta().getCodigo() / 100 == 2).count();
        String encabezado = archivo + ": " + resultados.size() + " peticion(es), " + ok + " con respuesta 2xx. Detalle: "
                + salida.toAbsolutePath();
        contexto.getEscenario().log(encabezado);
        // Si son muchas, se adjunta solo la primera y la ultima para no inflar el reporte
        if (resultados.size() <= 3) {
            contexto.getEscenario().attach(todo.toString(), "text/plain", base);
        } else {
            contexto.getEscenario().attach(resultados.get(0).resumen() + "\n...\n"
                    + resultados.get(resultados.size() - 1).resumen(), "text/plain", base);
        }
    }

    private static String recortar(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max) + "...");
    }
}
