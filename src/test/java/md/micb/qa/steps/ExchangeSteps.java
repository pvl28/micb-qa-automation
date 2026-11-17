package md.micb.qa.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import md.micb.qa.pages.components.CurrencyConverter;
import md.micb.qa.pages.components.ExchangeRates;
import md.micb.qa.pages.components.ExchangeRates.Rate;
import md.micb.qa.support.Money;
import md.micb.qa.support.ScenarioContext;
import org.assertj.core.api.SoftAssertions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExchangeSteps {

    /** Converter tolerance: one cent. See README "Dynamic data and rounding". */
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private final ScenarioContext context;

    // ScenarioContext is injected by cucumber-picocontainer (new instance per scenario)
    public ExchangeSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the visitor scrolls to the exchange rates")
    public void scrollToRates() {
        new ExchangeRates().scrollTo();
    }

    @When("the visitor selects the {string} rates tab")
    public void selectTab(String tab) {
        new ExchangeRates().selectTab(tab);
    }

    @Then("the cash rates table lists currencies:")
    @Then("the card rates table lists currencies:")
    public void tableListsCurrencies(List<String> expected) {
        List<String> actual = new ExchangeRates().rates().stream().map(Rate::currency).toList();
        assertThat(actual).as("Currencies in the active rates table").containsExactlyElementsOf(expected);
    }

    @Then("every quoted currency has positive buy and sell rates with 2 decimals")
    public void ratesPositiveTwoDecimals() {
        SoftAssertions soft = new SoftAssertions();
        for (Rate r : new ExchangeRates().rates()) {
            r.buy().ifPresent(v -> soft.assertThat(v).as("%s buy", r.currency()).isPositive());
            r.sell().ifPresent(v -> soft.assertThat(v).as("%s sell", r.currency()).isPositive());
            if (r.buy().isPresent()) {
                soft.assertThat(r.buyText()).as("%s buy format", r.currency()).matches("\\d+\\.\\d{2}");
            }
            if (r.sell().isPresent()) {
                soft.assertThat(r.sellText()).as("%s sell format", r.currency()).matches("\\d+\\.\\d{2}");
            }
        }
        soft.assertAll();
    }

    @Then("every quoted currency has a buy rate lower than or equal to the sell rate")
    public void buyNotAboveSell() {
        // Soft assertions report ALL broken currencies at once, not only the first one
        SoftAssertions soft = new SoftAssertions();
        for (Rate r : new ExchangeRates().rates()) {
            if (r.buy().isPresent() && r.sell().isPresent()) {
                soft.assertThat(r.buy().get())
                        .as("%s: bank buy rate must not exceed sell rate", r.currency())
                        .isLessThanOrEqualTo(r.sell().get());
            }
        }
        soft.assertAll();
    }

    @Then("EUR and USD are quoted")
    public void eurUsdQuoted() {
        List<Rate> rates = new ExchangeRates().rates();
        for (String code : List.of("EUR", "USD")) {
            Rate rate = rates.stream().filter(r -> r.currency().equals(code)).findFirst()
                    .orElseThrow(() -> new AssertionError(code + " missing in rates table"));
            assertThat(rate.buy()).as("%s buy rate quoted", code).isPresent();
            assertThat(rate.sell()).as("%s sell rate quoted", code).isPresent();
        }
    }

    @Given("the converter sells {string} for {string}")
    public void converterCurrencies(String from, String to) {
        CurrencyConverter converter = new CurrencyConverter();
        // These are the default selections on the site; the step verifies the precondition
        assertThat(converter.sellCurrency()).as("Converter 'sell' currency").isEqualTo(from);
        assertThat(converter.resultCurrency()).as("Converter 'get' currency").isEqualTo(to);
    }

    @When("the visitor enters {bigdecimal} in the converter")
    public void enterAmount(BigDecimal amount) {
        context.put("amount", amount);
        new CurrencyConverter().enterSellAmount(amount.toPlainString());
    }

    @Then("the converter result equals {bigdecimal} multiplied by the cash buy rate of {string}, rounded to 2 decimals")
    public void converterResult(BigDecimal amount, String currency) {
        BigDecimal rate = new ExchangeRates().cashBuyRate(currency);
        BigDecimal expected = Money.round2(amount.multiply(rate));
        BigDecimal actual = new CurrencyConverter().result();
        assertThat(Money.closeTo(actual, expected, CENT))
                .as("Converter: %s %s x buy rate %s = expected %s (±%s), actual %s",
                        amount, currency, rate, expected, CENT, actual)
                .isTrue();
    }

    @When("the visitor selects day {int} of the previous month in the rates date picker")
    public void selectPastDate(int day) {
        String shown = new ExchangeRates().selectDayInPreviousMonth(day);
        LocalDate expectedDate = LocalDate.now().minusMonths(1).withDayOfMonth(day);
        context.put("expectedDate", expectedDate);
        context.put("shownDate", shown);
    }

    @Then("the rates date field shows the selected date")
    public void dateFieldShowsDate() {
        LocalDate expected = context.get("expectedDate", LocalDate.class);
        String shown = context.get("shownDate", String.class);
        // The site's picker uses jQuery UI format "dd-mm-yy" (e.g. 10-10-2026)
        assertThat(shown).as("Date picker value")
                .isEqualTo(expected.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }
}
