package lab.rates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: run everywhere, no Docker needed. */
class RateSnapshotTest {

    private static RateSnapshot of(String currency, String buy, String sell) {
        return new RateSnapshot(UUID.randomUUID(), LocalDate.of(2026, 1, 15), currency,
                new BigDecimal(buy), new BigDecimal(sell));
    }

    @Test
    void validSnapshotPasses() {
        assertThatCode(() -> of("EUR", "19.95", "20.45").validate()).doesNotThrowAnyException();
    }

    @Test
    void equalBuyAndSellIsAllowed() {
        assertThatCode(() -> of("USD", "18.00", "18.00").validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} buy={1} sell={2} -> rejected")
    @CsvSource({
            "EUR, 20.50, 20.45",   // buy > sell
            "EUR, 0, 20.45",       // zero
            "EUR, -1, 20.45",      // negative
            "eur, 19.95, 20.45",   // not ISO upper-case
            "EURO, 19.95, 20.45"   // 4 letters
    })
    void invalidSnapshotsAreRejected(String currency, String buy, String sell) {
        assertThatThrownBy(() -> of(currency, buy, sell).validate()).isInstanceOf(IllegalArgumentException.class);
    }
}
