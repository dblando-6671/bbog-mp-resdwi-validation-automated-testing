package co.com.bdb.automation.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lee el archivo plano tal cual llega del host.
 * Se usa ISO-8859-1 para que cada byte sea exactamente un caracter: asi las posiciones
 * del .ctl (que son en bytes) coinciden con las posiciones del String y el 0x00 se conserva.
 * Si el archivo llega por S3, aqui se puede reutilizar S3ClientBdb y devolver el mismo String.
 */
public class ArchivoClient {

    public String leerRuta(Path ruta) {
        try {
            return new String(Files.readAllBytes(ruta), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + ruta, e);
        }
    }

    /** Lee un archivo de src/test/resources (por ejemplo "testData/RESDWI_OK.TXT"). */
    public String leerRecurso(String recurso) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalArgumentException("No existe el recurso " + recurso);
            }
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el recurso " + recurso, e);
        }
    }
}
