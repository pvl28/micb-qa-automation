package md.micb.qa.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import md.micb.qa.api.LoggingFilter;

import java.nio.charset.StandardCharsets;

/** For @api scenarios: attach the last HTTP request/response to the report when a scenario fails. */
public class ApiHooks {

    @Before("@api")
    public void reset() {
        LoggingFilter.clear();
    }

    @After("@api")
    public void attachOnFailure(Scenario scenario) {
        String last = LoggingFilter.lastExchange();
        if (scenario.isFailed() && last != null) {
            scenario.attach(last.getBytes(StandardCharsets.UTF_8), "text/plain", "last-http-exchange");
        }
    }
}
