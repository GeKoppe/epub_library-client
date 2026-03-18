package org.koppe.epub.client.http;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
            }
        }
        return sb.toString();
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
