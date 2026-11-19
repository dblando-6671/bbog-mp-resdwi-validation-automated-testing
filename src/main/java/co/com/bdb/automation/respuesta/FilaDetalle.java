package co.com.bdb.automation.respuesta;

import co.com.bdb.automation.model.CampoCtl;

/** Una fila de la hoja de auditoria campo a campo. */
public class FilaDetalle {

    private final int linea;
    private final String secuencia;
    private final Double monto;
    private final CampoCtl campo;
    private final String valorTxt;
    private final ValidacionCampo validacion;

    public FilaDetalle(int linea, String secuencia, Double monto, CampoCtl campo, String valorTxt,
                       ValidacionCampo validacion) {
        this.linea = linea;
        this.secuencia = secuencia;
        this.monto = monto;
        this.campo = campo;
        this.valorTxt = valorTxt;
        this.validacion = validacion;
    }

    public int getLinea() { return linea; }
    public String getSecuencia() { return secuencia; }
    public Double getMonto() { return monto; }
    public CampoCtl getCampo() { return campo; }
    public String getValorTxt() { return valorTxt; }
    public ValidacionCampo getValidacion() { return validacion; }
}
