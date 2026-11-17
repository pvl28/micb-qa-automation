package md.micb.qa.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import md.micb.qa.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Browser lifecycle for scenarios tagged @ui: a fresh browser per scenario
 * (independent scenarios, no leftover cookies), diagnostics on failure.
 */
public class UiHooks {

    private static final Logger LOG = LoggerFactory.getLogger(UiHooks.class);
    private static final Path SCREENSHOT_DIR = Paths.get("target", "screenshots");

    @Before(value = "@ui", order = 0)
    public void startBrowser(Scenario scenario) {
        LOG.info("=== START [{}] {}", String.join(" ", scenario.getSourceTagNames()), scenario.getName());
        DriverManager.start();
    }

    @After(value = "@ui", order = 0)
    public void stopBrowser(Scenario scenario) {
        try {
            if (scenario.isFailed() && DriverManager.isStarted()) {
                attachDiagnostics(scenario, DriverManager.driver());
            }
        } finally {
            DriverManager.quit();
            LOG.info("=== END   {} -> {}", scenario.getName(), scenario.getStatus());
        }
    }

    private void attachDiagnostics(Scenario scenario, WebDriver driver) {
        try {
            String url = driver.getCurrentUrl();
            scenario.attach(url, "text/plain", "current-url");
            LOG.error("Scenario failed at URL {}", url);

            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(png, "image/png", "screenshot");
            Files.createDirectories(SCREENSHOT_DIR);
            Path file = SCREENSHOT_DIR.resolve(safeName(scenario) + ".png");
            Files.write(file, png);
            LOG.error("Screenshot saved: {}", file.toAbsolutePath());

            String source = driver.getPageSource();
            scenario.attach(source.getBytes(StandardCharsets.UTF_8), "text/html", "page-source");
        } catch (IOException | RuntimeException e) {
            // Diagnostics must never hide the original failure
            LOG.warn("Could not collect failure diagnostics: {}", e.toString());
        }
    }

    private static String safeName(Scenario scenario) {
        String line = scenario.getLine() == null ? "" : "_L" + scenario.getLine();
        return scenario.getName().replaceAll("[^A-Za-z0-9._-]+", "_") + line;
    }
}
