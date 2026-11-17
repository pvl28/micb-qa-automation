package md.micb.qa.pages.components;

import md.micb.qa.pages.BasePage;
import md.micb.qa.support.Money;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.math.BigDecimal;

/** "Convertor valutar" (cash tab): "Vreau să vând" -> "Am să obțin". */
public class CurrencyConverter extends BasePage {

    private static final By SELL_INPUT = By.cssSelector(".cash-tab input.sellItem");
    private static final By RESULT_INPUT = By.cssSelector(".cash-tab input.buyItem");
    private static final By SELL_CURRENCY = By.cssSelector(".cash-tab select.sell-select");
    private static final By RESULT_CURRENCY = By.cssSelector(".cash-tab select.buy-select");

    /** Currencies are select2 widgets; the underlying <select> keeps the real value. */
    public String sellCurrency() {
        return new Select(driver.findElement(SELL_CURRENCY)).getFirstSelectedOption().getAttribute("value");
    }

    public String resultCurrency() {
        return new Select(driver.findElement(RESULT_CURRENCY)).getFirstSelectedOption().getAttribute("value");
    }

    public void enterSellAmount(String amount) {
        WebElement input = visible(SELL_INPUT);
        scrollIntoView(input);
        input.clear();
        input.sendKeys(amount);
        log.info("Converter: sell {} {}", amount, sellCurrency());
    }

    /** Waits until the result field is filled with a number and returns it. */
    public BigDecimal result() {
        String text = wait.withMessage("Converter result did not appear")
                .until(d -> {
                    String v = d.findElement(RESULT_INPUT).getAttribute("value");
                    return v != null && v.matches(".*\\d.*") ? v : null;
                });
        log.info("Converter: result '{}' {}", text, resultCurrency());
        return Money.parse(text);
    }
}
