package org.koppe.epub.client.http;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents all http query parameters to be added to an http request.
 */
public class HttpQuery {
    /**
     * All query parameters
     */
    private final List<QueryParameter<? extends Object>> params;

    /**
     * Default constructor
     * 
     * @param params All query parameters
     */
    private HttpQuery(List<QueryParameter<? extends Object>> params) {
        this.params = params;
    }

    /**
     * Creates a typical http query string from the existing parameters
     * 
     * @return URL encoded query string
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder().append("?");
        boolean first = true;

        for (int i = 0; i < params.size(); i++) {
            var p = params.get(i);
            if (p.getValue() == null) {
                continue;
            }
            if (!first) {
                sb.append("&");
            }
            sb.append(p.getKey()).append("=");
            if (String.class.equals(p.getType())) {
                sb.append(URLEncoder.encode((String) p.getValue(), StandardCharsets.UTF_8));
            } else if (Boolean.class.equals(p.getType())) {
                sb.append((Boolean) p.getValue() ? "true" : "false");
            } else if (LocalDate.class.equals(p.getType())) {
                sb.append(((LocalDate) p.getValue()).format(DateTimeFormatter.ISO_DATE));
            } else if (Integer.class.equals(p.getType())) {
                sb.append((Integer) p.getValue());
            } else if (Long.class.equals(p.getType())) {
                sb.append((Long) p.getValue());
            } else if (Float.class.equals(p.getType())) {
                sb.append((Float) p.getValue());
            } else if (Double.class.equals(p.getType())) {
                sb.append((Double) p.getValue());
            }
            first = false;
        }
        return sb.toString();
    }

    /**
     * Returns value query value associated with given key
     * 
     * @param key Key to get the associated value for
     * @return Value associated to given key
     */
    public @Nullable Object get(@NotNull String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException();
        }
        for (var x : params) {
            if (x.getKey().equals(key))
                return x.getValue();
        }
        return null;
    }

    // #region get with cast
    /**
     * Returns value query value associated with given key and casts it to the
     * expected class, if the associated value is of the expected type
     * 
     * @param <T>      Type to cast to
     * @param key      Key to get the associated value for
     * @param expected Expected type of the value
     * @throws ClassCastException       If the value is not of the expected type
     * @throws IllegalArgumentException If one of the needed arguments is not given
     * @return Value associated to given key
     */
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(@NotNull String key, @NotNull Class<T> expected)
            throws IllegalArgumentException, ClassCastException {
        if (key == null || key.isBlank() || expected == null) {
            throw new IllegalArgumentException();
        }

        for (var x : params) {
            if (x.getKey().equals(key)) {
                if (!x.getType().equals(expected)) {
                    throw new ClassCastException();
                }
                return (T) x.getValue();
            }
        }

        return null;
    }

    // #region exists
    /**
     * Checks if a value associated to the given key exists
     * 
     * @param key Key for which to check if an associated value exists
     * @return True, if a value exists, false otherwise or if no key is given
     */
    public boolean exists(@NotNull String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        for (var x : params) {
            if (x.getKey().equals(key))
                return true;
        }
        return false;
    }

    // #region overwrite
    /**
     * Overwrites the value for the given key
     * 
     * @param <T>   Type of the vaue
     * @param key   Key to overwrite the value for
     * @param value Value to associate with given key
     */
    public <T> void overwrite(@NotNull String key, T value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException();
        }
        for (var x : params) {
            if (x.getKey().equals(key)) {
                x.setValue(value);
                return;
            }
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    private static class QueryParameter<T> {
        private Class<T> type;
        private Object value;
        private String key;
    }

    @NoArgsConstructor
    public static class Builder {
        private final List<QueryParameter<? extends Object>> params = new ArrayList<>();

        public <T> Builder addParam(Class<T> type, String key, T value) {
            params.add(new QueryParameter<>(type, value, key));
            return this;
        }

        public HttpQuery build() {
            return new HttpQuery(params);
        }
    }
}
