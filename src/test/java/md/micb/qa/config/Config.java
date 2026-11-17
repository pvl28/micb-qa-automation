package md.micb.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

/**
 * Reads config.properties from the classpath. A JVM system property with the same key
 * (-Dkey=value) wins, so CI can change settings without editing files.
 */
public final class Config {

    private static final Properties PROPS = load();

    private Config() {
    }

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            p.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read config.properties", e);
        }
        return p;
    }

    public static String get(String key) {
        String value = System.getProperty(key, PROPS.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration key: " + key);
        }
        return value.trim();
    }

    public static String baseUrl() {
        return get("base.url").replaceAll("/+$", "");
    }

    public static String browser() {
        return get("browser").toLowerCase();
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get("headless"));
    }

    public static int windowWidth() {
        return Integer.parseInt(get("window.width"));
    }

    public static int windowHeight() {
        return Integer.parseInt(get("window.height"));
    }

    public static Duration timeout() {
        return Duration.ofSeconds(Long.parseLong(get("timeout.seconds")));
    }

    public static Duration pageLoadTimeout() {
        return Duration.ofSeconds(Long.parseLong(get("page.load.timeout.seconds")));
    }

    public static String apiUserAgent() {
        return get("api.user.agent");
    }
}
