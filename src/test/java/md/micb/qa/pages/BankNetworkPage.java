package md.micb.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * "Rețeaua băncii": radio buttons Branches/ATMs, city filter and extra checkboxes.
 * The Google Map itself is third-party and is not asserted.
 */
public class BankNetworkPage extends BasePage {

    private static final By BRANCHES_RADIO = By.id("agency_id");
    private static final By ATMS_RADIO_LABEL = By.cssSelector("label[for='atms_is']");
    private static final By ATMS_RADIO = By.id("atms_is");
    private static final By CITY_SELECT = By.id("citySelect");
    /** Checkbox rows for branches (.sc_show) and ATMs (.bc_show) */
    private static final By BRANCH_FILTERS = By.cssSelector("section.bank-network .checkbox-row.sc_show");
    private static final By ATM_FILTERS = By.cssSelector("section.bank-network .checkbox-row.bc_show");

    public BankNetworkPage open() {
        openPath("/reteaua-bancii/");
        visible(By.cssSelector("section.bank-network"));
        // Filters are shown by the page script after initialisation, not in the initial HTML render
        wait.withMessage("Branch filters were not initialised")
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(BRANCH_FILTERS));
        return this;
    }

    public boolean branchesSelected() {
        return driver.findElement(BRANCHES_RADIO).isSelected();
    }

    public void chooseAtms() {
        clickable(ATMS_RADIO_LABEL).click();
        wait.withMessage("ATM radio button not selected")
                .until(ExpectedConditions.elementSelectionStateToBe(ATMS_RADIO, true));
        wait.withMessage("ATM filters did not appear")
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(ATM_FILTERS));
        wait.withMessage("Branch filters did not hide")
                .until(ExpectedConditions.invisibilityOfAllElements(driver.findElements(BRANCH_FILTERS)));
    }

    public List<String> visibleBranchFilters() {
        return visibleLabels(BRANCH_FILTERS);
    }

    public List<String> visibleAtmFilters() {
        return visibleLabels(ATM_FILTERS);
    }

    private List<String> visibleLabels(By rows) {
        return driver.findElements(rows).stream()
                .filter(WebElement::isDisplayed)
                .map(r -> textContent(r.findElement(By.tagName("label"))))
                .toList();
    }

    public List<String> cities() {
        return new Select(driver.findElement(CITY_SELECT)).getOptions().stream()
                .map(o -> o.getAttribute("value").trim())
                .filter(v -> !v.isEmpty())
                .toList();
    }
}
