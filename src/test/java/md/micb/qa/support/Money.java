package md.micb.qa.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Parses amounts as the site prints them. Observed formats:
 * "3 653,92 MDL" (calculator, comma decimal, space thousands),
 * "1 818.00" (converter, dot decimal), "20.05" (rates), "11,40%".
 * BigDecimal is used instead of double so that money comparisons are exact.
 */
public final class Money {

    private Money() {
    }

    public static BigDecimal parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Amount text is null");
        }
        String cleaned = text
                .replace("MDL", "")
                .replace("%", "")
                .replace(' ', ' ')
                .replace(" ", "")
                .replace(",", ".")
                .trim();
        if (cleaned.isEmpty() || !cleaned.matches("-?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("Not a number: '" + text + "'");
        }
        return new BigDecimal(cleaned);
    }

    /** True for "–" and similar placeholders the site uses when a currency is not traded. */
    public static boolean isNotQuoted(String text) {
        String t = text == null ? "" : text.replace(' ', ' ').trim();
        return t.isEmpty() || t.equals("–") || t.equals("-");
    }

    public static BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** |a - b| <= tolerance */
    public static boolean closeTo(BigDecimal a, BigDecimal b, BigDecimal tolerance) {
        return a.subtract(b).abs().compareTo(tolerance) <= 0;
    }
}
