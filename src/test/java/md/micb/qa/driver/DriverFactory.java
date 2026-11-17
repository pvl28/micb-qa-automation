package md.micb.qa.driver;

import md.micb.qa.config.Config;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * Creates a WebDriver from configuration.
 * The driver binary is resolved by Selenium Manager (bundled with Selenium 4), so no
 * WebDriverManager dependency and no hard-coded driver paths are needed.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create() {
        WebDriver driver = switch (Config.browser()) {
            case "chrome" -> new ChromeDriver(chromeOptions());
            case "firefox" -> new FirefoxDriver(firefoxOptions());
            default -> throw new IllegalArgumentException("Unsupported browser: " + Config.browser());
        };
        // Implicit wait stays 0: mixing implicit and explicit waits makes timing unpredictable.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Config.pageLoadTimeout());
        driver.manage().window().setSize(new Dimension(Config.windowWidth(), Config.windowHeight()));
        return driver;
    }

    private static ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        if (Config.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=" + Config.windowWidth() + "," + Config.windowHeight(),
                // Fixed UI language: the site (WPML) must not pick a language from the OS locale
                "--lang=ro-RO",
                "--disable-search-engine-choice-screen",
                "--no-first-run");
        return options;
    }

    private static FirefoxOptions firefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        if (Config.headless()) {
            options.addArguments("-headless");
        }
        options.addPreference("intl.accept_languages", "ro-RO,ro");
        return options;
    }
}
