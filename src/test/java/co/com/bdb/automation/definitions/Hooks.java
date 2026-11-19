package co.com.bdb.automation.definitions;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class Hooks {

    private final Contexto contexto;

    public Hooks(Contexto contexto) {
        this.contexto = contexto;
    }

    @Before
    public void antes(Scenario scenario) {
        contexto.setEscenario(scenario);
    }

    @After
    public void despues() {
        if (contexto.getServidorEco() != null) {
            contexto.getServidorEco().detener();
        }
    }
}
