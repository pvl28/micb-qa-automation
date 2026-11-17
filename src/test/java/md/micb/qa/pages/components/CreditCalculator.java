package md.micb.qa.pages.components;

import md.micb.qa.pages.BasePage;
import md.micb.qa.support.Money;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Credit calculator on the home page (WordPress plugin "micb-credit-calculator").
 * Inputs are text fields bound to noUiSlider handles; results are a list of label/value pairs.
 * Labels are read on the Romanian page, which is the site default.
 */
public class CreditCalculator extends BasePage {

    public static final String MONTHLY_PAYMENT = "Plata lunară";
    public static final String TOTAL_COST = "Costul total al creditului";
    public static final String INTEREST_RATE = "Rata dobânzii";
    public static final String EFFECTIVE_RATE = "Dobânda anuală efectivă";

    private static final By SECTION = By.cssSelector("section.credit-calculator");
    private static final By SLIDER_GROUPS = By.cssSelector("section.credit-calculator .slider-group");
    private static final By RESULT_ITEMS = By.cssSelector("section.credit-calculator .right-box .box-body li");
    private static final By SCHEDULE_BUTTON = By.cssSelector("section.credit-calculator a[href='#payment-schedule-modal']");

    public CreditCalculator scrollTo() {
        scrollIntoView(visible(SECTION));
        // The plugin script turns the inputs into noUiSlider widgets when it initialises
        wait.withMessage("Credit calculator was not initialised (no noUiSlider)")
                .until(d -> d.findElements(By.cssSelector("section.credit-calculator .noUi-handle")).size() >= 2);
        return this;
    }

    public void setAmount(long amount) {
        setSliderValue(0, amount);
    }

    public void setTermMonths(long months) {
        setSliderValue(1, months);
    }

    public long amount() {
        return sliderValue(0);
    }

    public long termMonths() {
        return sliderValue(1);
    }

    /** Upper bound printed under the amount slider, e.g. "400 000 MDL". */
    public long maxAmount() {
        List<WebElement> range = sliderGroup(0).findElements(By.cssSelector(".admissible-range span"));
        return Money.parse(textContent(range.get(range.size() - 1))).longValueExact();
    }

    /**
     * Same as a user: click the field (the plugin selects its text on focus), type, press Enter.
     * On Enter the plugin clamps the value to the allowed range, rounds it to the slider step,
     * reformats the field ("50 000") and recalculates. We wait until the slider handle reports
     * the same value as the field, i.e. the value was committed.
     * The slider DOM can be re-created by the plugin, so stale elements are re-located.
     */
    private void setSliderValue(int index, long value) {
        WebElement input = sliderGroup(index).findElement(By.cssSelector("input.slider-value"));
        scrollIntoView(input);
        input.click();
        input.sendKeys(String.valueOf(value), Keys.ENTER);
        wait.ignoring(StaleElementReferenceException.class)
                .withMessage("Calculator did not apply value " + value)
                .until(d -> {
                    String text = sliderGroup(index).findElement(By.cssSelector("input.slider-value")).getAttribute("value");
                    long field = Money.parse(text).longValue();
                    return field == sliderValue(index) && !input.equals(d.switchTo().activeElement());
                });
        log.info("Calculator input #{} typed {} -> applied {}", index, value, sliderValue(index));
    }

    private long sliderValue(int index) {
        String now = sliderGroup(index).findElement(By.cssSelector(".noUi-handle")).getAttribute("aria-valuenow");
        return new BigDecimal(now).longValue();
    }

    private WebElement sliderGroup(int index) {
        return allVisible(SLIDER_GROUPS).get(index);
    }

    /** Visible result rows as label -> value text. */
    public Map<String, String> results() {
        Map<String, String> map = new LinkedHashMap<>();
        for (WebElement li : driver.findElements(RESULT_ITEMS)) {
            if (li.isDisplayed()) {
                map.put(textContent(li.findElement(By.tagName("p"))), textContent(li.findElement(By.tagName("b"))));
            }
        }
        return map;
    }

    public BigDecimal resultValue(String label) {
        String text = results().get(label);
        if (text == null) {
            throw new AssertionError("Result '" + label + "' not found. Visible results: " + results());
        }
        return Money.parse(text);
    }

    public PaymentScheduleModal openSchedule() {
        clickable(SCHEDULE_BUTTON).click();
        return new PaymentScheduleModal().waitUntilOpen();
    }
}
