package co.com.bdb.automation.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Variables de configuracion. Orden de prioridad (gana la primera que tenga valor):
 * 1. Valores fijados en la prueba (por ejemplo el servidor local de los escenarios de regresion)
 * 2. Propiedad de sistema: -DNOMBRE=valor o -Dnombre.en.minuscula=valor (ej. -Dresdwi.archivo)
 * 3. Archivo .env en la raiz del proyecto (o el indicado con -Denv.archivo=ruta)
 * 4. Variable de entorno del sistema operativo
 * El .env va antes que el sistema para que unas credenciales AWS viejas definidas en Windows
 * no reemplacen las que se acaban de pegar en el .env.
 */
public class Configuracion {

    private final Map<String, String> fijadas = new HashMap<>();
    private final Map<String, String> archivoEnv;
    private final Path rutaEnv;

    public Configuracion() {
        this(Paths.get(System.getProperty("env.archivo", ".env")));
    }

    public Configuracion(Path rutaEnv) {
        this.rutaEnv = rutaEnv;
        this.archivoEnv = Files.isRegularFile(rutaEnv) ? leerEnv(rutaEnv) : Collections.emptyMap();
    }

    public Optional<String> obtener(String nombre) {
        String v = fijadas.get(nombre);
        if (vacio(v)) {
            v = System.getProperty(nombre);
        }
        if (vacio(v)) {
            v = System.getProperty(nombre.toLowerCase().replace('_', '.'));
        }
        if (vacio(v)) {
            v = archivoEnv.get(nombre);
        }
        if (vacio(v)) {
            v = System.getenv(nombre);
        }
        return vacio(v) ? Optional.empty() : Optional.of(v.trim());
    }

    public String obtener(String nombre, String porDefecto) {
        return obtener(nombre).orElse(porDefecto);
    }

    public String requerido(String nombre) {
        return obtener(nombre).orElseThrow(() -> new IllegalStateException(
                "Falta la variable " + nombre + ". Agreguela al archivo " + rutaEnv.toAbsolutePath()
                        + " (NOMBRE=valor) o envie -D" + nombre + "=valor."));
    }

    public int entero(String nombre, int porDefecto) {
        return obtener(nombre).map(Integer::parseInt).orElse(porDefecto);
    }

    public boolean tiene(String nombre) {
        return obtener(nombre).isPresent();
    }

    /** Fija un valor solo para esta ejecucion (tiene prioridad sobre todo lo demas). */
    public void fijar(String nombre, String valor) {
        fijadas.put(nombre, valor);
    }

    public Path getRutaEnv() {
        return rutaEnv;
    }

    private static boolean vacio(String v) {
        return v == null || v.trim().isEmpty();
    }

    /** Formato .env: NOMBRE=valor, admite comentarios (#), "export ", y valores entre comillas. */
    static Map<String, String> leerEnv(Path ruta) {
        Map<String, String> valores = new LinkedHashMap<>();
        try {
            for (String linea : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                String l = linea.trim();
                if (l.startsWith("﻿")) {
                    l = l.substring(1);
                }
                if (l.isEmpty() || l.startsWith("#")) {
                    continue;
                }
                if (l.startsWith("export ")) {
                    l = l.substring(7).trim();
                }
                int igual = l.indexOf('=');
                if (igual <= 0) {
                    continue;
                }
                String nombre = l.substring(0, igual).trim();
                String valor = l.substring(igual + 1).trim();
                if (valor.length() >= 2 && (valor.startsWith("\"") && valor.endsWith("\"")
                        || valor.startsWith("'") && valor.endsWith("'"))) {
                    valor = valor.substring(1, valor.length() - 1);
                } else {
                    int comentario = valor.indexOf(" #");
                    if (comentario >= 0) {
                        valor = valor.substring(0, comentario).trim();
                    }
                }
                valores.put(nombre, valor);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
        return valores;
    }
}
