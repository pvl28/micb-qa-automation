package md.micb.qa.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import md.micb.qa.pages.components.CreditCalculator;
import md.micb.qa.pages.components.PaymentScheduleModal;
import md.micb.qa.pages.components.PaymentScheduleModal.Row;
import md.micb.qa.support.Money;
import md.micb.qa.support.ScenarioContext;
import org.assertj.core.api.SoftAssertions;

import java.math.BigDecimal;
import java.util.List;

import static md.micb.qa.pages.components.CreditCalculator.EFFECTIVE_RATE;
import static md.micb.qa.pages.components.CreditCalculator.INTEREST_RATE;
import static md.micb.qa.pages.components.CreditCalculator.MONTHLY_PAYMENT;
import static md.micb.qa.pages.components.CreditCalculator.TOTAL_COST;
import static org.assertj.core.api.Assertions.assertThat;

public class CreditCalculatorSteps {

    /** Values in the schedule are rounded to cents in every row. */
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private final ScenarioContext context;
    private final CreditCalculator calculator = new CreditCalculator();

    public CreditCalculatorSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the visitor scrolls to the credit calculator")
    public void scrollToCalculator() {
        calculator.scrollTo();
    }

    @When("the visitor sets the credit amount to {long} MDL and the term to {long} months")
    public void setAmountAndTerm(long amount, long term) {
        calculator.setAmount(amount);
        calculator.setTermMonths(term);
        context.put("amount", calculator.amount());
        context.put("term", calculator.termMonths());
    }

    @When("the visitor remembers the monthly payment as {string}")
    public void rememberPayment(String key) {
        context.put(key, calculator.resultValue(MONTHLY_PAYMENT));
    }

    @Then("the monthly payment is greater than {string}")
    public void paymentGreater(String key) {
        BigDecimal base = context.get(key, BigDecimal.class);
        assertThat(calculator.resultValue(MONTHLY_PAYMENT)).as("Monthly payment vs '%s'", key).isGreaterThan(base);
    }

    @Then("the monthly payment is less than {string}")
    public void paymentLess(String key) {
        BigDecimal base = context.get(key, BigDecimal.class);
        assertThat(calculator.resultValue(MONTHLY_PAYMENT)).as("Monthly payment vs '%s'", key).isLessThan(base);
    }

    @Then("the credit amount equals the maximum shown under the slider")
    public void amountIsMax() {
        assertThat(calculator.amount()).as("Amount after entering a too large value")
                .isEqualTo(calculator.maxAmount());
    }

    @Then("the total cost of the credit is greater than the credit amount")
    public void totalCostAboveAmount() {
        long amount = context.get("amount", Long.class);
        assertThat(calculator.resultValue(TOTAL_COST)).isGreaterThan(BigDecimal.valueOf(amount));
    }

    @Then("the effective annual rate is greater than or equal to the interest rate")
    public void effectiveRateNotBelowNominal() {
        assertThat(calculator.resultValue(EFFECTIVE_RATE))
                .as("DAE (effective rate) vs nominal rate")
                .isGreaterThanOrEqualTo(calculator.resultValue(INTEREST_RATE));
    }

    @When("the visitor opens the repayment schedule")
    public void openSchedule() {
        PaymentScheduleModal modal = calculator.openSchedule();
        context.put("rows", modal.rows());
        context.put("modal", modal);
    }

    @Then("the schedule has {int} rows")
    public void scheduleRows(int term) {
        assertThat(rows()).as("Schedule rows").hasSize(term);
        assertThat(rows().get(0).month()).isEqualTo(1);
        assertThat(rows().get(term - 1).month()).isEqualTo(term);
    }

    @Then("the principal parts add up to {long} MDL")
    public void principalSum(long amount) {
        BigDecimal sum = rows().stream().map(Row::principal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).as("Σ principal").isEqualByComparingTo(BigDecimal.valueOf(amount));
        assertThat(rows().get(0).openingBalance()).as("First opening balance")
                .isEqualByComparingTo(BigDecimal.valueOf(amount));
    }

    @Then("in every row interest + principal + fee equals the payment")
    public void rowSums() {
        SoftAssertions soft = new SoftAssertions();
        for (Row r : rows()) {
            BigDecimal sum = r.interest().add(r.principal()).add(r.fee());
            soft.assertThat(Money.closeTo(sum, r.payment(), CENT))
                    .as("Month %d: %s + %s + %s = %s, payment %s", r.month(), r.interest(), r.principal(),
                            r.fee(), sum, r.payment())
                    .isTrue();
        }
        soft.assertAll();
    }

    @Then("every next opening balance equals the previous balance minus principal")
    public void balanceChain() {
        SoftAssertions soft = new SoftAssertions();
        List<Row> rows = rows();
        for (int i = 1; i < rows.size(); i++) {
            Row prev = rows.get(i - 1);
            BigDecimal expected = prev.openingBalance().subtract(prev.principal());
            soft.assertThat(Money.closeTo(rows.get(i).openingBalance(), expected, CENT))
                    .as("Month %d opening balance %s, expected %s", rows.get(i).month(),
                            rows.get(i).openingBalance(), expected)
                    .isTrue();
        }
        Row last = rows.get(rows.size() - 1);
        soft.assertThat(Money.closeTo(last.openingBalance(), last.principal(), CENT))
                .as("Last row repays the remaining balance").isTrue();
        soft.assertAll();
    }

    @Then("the schedule payments add up to the total cost")
    public void paymentsSumToTotalCost() {
        BigDecimal sum = rows().stream().map(Row::payment).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = modal().summaryValue("Cost total");
        // Each row is rounded to cents separately, so allow 1 cent per row of accumulated rounding
        BigDecimal tolerance = CENT.multiply(BigDecimal.valueOf(rows().size()));
        assertThat(Money.closeTo(sum, totalCost, tolerance))
                .as("Σ payments %s vs 'Cost total' %s (±%s)", sum, totalCost, tolerance)
                .isTrue();
    }

    @Then("the schedule monthly payment matches the calculator")
    public void scheduleMatchesCalculator() {
        BigDecimal fromCalculator = calculator.resultValue(MONTHLY_PAYMENT);
        assertThat(modal().summaryValue("Plata lunară")).as("Modal monthly payment")
                .isEqualByComparingTo(fromCalculator);
        assertThat(rows().get(0).payment()).as("First row payment").isEqualByComparingTo(fromCalculator);
    }

    @SuppressWarnings("unchecked")
    private List<Row> rows() {
        return context.get("rows", List.class);
    }

    private PaymentScheduleModal modal() {
        return context.get("modal", PaymentScheduleModal.class);
    }
}
