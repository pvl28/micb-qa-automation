package md.micb.qa.api;

import md.micb.qa.support.Money;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * load_exchange returns an HTML fragment (the server renders the table), not JSON.
 * jsoup turns it into objects so we can assert on data instead of on raw strings.
 */
public final class ExchangeHtmlParser {

    public record Quote(String currency, Optional<BigDecimal> buy, Optional<BigDecimal> sell, Optional<BigDecimal> bnm) {
    }

    private ExchangeHtmlParser() {
    }

    /** The fragment contains both tabs ("cash", "card"); the requested one is marked "active". */
    public static String activeTab(String html) {
        Element active = Jsoup.parseBodyFragment(html).selectFirst(".tab-content.active");
        return active == null ? null : active.attr("data-exchangetab");
    }

    public static List<Quote> parse(String html, String tab) {
        Document doc = Jsoup.parseBodyFragment(html);
        // jsoup lower-cases attribute names: data-exchangeTab -> data-exchangetab
        Element table = doc.selectFirst(".tab-content[data-exchangetab=" + tab + "] .exchange-table");
        if (table == null) {
            throw new AssertionError("No exchange table for tab '" + tab + "' in response:\n" + abbreviate(html));
        }
        List<String> codes = table.select(".currency-list .currency").stream()
                .map(e -> {
                    String[] parts = e.text().trim().split("\\s+");
                    return parts[parts.length - 1];
                })
                .toList();
        List<Element> rows = table.select(".table .row");
        if (rows.size() != codes.size()) {
            throw new AssertionError("Currencies (" + codes.size() + ") and rate rows (" + rows.size() + ") differ");
        }
        List<Quote> quotes = new ArrayList<>();
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            Element row = rows.get(i);
            List<Element> cols = row.select(".col");
            quotes.add(new Quote(code,
                    value(row.selectFirst(".buy_" + code)),
                    value(row.selectFirst(".sell_" + code)),
                    cols.size() > 2 ? value(cols.get(2)) : Optional.empty()));
        }
        return quotes;
    }

    private static Optional<BigDecimal> value(Element cell) {
        if (cell == null) {
            return Optional.empty();
        }
        String text = cell.text().replace(' ', ' ').trim();
        return Money.isNotQuoted(text) ? Optional.empty() : Optional.of(Money.parse(text));
    }

    private static String abbreviate(String s) {
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
