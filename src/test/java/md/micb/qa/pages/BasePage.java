package md.micb.qa.pages;

import md.micb.qa.config.Config;
import md.micb.qa.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Common helpers for all page objects. Every interaction waits for an explicit
 * condition (visible / clickable / attribute) instead of Thread.sleep.
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage() {
        this.driver = DriverManager.driver();
        this.wait = new WebDriverWait(driver, Config.timeout());
    }

    protected void openPath(String path) {
        String url = Config.baseUrl() + path;
        log.info("Open {}", url);
        driver.get(url);
        waitForDocumentReady();
    }

    /**
     * "Page is ready for a user" = document loaded AND the site's own scripts have attached
     * their handlers.
     *
     * Why readyState is not enough here: the site loads Cookiebot with data-blockingmode="auto".
     * Cookiebot holds back the other scripts (jQuery, theme, calculator) and runs them only after
     * it has decided on consent — about 1 s AFTER the load event. Clicks made in that window are
     * silently lost. The theme binds a jQuery click handler to the mega-menu buttons on every page,
     * so its presence is a reliable "scripts initialised" signal.
     * (jQuery._data is jQuery's internal, read-only view of attached handlers; we only READ it.)
     */
    protected void waitForDocumentReady() {
        wait.withMessage("document.readyState did not become 'complete'")
                .until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        wait.withMessage("Site scripts did not initialise (theme click handlers not attached)")
                .until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                        "const b=document.querySelector('.subnav-box .dropdown .dropdown-btn');"
                                + "if(!b){return true;}"   // page without the desktop header: nothing to wait for
                                + "const ev=window.jQuery&&jQuery._data(b,'events');"
                                + "return !!(ev&&ev.click);"));
    }

    protected WebElement visible(By locator) {
        return wait.withMessage("Element not visible: " + locator)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement clickable(By locator) {
        return wait.withMessage("Element not clickable: " + locator)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected List<WebElement> allVisible(By locator) {
        return wait.withMessage("Elements not visible: " + locator)
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    /** Text of an element including text hidden by CSS (textContent), normalised. */
    protected String textContent(WebElement element) {
        String raw = element.getAttribute("textContent");
        return raw == null ? "" : raw.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public String title() {
        return driver.getTitle();
    }

    public String htmlLang() {
        return driver.findElement(By.tagName("html")).getAttribute("lang");
    }
}
