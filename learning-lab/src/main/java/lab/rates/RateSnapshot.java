package lab.rates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Message payload: one currency rate for one day. Values in tests are synthetic. */
public record RateSnapshot(UUID messageId, LocalDate rateDate, String currency, BigDecimal buy, BigDecimal sell) {

    /** Business validation done by the consumer before touching the database. */
    public void validate() {
        if (messageId == null || rateDate == null || currency == null || buy == null || sell == null) {
            throw new IllegalArgumentException("All fields are required");
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be an ISO 4217 code: " + currency);
        }
        if (buy.signum() <= 0 || sell.signum() <= 0) {
            throw new IllegalArgumentException("Rates must be positive");
        }
        if (buy.compareTo(sell) > 0) {
            throw new IllegalArgumentException("Buy rate " + buy + " is greater than sell rate " + sell);
        }
    }
}
