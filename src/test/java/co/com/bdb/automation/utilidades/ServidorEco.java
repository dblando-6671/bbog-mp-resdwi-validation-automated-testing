package co.com.bdb.automation.utilidades;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Servidor HTTP local para probar el envio de curls sin depender del ambiente.
 * Responde 200 con un JSON que repite metodo y ruta, y guarda cada peticion recibida.
 */
public class ServidorEco {

    /** Peticion tal como la recibio el servidor. */
    public static class Recibida {
        public final String metodo;
        public final String ruta;
        public final Map<String, String> encabezados;
        public final String cuerpo;

        Recibida(String metodo, String ruta, Map<String, String> encabezados, String cuerpo) {
            this.metodo = metodo;
            this.ruta = ruta;
            this.encabezados = encabezados;
            this.cuerpo = cuerpo;
        }

        public String encabezado(String nombre) {
            return encabezados.get(nombre.toLowerCase());
        }
    }

    private final HttpServer servidor;
    private final List<Recibida> recibidas = Collections.synchronizedList(new ArrayList<>());

    public ServidorEco() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", intercambio -> {
            Map<String, String> enc = new TreeMap<>();
            intercambio.getRequestHeaders().forEach((k, v) -> enc.put(k.toLowerCase(), String.join(",", v)));
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String ruta = intercambio.getRequestURI().toString();
            recibidas.add(new Recibida(intercambio.getRequestMethod(), ruta, enc, cuerpo));
            int codigo = ruta.startsWith("/error") ? 500 : 200;
            byte[] respuesta = ("{\"estado\":\"" + (codigo == 200 ? "OK" : "ERROR") + "\",\"metodo\":\""
                    + intercambio.getRequestMethod() + "\",\"ruta\":\"" + ruta + "\"}").getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            intercambio.sendResponseHeaders(codigo, respuesta.length);
            try (OutputStream out = intercambio.getResponseBody()) {
                out.write(respuesta);
            }
        });
        servidor.start();
    }

    public String getUrl() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public List<Recibida> getRecibidas() {
        return recibidas;
    }

    public Recibida ultima() {
        if (recibidas.isEmpty()) {
            throw new AssertionError("El servidor de prueba no recibio ninguna peticion.");
        }
        return recibidas.get(recibidas.size() - 1);
    }

    public void detener() {
        servidor.stop(0);
    }
}
