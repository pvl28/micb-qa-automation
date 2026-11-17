package md.micb.qa.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import md.micb.qa.config.Config;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin REST Assured wrapper for the two PUBLIC interfaces found on micb.md:
 * 1) WordPress REST API  GET /wp-json/wp/v2/pages  (public read of published pages)
 * 2) admin-ajax.php      POST action=load_exchange (used by the home page date picker)
 * No authentication, no write operations.
 */
public class ApiClient {

    private final RequestSpecification spec = new RequestSpecBuilder()
            .setBaseUri(Config.baseUrl())
            .addHeader("User-Agent", Config.apiUserAgent())
            .addFilter(new LoggingFilter())
            .build();

    public Response getPages(Map<String, ?> query) {
        return given(spec).accept(ContentType.JSON).queryParams(query).get("/wp-json/wp/v2/pages");
    }

    public Response getPage(long id) {
        return given(spec).accept(ContentType.JSON).get("/wp-json/wp/v2/pages/{id}", id);
    }

    public Response get(String path) {
        return given(spec).get(path);
    }

    /** Same request the site sends when a date is picked: date format dd-mm-yyyy, tab cash|card. */
    public Response loadExchange(String date, String tab) {
        return given(spec)
                .contentType(ContentType.URLENC.withCharset("UTF-8"))
                .formParam("action", "load_exchange")
                .formParam("date_value", date)
                .formParam("active_tab", tab)
                .post("/wp-admin/admin-ajax.php");
    }
}
