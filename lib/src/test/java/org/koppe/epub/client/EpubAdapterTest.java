package org.koppe.epub.client;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.SessionExpiredException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class EpubAdapterTest {
    private static MockWebServer server;
    private static EpubAdapter adapter;

    @BeforeEach
    public void setup() {
        server = new MockWebServer();
        server.setDispatcher(new MockDispatcher());
        adapter = new EpubAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));
    }

    @Test
    public void testAddEpubException() {
        server = new MockWebServer();
        adapter = new EpubAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));
        assertThrows(IllegalArgumentException.class, () -> adapter.addEpub(null, null));
        assertThrows(IllegalArgumentException.class, () -> adapter.addEpub("   ", null));

        try {
            server.enqueue(new MockResponse().setResponseCode(204));
            assertNull(adapter.addEpub("fake-jwt-123", DtoRecord.epub1));

            server.enqueue(new MockResponse().setResponseCode(404));
            assertThrows(ApiCallException.class, () -> adapter.addEpub("fake-jwt-123", DtoRecord.epub1));

            server.enqueue(new MockResponse().setResponseCode(403));
            assertThrows(ApiCallException.class, () -> adapter.addEpub("fake-jwt-123", DtoRecord.epub1));

            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(ApiCallException.class, () -> adapter.addEpub("fake-jwt-123", DtoRecord.epub1));

            server.enqueue(new MockResponse().setResponseCode(503));
            assertThrows(ApiCallException.class, () -> adapter.addEpub("fake-jwt-123", DtoRecord.epub1));

            server.enqueue(new MockResponse().setResponseCode(400));
            assertThrows(ApiCallException.class, () -> adapter.addEpub("fake-jwt-123", DtoRecord.epub1));

            server.enqueue(new MockResponse().setResponseCode(401));
            assertThrows(SessionExpiredException.class, () -> adapter.addEpub("fake-jwt-123", DtoRecord.epub1));

        } catch (Exception ex) {
            fail();
        }
    }
}
