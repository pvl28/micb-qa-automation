package md.micb.qa.pages.components;

import md.micb.qa.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URI;
import java.util.List;

/**
 * Desktop header: logo, top menu, language switcher, mega-menu and "Contact centru".
 * Locators use structure (classes, href) rather than visible text where possible,
 * so the same component works in RO, RU and EN.
 */
public class Header extends BasePage {

    private static final By NAV = By.cssSelector("nav.nav-section");
    private static final By LOGO = By.cssSelector("nav.nav-section a.logo");
    private static final By MAIN_MENU_LINKS = By.cssSelector("nav.nav-section #menu-main a");
    private static final By LANG_LINKS = By.cssSelector("nav.nav-section .lang-switcher a");
    private static final By CONTACT_CENTRE = By.cssSelector(".subnav-box .dropdown.call-center");

    public boolean isLogoVisible() {
        return visible(LOGO).isDisplayed();
    }

    /** hrefs of the top menu, e.g. https://micb.md/despre-banca/ */
    public List<String> mainMenuHrefs() {
        visible(NAV);
        return driver.findElements(MAIN_MENU_LINKS).stream()
                .map(a -> a.getAttribute("href"))
                .toList();
    }

    public void switchLanguage(String code) {
        WebElement link = languageLink(code);
        String target = link.getAttribute("href");
        log.info("Switch language to '{}' -> {}", code, target);
        link.click();
        // Compare the path only: Google Analytics' cross-domain linker may append "?_gl=..."
        String targetPath = URI.create(target).getPath();
        wait.withMessage("URL path did not change to " + targetPath)
                .until(d -> targetPath.equals(URI.create(d.getCurrentUrl()).getPath()));
        waitForDocumentReady();
    }

    public String activeLanguage() {
        return visible(By.cssSelector("nav.nav-section .lang-switcher a.active")).getText().trim();
    }

    private WebElement languageLink(String code) {
        return allVisible(LANG_LINKS).stream()
                .filter(a -> a.getText().trim().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Language link not found: " + code));
    }

    /**
     * Opens a mega-menu entry (e.g. "Credite") and clicks a link inside it by href path.
     * On desktop (> 1024 px) the theme toggles the dropdown on CLICK and marks it ".active".
     */
    public void openFromMegaMenu(String menuLabel, String hrefPath) {
        WebElement dropdown = expandMegaMenu(menuLabel);
        By linkLocator = By.cssSelector(".dropdown-menu a[href$='" + hrefPath + "']");
        WebElement link = wait.withMessage("Link " + hrefPath + " not visible in '" + menuLabel + "' menu")
                .until(d -> dropdown.findElements(linkLocator).stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
        log.info("Mega-menu '{}' -> {}", menuLabel, link.getAttribute("href"));
        link.click();
        waitForDocumentReady();
    }

    private WebElement expandMegaMenu(String menuLabel) {
        WebElement dropdown = megaMenuDropdown(menuLabel);
        return expand(dropdown);
    }

    /**
     * Opens a toggle dropdown (one click) and waits until it is open: the theme adds ".active"
     * and the menu fades in (opacity animation), so we wait for both.
     * Page scripts are guaranteed to be initialised by BasePage.waitForDocumentReady().
     */
    private WebElement expand(WebElement dropdown) {
        WebElement button = dropdown.findElement(By.cssSelector(".dropdown-btn"));
        WebElement menu = dropdown.findElement(By.cssSelector(".dropdown-menu"));
        button.click();
        wait.withMessage(() -> "Dropdown '" + textContent(button) + "' did not open; class='"
                        + dropdown.getAttribute("class") + "'")
                .until(d -> dropdown.getAttribute("class").contains("active") && menu.isDisplayed());
        return dropdown;
    }

    private WebElement megaMenuDropdown(String menuLabel) {
        By dropdowns = By.cssSelector(".subnav-box .dropdown");
        return allVisible(dropdowns).stream()
                .filter(d -> textContent(d.findElement(By.cssSelector(".dropdown-btn"))).equalsIgnoreCase(menuLabel))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Mega-menu entry not found: " + menuLabel));
    }

    public void openContactCentre() {
        expand(visible(CONTACT_CENTRE));
    }

    public String contactCentreText() {
        return textContent(driver.findElement(CONTACT_CENTRE).findElement(By.cssSelector(".dropdown-menu .text")));
    }

    public List<String> contactCentrePhoneHrefs() {
        return driver.findElement(CONTACT_CENTRE)
                .findElements(By.cssSelector(".dropdown-menu .buttons a")).stream()
                .filter(WebElement::isDisplayed)
                .map(a -> a.getAttribute("href"))
                .toList();
    }
}
