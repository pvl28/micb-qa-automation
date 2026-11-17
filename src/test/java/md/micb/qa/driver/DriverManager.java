package md.micb.qa.driver;

import org.openqa.selenium.WebDriver;

/**
 * Holds one WebDriver per thread. ThreadLocal keeps scenarios isolated even if
 * parallel execution is switched on later.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static void start() {
        if (DRIVER.get() == null) {
            DRIVER.set(DriverFactory.create());
        }
    }

    public static WebDriver driver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver is not started. Is the scenario tagged @ui?");
        }
        return driver;
    }

    public static boolean isStarted() {
        return DRIVER.get() != null;
    }

    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }
}
