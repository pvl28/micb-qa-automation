package md.micb.qa.pages;

import md.micb.qa.pages.components.CookieBanner;
import md.micb.qa.pages.components.Header;

public class HomePage extends BasePage {

    public HomePage open() {
        openPath("/");
        return this;
    }

    public Header header() {
        return new Header();
    }

    public CookieBanner cookieBanner() {
        return new CookieBanner();
    }

    public void reload() {
        driver.navigate().refresh();
        waitForDocumentReady();
    }
}
