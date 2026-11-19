package co.com.bdb.automation.service;

import co.com.bdb.automation.client.Configuracion;
import co.com.bdb.automation.client.HttpCurlClient;
import co.com.bdb.automation.model.PeticionCurl;
import co.com.bdb.automation.respuesta.RespuestaHttp;
import co.com.bdb.automation.respuesta.ResultadoCurl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee un curl de la carpeta de curls (por defecto src/test/resources/curl), reemplaza variables
 * del .env y lo envia. Puede repetirse N veces (cada envio con su propio UUID y CONTADOR).
 */
public class CurlService {

    private final Configuracion config;
    private final CurlParser parser = new CurlParser();
    private final HttpCurlClient cliente;

    public CurlService(Configuracion config) {
        this.config = config;
        this.cliente = new HttpCurlClient(config);
    }

    public ResultadoCurl enviar(String archivoCurl, int contador) {
        Path ruta = rutaCurl(archivoCurl);
        String texto;
        try {
            texto = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el curl " + ruta.toAbsolutePath(), e);
        }
        PeticionCurl p = parser.parsear(texto, new VariablesCurl(config, contador), ruta.getParent());
        p.setNombre(archivoCurl);
        RespuestaHttp r = cliente.enviar(p);
        return new ResultadoCurl(contador, p, Enmascarador.describir(p, cliente.getUltimosEncabezadosAgregados()), r);
    }

    public List<ResultadoCurl> enviarVarias(String archivoCurl, int veces) {
        long pausa = config.entero("CURL_PAUSA_MS", 0);
        List<ResultadoCurl> resultados = new ArrayList<>();
        for (int i = 1; i <= veces; i++) {
            resultados.add(enviar(archivoCurl, i));
            if (pausa > 0 && i < veces) {
                try {
                    Thread.sleep(pausa);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return resultados;
    }

    public Path rutaCurl(String archivoCurl) {
        Path carpeta = Paths.get(config.obtener("CURL_CARPETA", "src/test/resources/curl"));
        Path ruta = carpeta.resolve(archivoCurl);
        if (!Files.isRegularFile(ruta)) {
            throw new IllegalArgumentException("No existe el curl " + ruta.toAbsolutePath());
        }
        return ruta;
    }
}
