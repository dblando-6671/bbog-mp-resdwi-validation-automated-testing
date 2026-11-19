package co.com.bdb.automation.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Descarga de S3 el RESDWI.TXT configurado en el .env y lo audita.
 * Usa las variables del .env. No corre con un mvn test normal; se ejecuta con:
 * mvn test -Dtest=RunnerAmbienteS3
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/apiTest")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "co.com.bdb.automation.definitions")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@ambiente and @s3")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-reports/ambiente-s3.html")
public class RunnerAmbienteS3 {
}
