package md.micb.qa.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Single entry point for Maven Surefire / JUnit Platform.
 * Tag selection comes from -Dcucumber.filter.tags (see pom.xml), e.g. "@smoke and @api".
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "md.micb.qa")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value =
        "pretty,"
        + "html:target/cucumber-reports/cucumber.html,"
        + "json:target/cucumber-reports/cucumber.json,"
        + "junit:target/cucumber-reports/cucumber-junit.xml,"
        + "rerun:target/cucumber-reports/rerun.txt")
public class RunCucumberTest {
}
