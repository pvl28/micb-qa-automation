package md.micb.qa.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import md.micb.qa.pages.ContentPage;
import md.micb.qa.pages.HomePage;
import md.micb.qa.pages.components.CookieBanner;
import md.micb.qa.pages.components.Header;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NavigationSteps {

    private final HomePage home = new HomePage();

    @Given("the visitor opens the home page")
    public void openHome() {
        home.open();
        new CookieBanner().declineIfShown();
    }

    @Given("the visitor opens the home page without handling cookies")
    public void openHomeKeepBanner() {
        home.open();
    }

    @Given("the visitor opens the page {string}")
    public void openPage(String path) {
        new ContentPage().open(path);
        new CookieBanner().declineIfShown();
    }

    @When("the visitor declines optional cookies")
    public void declineCookies() {
        CookieBanner banner = home.cookieBanner();
        assertThat(banner.waitUntilShown())
                .as("Cookie banner should be shown to a new visitor")
                .isTrue();
        banner.decline();
    }

    @When("the visitor reloads the page")
    public void reload() {
        home.reload();
    }

    @Then("the cookie banner is not shown again")
    public void bannerNotShown() {
        // The banner normally appears ~1 s after load; 5 s of absence is a clear signal.
        assertThat(home.cookieBanner().staysHidden(Duration.ofSeconds(5)))
                .as("Cookie banner must not re-appear after consent was given")
                .isTrue();
    }

    @Then("the page title contains {string}")
    public void titleContains(String expected) {
        assertThat(home.title()).as("Page title").contains(expected);
    }

    @Then("the page title mentions {string} ignoring case")
    public void titleMentions(String expected) {
        assertThat(home.title()).as("Page title").containsIgnoringCase(expected);
    }

    @Then("the bank logo is visible")
    public void logoVisible() {
        assertThat(new Header().isLogoVisible()).as("Logo visible").isTrue();
    }

    @Then("the main menu contains links to:")
    public void mainMenuContains(List<String> paths) {
        List<String> hrefs = new Header().mainMenuHrefs();
        for (String path : paths) {
            assertThat(hrefs)
                    .as("Main menu links (expected one ending with %s)", path)
                    .anyMatch(h -> h.endsWith(path));
        }
    }

    @When("the visitor switches the language to {string}")
    public void switchLanguage(String code) {
        new Header().switchLanguage(code);
    }

    @Then("the page language is {string}")
    public void pageLanguage(String lang) {
        assertThat(home.htmlLang()).as("<html lang>").isEqualTo(lang);
    }

    @Then("the active language is {string}")
    public void activeLanguage(String code) {
        assertThat(new Header().activeLanguage()).as("Active language in switcher").isEqualToIgnoringCase(code);
    }

    @Then("the URL path starts with {string}")
    public void urlPathStartsWith(String prefix) {
        assertThat(path()).as("URL path").startsWith(prefix);
    }

    @Then("the URL path is {string}")
    public void urlPathIs(String expected) {
        assertThat(path()).as("URL path").isEqualTo(expected);
    }

    @Then("the page has a heading")
    public void pageHasHeading() {
        assertThat(new ContentPage().heading()).as("Page heading").isNotBlank();
    }

    @Then("the current breadcrumb is {string}")
    public void currentBreadcrumb(String expected) {
        assertThat(new ContentPage().currentBreadcrumb()).as("Last breadcrumb").isEqualTo(expected);
    }

    private String path() {
        return URI.create(home.currentUrl()).getPath();
    }
}
