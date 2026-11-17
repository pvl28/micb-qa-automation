package md.micb.qa.api;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs every API call (method, URL, status, time) to the run log, and keeps the last
 * exchange so a failed scenario can attach the full request/response to the report.
 */
public class LoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);
    private static final ThreadLocal<String> LAST = new ThreadLocal<>();

    @Override
    public Response filter(FilterableRequestSpecification req, FilterableResponseSpecification res, FilterContext ctx) {
        long start = System.nanoTime();
        Response response = ctx.next(req, res);
        long ms = (System.nanoTime() - start) / 1_000_000;
        LOG.info("{} {} -> {} ({} ms)", req.getMethod(), req.getURI(), response.getStatusCode(), ms);
        String body = response.asString();
        LAST.set(req.getMethod() + " " + req.getURI()
                + (req.getFormParams().isEmpty() ? "" : "\nform: " + req.getFormParams())
                + "\n\nHTTP " + response.getStatusCode() + "\n" + response.getHeaders()
                + "\n\n" + (body.length() > 20_000 ? body.substring(0, 20_000) + "\n...[truncated]" : body));
        return response;
    }

    public static String lastExchange() {
        return LAST.get();
    }

    public static void clear() {
        LAST.remove();
    }
}
