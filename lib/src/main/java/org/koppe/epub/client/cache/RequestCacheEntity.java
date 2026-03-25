package org.koppe.epub.client.cache;

import org.koppe.epub.client.http.HttpQuery;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;
import okhttp3.Request;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class RequestCacheEntity {
    @Include
    @lombok.ToString.Include
    private String requestGuid;
    @Include
    @lombok.ToString.Include
    private String baseUrl;
    @Include
    @lombok.ToString.Include
    private boolean paged;
    @Include
    @lombok.ToString.Include
    private int page;
    @Include
    @lombok.ToString.Include
    private int pageSize;
    @Include
    @lombok.ToString.Include
    private HttpQuery query;
    private Class<?> expectedResponse;
    private Request request;
}
