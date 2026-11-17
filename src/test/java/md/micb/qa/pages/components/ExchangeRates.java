package md.micb.qa.pages.components;

import md.micb.qa.pages.BasePage;
import md.micb.qa.support.Money;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "Curs valutar" block on the home page: tabs "Numerar" (cash) and "Carduri" (card),
 * a date picker and a table built as parallel columns:
 * a list of currency names and rows with cells .buy_XXX / .sell_XXX / BNM rate.
 */
public class ExchangeRates extends BasePage {

    private static final By SECTION = By.cssSelector("section.exchange-section");
    private static final By ACTIVE_TABLE = By.cssSelector("section.exchange-section .ajax_set .tab-content.active .exchange-table");
    private static final By DATE_INPUT = By.id("date_currency");
    private static final By DATEPICKER = By.id("ui-datepicker-div");

    /** One currency row as displayed. Buy/sell are empty when the bank does not quote it ("–"). */
    public record Rate(String currency, Optional<BigDecimal> buy, Optional<BigDecimal> sell, String buyText, String sellText) {
    }

    public ExchangeRates scrollTo() {
        scrollIntoView(visible(SECTION));
        // Converter currency fields become select2 widgets once the theme script has run
        wait.withMessage("Exchange section scripts not initialised")
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("section.exchange-section select.sell-select.select2-hidden-accessible")));
        return this;
    }

    public void selectTab(String tab) {
        By tabLocator = By.cssSelector("section.exchange-section li[data-exchangetab='" + tab + "']");
        WebElement element = clickable(tabLocator);
        scrollIntoView(element);
        element.click();
        wait.withMessage("Tab '" + tab + "' did not become active")
                .until(ExpectedConditions.attributeContains(tabLocator, "class", "active"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".ajax_set .tab-content.active[data-exchangetab='" + tab + "']")));
    }

    public List<Rate> rates() {
        WebElement table = visible(ACTIVE_TABLE);
        List<String> currencies = table.findElements(By.cssSelector(".currency-list .currency")).stream()
                .map(this::currencyCode)
                .toList();
        List<Rate> result = new ArrayList<>();
        for (String code : currencies) {
            WebElement buy = table.findElement(By.cssSelector(".buy_" + code));
            WebElement sell = table.findElement(By.cssSelector(".sell_" + code));
            String buyText = textContent(buy);
            String sellText = textContent(sell);
            result.add(new Rate(code, parse(buyText), parse(sellText), buyText, sellText));
        }
        log.info("Exchange rates on page: {}", result);
        return result;
    }

    /** Bank buy rate for a currency from the cash table (used to verify the converter). */
    public BigDecimal cashBuyRate(String currency) {
        WebElement cell = visible(By.cssSelector(
                "section.exchange-section .tab-content.active[data-exchangetab='cash'] .buy_" + currency));
        return Money.parse(textContent(cell));
    }

    private Optional<BigDecimal> parse(String text) {
        return Money.isNotQuoted(text) ? Optional.empty() : Optional.of(Money.parse(text));
    }

    private String currencyCode(WebElement currencyCell) {
        // textContent is "€ EUR" -> last token is the ISO code
        String[] parts = textContent(currencyCell).split(" ");
        return parts[parts.length - 1];
    }

    /**
     * Picks a day in the previous month with the jQuery UI date picker. The site then
     * reloads the table with AJAX (action=load_exchange); we wait until the old table
     * element is replaced (staleness) instead of sleeping.
     */
    public String selectDayInPreviousMonth(int day) {
        WebElement oldTable = visible(ACTIVE_TABLE);
        WebElement dateInput = clickable(DATE_INPUT);
        // Centre the field: at the top of the viewport it is covered by the sticky header
        scrollIntoView(dateInput);
        dateInput.click();
        WebElement picker = visible(DATEPICKER);
        picker.findElement(By.cssSelector("a.ui-datepicker-prev")).click();
        By dayLink = By.xpath("//div[@id='ui-datepicker-div']//td[not(contains(@class,'ui-state-disabled'))]/a[normalize-space()='" + day + "']");
        clickable(dayLink).click();
        wait.withMessage("Exchange table was not reloaded after choosing a date")
                .until(ExpectedConditions.stalenessOf(oldTable));
        visible(ACTIVE_TABLE);
        String value = driver.findElement(DATE_INPUT).getAttribute("value");
        log.info("Selected exchange-rate date: {}", value);
        return value;
    }

    /** Values from data-sell attributes, used to check the picker really changed the data source. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rawAttributes() {
        return (Map<String, Object>) ((JavascriptExecutor) driver).executeScript(
                "const r={};document.querySelectorAll('.ajax_set .tab-content.active [data-sell]')"
                        + ".forEach(e=>r[e.className.replace('col','').trim()]=e.getAttribute('data-sell'));return r;");
    }
}
