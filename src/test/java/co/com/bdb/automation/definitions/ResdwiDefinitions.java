package co.com.bdb.automation.definitions;

import co.com.bdb.automation.client.ArchivoClient;
import co.com.bdb.automation.model.DefinicionResdwi;
import co.com.bdb.automation.respuesta.EstadoQa;
import co.com.bdb.automation.respuesta.FilaResumen;
import co.com.bdb.automation.respuesta.ResultadoAuditoria;
import co.com.bdb.automation.service.AuditorResdwiService;
import co.com.bdb.automation.service.ReporteExcelService;
import co.com.bdb.automation.client.Configuracion;
import co.com.bdb.automation.client.S3ResdwiClient;
import co.com.bdb.automation.service.VariablesCurl;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ResdwiDefinitions {

    private static final String CARPETA_DATOS = "testData/resdwi/";
    private static final Path CARPETA_INFORMES = Paths.get("target", "reportes-resdwi");

    private final ArchivoClient archivoClient = new ArchivoClient();
    private final AuditorResdwiService auditor = new AuditorResdwiService();
    private final ReporteExcelService reporte = new ReporteExcelService();

    private final Contexto contexto;
    private final Configuracion config;
    private String nombreArchivo;
    private String contenido;
    private ResultadoAuditoria resultado;

    public ResdwiDefinitions(Contexto contexto) {
        this.contexto = contexto;
        this.config = contexto.getConfiguracion();
    }

    @Dado("que cargo el archivo de prueba {string}")
    public void queCargoElArchivoDePrueba(String archivo) {
        nombreArchivo = DefinicionResdwi.NOMBRE_ARCHIVO_ESPERADO;
        contenido = archivoClient.leerRecurso(CARPETA_DATOS + archivo);
    }

    /** Ruta local en RESDWI_ARCHIVO (.env) o -Dresdwi.archivo. */
    @Dado("que cargo el archivo RESDWI indicado en el .env o por parámetro")
    public void queCargoElArchivoIndicado() {
        String ruta = config.obtener("RESDWI_ARCHIVO").orElse("");
        Assumptions.assumeFalse(ruta.isEmpty(),
                "Indique la ruta con RESDWI_ARCHIVO en el .env o con -Dresdwi.archivo=C:/ruta/RESDWI.TXT");
        Path path = Paths.get(ruta);
        nombreArchivo = path.getFileName().toString();
        contenido = archivoClient.leerRuta(path);
    }

    /** Bucket RESDWI_S3_BUCKET y ruta RESDWI_S3_KEY del .env (la ruta admite ${FECHA}). */
    @Dado("que descargo de S3 el archivo RESDWI configurado en el .env")
    public void queDescargoDeS3ElArchivoConfigurado() {
        descargarDeS3(config.requerido("RESDWI_S3_KEY"));
    }

    @Dado("que descargo de S3 el archivo {string}")
    public void queDescargoDeS3ElArchivo(String key) {
        descargarDeS3(key);
    }

    private void descargarDeS3(String keyConVariables) {
        String bucket = config.requerido("RESDWI_S3_BUCKET");
        String key = new VariablesCurl(config, 1).resolver(keyConVariables);
        contenido = new S3ResdwiClient(config).descargar(bucket, key);
        nombreArchivo = key.substring(key.lastIndexOf('/') + 1);
        contexto.getEscenario().log("Descargado s3://" + bucket + "/" + key + " (" + contenido.length() + " bytes)");
    }

    @Cuando("ejecuto la auditoría con {int} transacciones inyectadas")
    public void ejecutoLaAuditoriaCon(int esperadas) {
        auditar(esperadas);
    }

    @Cuando("ejecuto la auditoría sin informar transacciones inyectadas")
    public void ejecutoLaAuditoriaSinInformar() {
        auditar(null);
    }

    /** RESDWI_ESPERADAS en el .env o -Dresdwi.esperadas; si no esta, no se valida el conteo. */
    @Cuando("ejecuto la auditoría con las transacciones inyectadas indicadas en el .env o por parámetro")
    public void ejecutoLaAuditoriaConParametro() {
        auditar(config.obtener("RESDWI_ESPERADAS").map(Integer::valueOf).orElse(null));
    }

    @Entonces("el resultado general debe ser {string}")
    public void elResultadoGeneralDebeSer(String esperado) {
        assertThat(resultado.getResultadoGeneral())
                .as("Resultado general. Resumen:%n%s", resumenTexto())
                .isEqualTo(EstadoQa.desdeTexto(esperado));
    }

    @Entonces("la validación {string} debe ser {string}")
    public void laValidacionDebeSer(String metrica, String esperado) {
        FilaResumen fila = resultado.buscarValidacion(metrica).orElse(null);
        assertThat(fila).as("No existe la validacion '%s'. Resumen:%n%s", metrica, resumenTexto()).isNotNull();
        assertThat(fila.getEstado()).as("%s -> %s", metrica, fila.getObservacion())
                .isEqualTo(EstadoQa.desdeTexto(esperado));
    }

    @Y("no debe haber errores a nivel de campo")
    public void noDebeHaberErroresDeCampo() {
        assertThat(resultado.getTotalErroresCampo())
                .as("Campos con error: %s", resultado.getHallazgosPorCampo().keySet().stream()
                        .map(c -> c.getNombre()).collect(Collectors.joining(", ")))
                .isZero();
    }

    @Y("el campo {string} debe reportar error")
    public void elCampoDebeReportarError(String campo) {
        assertThat(resultado.getHallazgosPorCampo().keySet().stream().map(c -> c.getNombre()))
                .as("Campos con hallazgos").contains(campo);
    }

    @Y("se genera el informe Excel")
    public void seGeneraElInformeExcel() throws IOException {
        String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String base = contexto.getEscenario().getName().replaceAll("[^A-Za-z0-9]+", "_");
        Path salida = CARPETA_INFORMES.resolve("Informe_Auditoria_ODS_" + base + "_" + marca + ".xlsx");
        reporte.generar(resultado, salida);
        assertThat(salida).exists();
        contexto.getEscenario().attach(Files.readAllBytes(salida),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", salida.getFileName().toString());
        contexto.getEscenario().log("Informe generado: " + salida.toAbsolutePath());
    }

    private void auditar(Integer esperadas) {
        resultado = auditor.auditar(nombreArchivo, contenido, esperadas);
        contexto.getEscenario().log(resumenTexto());
    }

    private String resumenTexto() {
        return resultado.getResumen().stream().map(FilaResumen::toString).collect(Collectors.joining("\n"));
    }
}
