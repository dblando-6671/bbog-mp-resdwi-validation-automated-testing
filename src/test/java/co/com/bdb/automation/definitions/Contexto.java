package co.com.bdb.automation.definitions;

import co.com.bdb.automation.client.Configuracion;
import co.com.bdb.automation.utilidades.ServidorEco;
import io.cucumber.java.Scenario;

/**
 * Estado compartido entre las clases de steps de un mismo escenario.
 * Cucumber (picocontainer) crea uno nuevo por escenario y lo inyecta por constructor.
 */
public class Contexto {

    private final Configuracion configuracion = new Configuracion();
    private Scenario escenario;
    private ServidorEco servidorEco;

    public Configuracion getConfiguracion() { return configuracion; }
    public Scenario getEscenario() { return escenario; }
    public void setEscenario(Scenario escenario) { this.escenario = escenario; }
    public ServidorEco getServidorEco() { return servidorEco; }
    public void setServidorEco(ServidorEco servidorEco) { this.servidorEco = servidorEco; }
}
