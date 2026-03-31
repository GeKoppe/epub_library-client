package org.koppe.epub.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.SessionExpiredException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class AuthorAdapterTest {
    private static MockWebServer server;
    private static AuthorAdapter adapter;

    @Test
    public void testAddAuthorExceptions() {
        server = new MockWebServer();
        adapter = new AuthorAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));

        try {
            AuthorDto dto = new AuthorDto();
            dto.setFirstName("Test");
            dto.setSurname("Test");

            assertThrows(IllegalArgumentException.class, () -> adapter.addAuthor(null, null));
            assertThrows(IllegalArgumentException.class, () -> adapter.addAuthor(" ", null));

            server.enqueue(new MockResponse().setResponseCode(401));
            assertThrows(SessionExpiredException.class, () -> adapter.addAuthor("fake-jwt-123", dto));

            server.enqueue(new MockResponse().setResponseCode(400));
            assertThrows(BadRequestException.class, () -> adapter.addAuthor("fake-jwt-123", dto));

            server.enqueue(new MockResponse().setResponseCode(403));
            assertThrows(ApiCallException.class, () -> adapter.addAuthor("fake-jwt-123", dto));

            server.enqueue(new MockResponse().setResponseCode(404));
            assertThrows(ApiCallException.class, () -> adapter.addAuthor("fake-jwt-123", dto));

            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(ApiCallException.class, () -> adapter.addAuthor("fake-jwt-123", dto));

            server.enqueue(new MockResponse().setResponseCode(503));
            assertThrows(ApiCallException.class, () -> adapter.addAuthor("fake-jwt-123", dto));

            server.enqueue(new MockResponse().setResponseCode(200));
            assertNull(adapter.addAuthor("fake-jwt-123", dto));

        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void testGetAuthor() {
        server = new MockWebServer();
        EpubClient client = EpubClientFactory.newCachingClient(server.url("/").toString(),
                new CacheType[] { CacheType.AUTHORS, CacheType.CREDENTIALS });
        adapter = new AuthorAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));

        try {
            assertThrows(IllegalArgumentException.class, () -> adapter.getAuthorById(null, 1L, null));
            assertThrows(IllegalArgumentException.class, () -> adapter.getAuthorById(" ", 1L, null));

            server.enqueue(new MockResponse().setResponseCode(401));
            assertThrows(SessionExpiredException.class, () -> adapter.getAuthorById("fake-jwt-123", 1L, null));

            server.enqueue(new MockResponse().setResponseCode(400));
            assertThrows(ApiCallException.class, () -> adapter.getAuthorById("fake-jwt-123", 1L, null));

            server.enqueue(new MockResponse().setResponseCode(403));
            assertThrows(ApiCallException.class, () -> adapter.getAuthorById("fake-jwt-123", 1L, null));

            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(ApiCallException.class, () -> adapter.getAuthorById("fake-jwt-123", 1L, null));

            server.enqueue(new MockResponse().setResponseCode(503));
            assertThrows(ApiCallException.class, () -> adapter.getAuthorById("fake-jwt-123", 1L, null));

            server.enqueue(new MockResponse().setResponseCode(404));
            assertNull(adapter.getAuthorById("fake-jwt-123", 1L, null));

            server.enqueue(new MockResponse().setResponseCode(200));
            assertNull(adapter.getAuthorById("fake-jwt-123", 1L, null));

            AuthorDto dto = new AuthorDto();
            dto.setId(1L);
            client.cacheValue(CacheType.AUTHORS, (Long) 1L, dto);

            client.cacheValue(CacheType.CREDENTIALS, "jwt", "123");
            client.cacheValue(CacheType.CREDENTIALS, "username", "123");
            client.cacheValue(CacheType.CREDENTIALS, "password", "123");
            client.cacheValue(CacheType.CREDENTIALS, "refresh", "123");
            assertEquals(dto, client.getAuthor(1L, null));
        } catch (Exception ex) {
            fail();
        }
    }
}
