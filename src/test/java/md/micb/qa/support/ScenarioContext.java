package md.micb.qa.support;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-scenario storage shared between step classes.
 * PicoContainer creates a new instance for every scenario and injects it into any step
 * class that declares it in the constructor, so no static state leaks between scenarios.
 */
public class ScenarioContext {

    private final Map<String, Object> values = new HashMap<>();

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = values.get(key);
        if (value == null) {
            throw new IllegalStateException("No value stored under '" + key + "' in this scenario");
        }
        return type.cast(value);
    }
}
