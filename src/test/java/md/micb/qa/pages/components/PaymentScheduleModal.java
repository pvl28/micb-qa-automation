package md.micb.qa.pages.components;

import md.micb.qa.pages.BasePage;
import md.micb.qa.support.Money;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * "Grafic de rambursare" modal. Columns (visible ones):
 * Luna | Data | Sold inițial | Dobândă calculată | Rată lunară | Comision | Plată lunară.
 */
public class PaymentScheduleModal extends BasePage {

    private static final By MODAL = By.cssSelector(".payment-schedule-modal");

    /** One schedule row converted to numbers. */
    public record Row(int month, String date, BigDecimal openingBalance, BigDecimal interest,
                      BigDecimal principal, BigDecimal fee, BigDecimal payment) {
    }

    PaymentScheduleModal waitUntilOpen() {
        wait.withMessage("Payment schedule modal did not open")
                .until(d -> modal() != null && !modal().findElements(By.cssSelector("tbody tr")).isEmpty());
        return this;
    }

    private WebElement modal() {
        return driver.findElements(MODAL).stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
    }

    /**
     * Reads the whole table with one JavaScript call. Reading ~250 cells one by one through
     * WebDriver would be slow; here JS is used only to READ data, never to click or type.
     * Hidden columns (display:none) are skipped, exactly like a user would not see them.
     */
    @SuppressWarnings("unchecked")
    public List<Row> rows() {
        List<List<String>> raw = (List<List<String>>) ((JavascriptExecutor) driver).executeScript(
                "const m=arguments[0];"
                        + "return [...m.querySelectorAll('tbody tr')].map(r=>[...r.cells]"
                        + ".filter(c=>getComputedStyle(c).display!=='none').map(c=>c.textContent.trim()));",
                modal());
        List<String> headers = headers();
        if (!headers.equals(List.of("Luna", "Data", "Sold inițial al creditului", "Dobândă calculată",
                "Rată lunară", "Comision", "Plată lunară"))) {
            throw new AssertionError("Unexpected schedule columns: " + headers);
        }
        return raw.stream().map(c -> new Row(
                Integer.parseInt(c.get(0)), c.get(1), Money.parse(c.get(2)), Money.parse(c.get(3)),
                Money.parse(c.get(4)), Money.parse(c.get(5)), Money.parse(c.get(6)))).toList();
    }

    @SuppressWarnings("unchecked")
    public List<String> headers() {
        return (List<String>) ((JavascriptExecutor) driver).executeScript(
                "return [...arguments[0].querySelectorAll('thead td')]"
                        + ".filter(c=>getComputedStyle(c).display!=='none').map(c=>c.textContent.trim());",
                modal());
    }

    /** Summary block at the top of the modal: label -> value text. */
    @SuppressWarnings("unchecked")
    public Map<String, String> summary() {
        return (Map<String, String>) ((JavascriptExecutor) driver).executeScript(
                "const r={};arguments[0].querySelectorAll('.main-info li').forEach(li=>{"
                        + "if(getComputedStyle(li).display!=='none'){r[li.querySelector('p').textContent.trim()]="
                        + "li.querySelector('b').textContent.trim();}});return r;",
                modal());
    }

    public BigDecimal summaryValue(String label) {
        String text = summary().get(label);
        if (text == null) {
            throw new AssertionError("Summary item '" + label + "' not found: " + summary());
        }
        return Money.parse(text);
    }
}
