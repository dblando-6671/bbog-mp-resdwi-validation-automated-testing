package co.com.bdb.automation.service;

import co.com.bdb.automation.model.CampoCtl;
import co.com.bdb.automation.respuesta.EstadoQa;
import co.com.bdb.automation.respuesta.FilaDetalle;
import co.com.bdb.automation.respuesta.FilaResumen;
import co.com.bdb.automation.respuesta.ResultadoAuditoria;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Genera el informe Excel con las mismas 3 hojas del script Python. */
public class ReporteExcelService {

    private static final String REQUERIMIENTO =
            "Requerimiento: CVAPD00685673 | Jira: SP-2986 | Componente: CICS TS (Subfuente 337)";

    public Path generar(ResultadoAuditoria r, Path salida) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Estilos e = new Estilos(wb);
            hojaResumen(wb, e, r);
            hojaDetalle(wb, e, r);
            hojaHallazgos(wb, e, r);
            if (salida.getParent() != null) {
                Files.createDirectories(salida.getParent());
            }
            try (OutputStream out = Files.newOutputStream(salida)) {
                wb.write(out);
            }
            wb.dispose();
            return salida;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el informe " + salida, ex);
        }
    }

    private void hojaResumen(SXSSFWorkbook wb, Estilos e, ResultadoAuditoria r) {
        Sheet ws = wb.createSheet("Resumen Ejecutivo");
        celda(ws.createRow(0), 0, "INFORME DE CERTIFICACION QA - ARCHIVO PLANO " + r.getNombreArchivo(), e.titulo);
        celda(ws.createRow(1), 0, REQUERIMIENTO, e.subtitulo);
        encabezado(ws, 3, e, "METRICA / VALIDACION", "VALOR OBTENIDO", "VALOR ESPERADO", "ESTADO QA",
                "OBSERVACION TECNICA");
        int fila = 4;
        for (FilaResumen f : r.getResumen()) {
            Row row = ws.createRow(fila++);
            celda(row, 0, f.getMetrica(), e.normal);
            celda(row, 1, f.getObtenido(), e.normal);
            celda(row, 2, f.getEsperado(), e.normal);
            celda(row, 3, f.getEstado().getTexto(), e.estadoCentrado.get(f.getEstado()));
            celda(row, 4, f.getObservacion(), e.normal);
        }
        anchos(ws, 36, 40, 30, 14, 70);
        ws.createFreezePane(0, 4);
    }

    private void hojaDetalle(SXSSFWorkbook wb, Estilos e, ResultadoAuditoria r) {
        long campos = co.com.bdb.automation.model.DefinicionResdwi.CAMPOS.stream().filter(c -> !c.esHueco()).count();
        Sheet ws = wb.createSheet("Auditoria " + campos + " Campos");
        encabezado(ws, 0, e, "Linea", "Secuencia Host", "Monto COP", "#", "Columna ODS", "Posicion .ctl", "Long.",
                "Regla SQL*Loader", "Obligatorio", "Valor en TXT (punto medio = 0x00)", "Valor en Oracle", "Cumple",
                "Diagnostico");
        int fila = 1;
        for (FilaDetalle d : r.getDetalle()) {
            CampoCtl c = d.getCampo();
            Row row = ws.createRow(fila++);
            numero(row, 0, d.getLinea(), e.borde);
            celda(row, 1, d.getSecuencia(), e.borde);
            if (d.getMonto() != null) {
                numero(row, 2, d.getMonto(), e.borde);
            } else {
                celda(row, 2, "", e.borde);
            }
            numero(row, 3, c.getNumero(), e.borde);
            celda(row, 4, c.getNombre(), e.borde);
            celda(row, 5, c.getPosicion(), e.borde);
            numero(row, 6, c.getLongitud(), e.borde);
            celda(row, 7, c.getRegla(), e.borde);
            celda(row, 8, c.isObligatorio() ? "Obligatorio" : "Opcional", e.borde);
            celda(row, 9, d.getValorTxt(), e.borde);
            celda(row, 10, d.getValidacion().getValorOracle(), e.borde);
            celda(row, 11, d.getValidacion().getEstado().getTexto(), e.estado.get(d.getValidacion().getEstado()));
            celda(row, 12, d.getValidacion().getObservacion(), e.borde);
        }
        anchos(ws, 7, 22, 14, 5, 24, 13, 7, 40, 12, 30, 18, 12, 60);
        ws.createFreezePane(0, 1);
    }

    private void hojaHallazgos(SXSSFWorkbook wb, Estilos e, ResultadoAuditoria r) {
        Sheet ws = wb.createSheet("Hallazgos por Campo");
        encabezado(ws, 0, e, "#", "Columna ODS", "Posicion", "Registros con error", "Lineas (primeras 10)",
                "Diagnostico");
        int fila = 1;
        for (Map.Entry<CampoCtl, List<FilaDetalle>> h : r.getHallazgosPorCampo().entrySet()) {
            List<FilaDetalle> casos = h.getValue();
            String lineas = casos.stream().limit(10).map(c -> String.valueOf(c.getLinea()))
                    .collect(Collectors.joining(", ")) + (casos.size() > 10 ? " ..." : "");
            Row row = ws.createRow(fila++);
            numero(row, 0, h.getKey().getNumero(), e.borde);
            celda(row, 1, h.getKey().getNombre(), e.borde);
            celda(row, 2, h.getKey().getPosicion(), e.borde);
            numero(row, 3, casos.size(), e.borde);
            celda(row, 4, lineas, e.borde);
            celda(row, 5, diagnosticoMasComun(casos), e.borde);
        }
        if (r.getHallazgosPorCampo().isEmpty()) {
            Row row = ws.createRow(fila);
            celda(row, 0, "-", e.borde);
            celda(row, 1, "Sin hallazgos a nivel de campo", e.borde);
            celda(row, 2, "", e.borde);
            numero(row, 3, 0, e.borde);
            celda(row, 4, "", e.borde);
            celda(row, 5, "", e.borde);
        }
        anchos(ws, 5, 26, 12, 18, 40, 70);
        ws.createFreezePane(0, 1);
    }

    private String diagnosticoMasComun(List<FilaDetalle> casos) {
        Map<String, Long> conteo = casos.stream().map(c -> c.getValidacion().getObservacion())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        return conteo.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
    }

    // ---------------------------------------------------------------- utilidades

    private void encabezado(Sheet ws, int fila, Estilos e, String... columnas) {
        Row row = ws.createRow(fila);
        for (int i = 0; i < columnas.length; i++) {
            celda(row, i, columnas[i], e.encabezado);
        }
    }

    private void celda(Row row, int col, String valor, CellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(Texto.paraExcel(valor));
        c.setCellStyle(estilo);
    }

    private void numero(Row row, int col, double valor, CellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private void anchos(Sheet ws, int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            ws.setColumnWidth(i, anchos[i] * 256);
        }
    }

    /** Colores y fuentes del informe (mismos del script Python). */
    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle subtitulo;
        final CellStyle encabezado;
        final CellStyle borde;
        final CellStyle normal;
        final Map<EstadoQa, CellStyle> estado = new EnumMap<>(EstadoQa.class);
        final Map<EstadoQa, CellStyle> estadoCentrado = new EnumMap<>(EstadoQa.class);

        Estilos(SXSSFWorkbook wb) {
            titulo = wb.createCellStyle();
            titulo.setFont(fuente(wb, 14, true, false, "1F4E78"));

            subtitulo = wb.createCellStyle();
            subtitulo.setFont(fuente(wb, 10, false, true, "595959"));

            encabezado = wb.createCellStyle();
            encabezado.setFont(fuente(wb, 10, true, false, "FFFFFF"));
            relleno(encabezado, "1F4E78");
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            encabezado.setVerticalAlignment(VerticalAlignment.CENTER);
            encabezado.setWrapText(true);

            borde = wb.createCellStyle();
            bordes(borde);

            normal = wb.createCellStyle();
            bordes(normal);
            normal.setVerticalAlignment(VerticalAlignment.TOP);
            normal.setWrapText(true);

            Map<EstadoQa, String> colores = new EnumMap<>(EstadoQa.class);
            colores.put(EstadoQa.CUMPLE, "C6EFCE");
            colores.put(EstadoQa.NO_CUMPLE, "FFC7CE");
            colores.put(EstadoQa.INFO, "FFEB9C");
            for (Map.Entry<EstadoQa, String> c : colores.entrySet()) {
                CellStyle s = wb.createCellStyle();
                bordes(s);
                relleno(s, c.getValue());
                estado.put(c.getKey(), s);

                CellStyle sc = wb.createCellStyle();
                bordes(sc);
                relleno(sc, c.getValue());
                sc.setAlignment(HorizontalAlignment.CENTER);
                sc.setVerticalAlignment(VerticalAlignment.TOP);
                estadoCentrado.put(c.getKey(), sc);
            }
        }

        private static XSSFFont fuente(SXSSFWorkbook wb, int tamano, boolean negrita, boolean cursiva, String hex) {
            XSSFFont f = (XSSFFont) wb.createFont();
            f.setFontName("Calibri");
            f.setFontHeightInPoints((short) tamano);
            f.setBold(negrita);
            f.setItalic(cursiva);
            f.setColor(new XSSFColor(hexABytes(hex), null));
            return f;
        }

        private static void relleno(CellStyle s, String hex) {
            ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexABytes(hex), null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        private static void bordes(CellStyle s) {
            XSSFColor gris = new XSSFColor(hexABytes("D9D9D9"), null);
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            XSSFCellStyle x = (XSSFCellStyle) s;
            x.setTopBorderColor(gris);
            x.setBottomBorderColor(gris);
            x.setLeftBorderColor(gris);
            x.setRightBorderColor(gris);
        }

        private static byte[] hexABytes(String hex) {
            return new byte[]{(byte) Integer.parseInt(hex.substring(0, 2), 16),
                    (byte) Integer.parseInt(hex.substring(2, 4), 16),
                    (byte) Integer.parseInt(hex.substring(4, 6), 16)};
        }
    }
}
