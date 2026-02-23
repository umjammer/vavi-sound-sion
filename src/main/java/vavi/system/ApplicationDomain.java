package vavi.system;

import java.util.HashMap;
import java.util.Map;


public class ApplicationDomain {

    private final Map<String, Class<?>> definitions = new HashMap<>();

    public void define(String name, Class<?> type) {
        if (name == null || type == null) {
            return;
        }
        definitions.put(name, type);
    }

    public Class<?> getDefinition(String name) {
        return definitions.get(name);
    }
}
