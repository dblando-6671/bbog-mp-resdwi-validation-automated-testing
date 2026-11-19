package co.com.bdb.automation.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/** Regresion sin ambiente: auditor con archivos de testData y envio de curl a un servidor local. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/apiTest")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "co.com.bdb.automation.definitions")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@regresion and not @ambiente")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/resdwi.html, json:target/cucumber-reports/resdwi.json")
public class RunnerResdwi {
}
