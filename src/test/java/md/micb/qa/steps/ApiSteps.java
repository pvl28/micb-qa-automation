package md.micb.qa.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import md.micb.qa.api.ApiClient;
import md.micb.qa.api.ExchangeHtmlParser;
import md.micb.qa.api.ExchangeHtmlParser.Quote;
import md.micb.qa.config.Config;
import md.micb.qa.support.ScenarioContext;
import org.assertj.core.api.SoftAssertions;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSteps {

    private static final DateTimeFormatter SITE_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ApiClient api = new ApiClient();
    private final ScenarioContext context;

    public ApiSteps(ScenarioContext context) {
        this.context = context;
    }

    private Response response() {
        return context.get("response", Response.class);
    }

    // ---------- requests ----------

    @When("a client requests pages with parameters:")
    public void requestPages(Map<String, String> params) {
        context.put("response", api.getPages(params));
    }

    @When("a client requests the page with id {long}")
    public void requestPage(long id) {
        context.put("response", api.getPage(id));
    }

    @When("a client sends GET {string}")
    public void sendGet(String path) {
        context.put("response", api.get(path));
    }

    @When("a client requests exchange rates for the date {int} days ago and tab {string}")
    public void requestRates(int daysAgo, String tab) {
        String date = LocalDate.now().minusDays(daysAgo).format(SITE_DATE);
        context.put("date", date);
        context.put("tab", tab);
        context.put("response", api.loadExchange(date, tab));
    }

    @When("the client requests the same date again")
    public void requestSameDateAgain() {
        context.put("firstResponse", response());
        context.put("response", api.loadExchange(context.get("date", String.class), context.get("tab", String.class)));
    }

    // ---------- generic checks ----------

    @Then("the response status is {int}")
    public void status(int expected) {
        assertThat(response().statusCode()).as("HTTP status").isEqualTo(expected);
    }

    @Then("the response is JSON")
    public void isJson() {
        assertThat(response().contentType()).startsWith("application/json");
    }

    @Then("the response is HTML")
    public void isHtml() {
        assertThat(response().contentType()).startsWith("text/html");
    }

    @Then("the error code is {string}")
    public void errorCode(String code) {
        JsonPath json = response().jsonPath();
        assertThat(json.getString("code")).as("WordPress error code").isEqualTo(code);
        assertThat(json.getInt("data.status")).as("data.status mirrors HTTP status").isEqualTo(response().statusCode());
        assertThat(json.getString("message")).as("Human readable message").isNotBlank();
    }

    @Then("the error details mention parameter {string}")
    public void errorDetailsParam(String param) {
        Map<String, Object> params = response().jsonPath().getMap("data.params");
        assertThat(params).as("data.params of validation error").containsKey(param);
    }

    // ---------- WordPress pages ----------

    @Then("the response contains {int} items")
    public void itemsCount(int expected) {
        assertThat(response().jsonPath().getList("$")).as("Items in JSON array").hasSize(expected);
    }

    @Then("the pagination headers are consistent for page size {int}")
    public void paginationHeaders(int pageSize) {
        int total = Integer.parseInt(response().header("X-WP-Total"));
        int pages = Integer.parseInt(response().header("X-WP-TotalPages"));
        assertThat(total).as("X-WP-Total").isPositive();
        assertThat(pages).as("X-WP-TotalPages = ceil(total / per_page)")
                .isEqualTo((int) Math.ceil(total / (double) pageSize));
    }

    @Then("every page item has a positive id, a slug, a title and a link on the site")
    public void pageItemsWellFormed() {
        List<Map<String, Object>> items = response().jsonPath().getList("$");
        String host = URI.create(Config.baseUrl()).getHost();
        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> item : items) {
            soft.assertThat(((Number) item.get("id")).longValue()).as("id").isPositive();
            soft.assertThat((String) item.get("slug")).as("slug of %s", item.get("id")).matches("[a-z0-9-]+");
            @SuppressWarnings("unchecked")
            Map<String, Object> title = (Map<String, Object>) item.get("title");
            soft.assertThat(title).as("title of %s", item.get("id")).isNotNull();
            if (title != null) {
                soft.assertThat((String) title.get("rendered")).as("title.rendered of %s", item.get("id")).isNotBlank();
            }
            URI link = URI.create((String) item.get("link"));
            soft.assertThat(link.getScheme()).as("link scheme").isEqualTo("https");
            soft.assertThat(link.getHost()).as("link host").isEqualTo(host);
        }
        // _fields was honoured: no other fields are returned
        soft.assertThat(items).allSatisfy(i -> assertThat(i.keySet()).containsOnly("id", "slug", "link", "title"));
        soft.assertAll();
    }

    @Then("the only page has slug {string} and link path {string}")
    public void onlyPage(String slug, String path) {
        JsonPath json = response().jsonPath();
        assertThat(json.getString("[0].slug")).isEqualTo(slug);
        assertThat(URI.create(json.getString("[0].link")).getPath()).isEqualTo(path);
    }

    // ---------- exchange rates ----------

    private List<Quote> quotes() {
        return ExchangeHtmlParser.parse(response().asString(), context.get("tab", String.class));
    }

    @Then("the active tab in the response is {string}")
    public void activeTab(String tab) {
        assertThat(ExchangeHtmlParser.activeTab(response().asString())).isEqualTo(tab);
    }

    @Then("the {string} rates contain currencies {string}")
    public void ratesContain(String tab, String csv) {
        List<String> expected = Arrays.stream(csv.split(",")).map(String::trim).toList();
        List<String> actual = ExchangeHtmlParser.parse(response().asString(), tab).stream().map(Quote::currency).toList();
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Then("EUR and USD have buy and sell rates")
    public void eurUsdQuoted() {
        for (Quote q : quotes()) {
            if (q.currency().equals("EUR") || q.currency().equals("USD")) {
                assertThat(q.buy()).as("%s buy", q.currency()).isPresent();
                assertThat(q.sell()).as("%s sell", q.currency()).isPresent();
            }
        }
    }

    @Then("every quoted buy rate is positive and not greater than the sell rate")
    public void buyPositiveNotAboveSell() {
        SoftAssertions soft = new SoftAssertions();
        for (Quote q : quotes()) {
            q.buy().ifPresent(b -> soft.assertThat(b).as("%s buy", q.currency()).isPositive());
            if (q.buy().isPresent() && q.sell().isPresent()) {
                soft.assertThat(q.buy().get()).as("%s buy <= sell", q.currency())
                        .isLessThanOrEqualTo(q.sell().get());
            }
        }
        soft.assertAll();
    }

    @Then("every BNM rate is positive")
    public void bnmPositive() {
        SoftAssertions soft = new SoftAssertions();
        for (Quote q : quotes()) {
            soft.assertThat(q.bnm()).as("%s BNM rate present", q.currency()).isPresent();
            q.bnm().ifPresent(v -> soft.assertThat(v).as("%s BNM rate", q.currency()).isPositive());
        }
        soft.assertAll();
    }

    @Then("both responses contain identical rates")
    public void identicalRates() {
        String tab = context.get("tab", String.class);
        List<Quote> first = ExchangeHtmlParser.parse(context.get("firstResponse", Response.class).asString(), tab);
        List<Quote> second = quotes();
        assertThat(first).as("Rates for %s", context.get("date", String.class)).isNotEmpty();
        assertThat(second).isEqualTo(first);
    }
}
