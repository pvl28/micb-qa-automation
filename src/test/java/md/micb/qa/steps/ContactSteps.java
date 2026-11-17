package md.micb.qa.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import md.micb.qa.pages.BankNetworkPage;
import md.micb.qa.pages.components.CookieBanner;
import md.micb.qa.pages.components.CreditCalculator;
import md.micb.qa.pages.components.Header;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ContactSteps {

    @When("the visitor opens {string} from the {string} menu")
    public void openFromMenu(String link, String menu) {
        new Header().openFromMegaMenu(menu, link);
    }

    @Then("the credit calculator is displayed")
    public void calculatorDisplayed() {
        CreditCalculator calculator = new CreditCalculator().scrollTo();
        assertThat(calculator.results()).as("Calculator results").containsKey(CreditCalculator.MONTHLY_PAYMENT);
    }

    @When("the visitor opens the contact centre menu")
    public void openContactCentre() {
        new Header().openContactCentre();
    }

    @Then("the contact centre text contains {string}")
    public void contactCentreText(String expected) {
        assertThat(new Header().contactCentreText()).contains(expected);
    }

    @Then("the contact centre shows at least {int} phone link with {string} scheme")
    public void phoneLinks(int min, String scheme) {
        List<String> hrefs = new Header().contactCentrePhoneHrefs();
        assertThat(hrefs).as("Phone links").hasSizeGreaterThanOrEqualTo(min);
        assertThat(hrefs).as("Phone links must be callable tel: links with digits")
                .allMatch(h -> h.startsWith(scheme) && h.substring(scheme.length()).matches("\\+?\\d{6,15}"));
    }

    @Given("the visitor opens the bank network page")
    public void openBankNetwork() {
        new BankNetworkPage().open();
        new CookieBanner().declineIfShown();
    }

    @Then("branches are selected by default")
    public void branchesDefault() {
        assertThat(new BankNetworkPage().branchesSelected()).as("'Sucursale și Agenții' selected").isTrue();
    }

    @Then("the branch filters {string} and {string} are visible")
    public void branchFilters(String first, String second) {
        assertThat(new BankNetworkPage().visibleBranchFilters()).contains(first, second);
    }

    @Then("the city filter contains:")
    public void cityFilter(List<String> cities) {
        assertThat(new BankNetworkPage().cities()).containsAll(cities);
    }

    @When("the visitor chooses ATMs")
    public void chooseAtms() {
        new BankNetworkPage().chooseAtms();
    }

    @Then("the ATM filters are visible:")
    public void atmFilters(List<String> filters) {
        assertThat(new BankNetworkPage().visibleAtmFilters()).containsExactlyInAnyOrderElementsOf(filters);
    }

    @Then("no branch filters are visible")
    public void noBranchFilters() {
        assertThat(new BankNetworkPage().visibleBranchFilters()).isEmpty();
    }
}
