package org.koppe.epub.client;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.http.HttpQuery;

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
        assertThrows(IllegalArgumentException.class, () -> adapter.addEpub(null, DtoRecord.epub1));
        assertThrows(IllegalArgumentException.class, () -> adapter.addEpub("   ", DtoRecord.epub1));

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

    @Test
    public void testDeleteEpubException() {
        server = new MockWebServer();
        adapter = new EpubAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));
        assertThrows(IllegalArgumentException.class, () -> adapter.deleteEpub(null, 1));
        assertThrows(IllegalArgumentException.class, () -> adapter.deleteEpub("   ", 1));

        try {
            server.enqueue(new MockResponse().setResponseCode(204));
            assertNull(adapter.deleteEpub("fake-jwt-123", 1));

            server.enqueue(new MockResponse().setResponseCode(401));
            assertThrows(SessionExpiredException.class, () -> adapter.deleteEpub("fake-jwt-123", 1));

            server.enqueue(new MockResponse().setResponseCode(403));
            assertThrows(ApiCallException.class, () -> adapter.deleteEpub("fake-jwt-123", 1));

            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(ApiCallException.class, () -> adapter.deleteEpub("fake-jwt-123", 1));

            server.enqueue(new MockResponse().setResponseCode(503));
            assertThrows(ApiCallException.class, () -> adapter.deleteEpub("fake-jwt-123", 1));

            server.enqueue(new MockResponse().setResponseCode(400));
            assertThrows(ApiCallException.class, () -> adapter.deleteEpub("fake-jwt-123", 1));
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void testGetEpubException() {
        server = new MockWebServer();
        adapter = new EpubAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));
        assertThrows(IllegalArgumentException.class, () -> adapter.getEpub(null, 1, null));
        assertThrows(IllegalArgumentException.class, () -> adapter.getEpub("   ", 1, null));

        try {
            server.enqueue(new MockResponse().setResponseCode(204));
            assertNull(adapter.getEpub("fake-jwt-123", 1, null));

            server.enqueue(new MockResponse().setResponseCode(401));
            assertThrows(SessionExpiredException.class, () -> adapter.getEpub("fake-jwt-123", 1, null));

            server.enqueue(new MockResponse().setResponseCode(403));
            assertThrows(ApiCallException.class, () -> adapter.getEpub("fake-jwt-123", 1, null));

            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(ApiCallException.class, () -> adapter.getEpub("fake-jwt-123", 1, null));

            server.enqueue(new MockResponse().setResponseCode(503));
            assertThrows(ApiCallException.class, () -> adapter.getEpub("fake-jwt-123", 1, null));

            server.enqueue(new MockResponse().setResponseCode(400));
            assertThrows(ApiCallException.class, () -> adapter.getEpub("fake-jwt-123", 1, null));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testAddEditionExceptions() {
        server = new MockWebServer();
        adapter = new EpubAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));
        assertThrows(IllegalArgumentException.class, () -> adapter.addEpubEdition(null, 1, DtoRecord.edition1));
        assertThrows(IllegalArgumentException.class, () -> adapter.addEpubEdition("   ", 1, DtoRecord.edition1));

        try {
            server.enqueue(new MockResponse().setResponseCode(204));
            assertNull(adapter.addEpubEdition("fake-jwt-123", 1, DtoRecord.edition1));

            server.enqueue(new MockResponse().setResponseCode(401));
            assertThrows(SessionExpiredException.class,
                    () -> adapter.addEpubEdition("fake-jwt-123", 1, DtoRecord.edition1));

            server.enqueue(new MockResponse().setResponseCode(404));
            assertThrows(NotFoundException.class,
                    () -> adapter.addEpubEdition("fake-jwt-123", 1, DtoRecord.edition1));

            server.enqueue(new MockResponse().setResponseCode(400));
            assertThrows(BadRequestException.class,
                    () -> adapter.addEpubEdition("fake-jwt-123", 1, DtoRecord.edition1));

            server.enqueue(new MockResponse().setResponseCode(403));
            assertThrows(ApiCallException.class,
                    () -> adapter.addEpubEdition("fake-jwt-123", 1, DtoRecord.edition1));

            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(ApiCallException.class,
                    () -> adapter.addEpubEdition("fake-jwt-123", 1, DtoRecord.edition1));

            server.enqueue(new MockResponse().setResponseCode(503));
            assertThrows(ApiCallException.class,
                    () -> adapter.addEpubEdition("fake-jwt-123", 1, DtoRecord.edition1));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testGetPagedExceptions() {
        server = new MockWebServer();
        adapter = new EpubAdapter(EpubClientFactory.newCredentialCacheClient(server.url("/").toString()));
        assertThrows(IllegalArgumentException.class, () -> adapter.getEpubsPaged(null, null));
        assertThrows(IllegalArgumentException.class, () -> adapter.getEpubsPaged("   ", null));

        try {
            server.enqueue(new MockResponse().setResponseCode(204));
            assertNull(adapter.getEpubsPaged("fake-jwt-123", null));

            server.enqueue(new MockResponse().setResponseCode(403));
            assertThrows(ApiCallException.class, () -> adapter.getEpubsPaged("fake-jwt-123", null));

            server.enqueue(new MockResponse().setResponseCode(400));
            assertThrows(ApiCallException.class, () -> adapter.getEpubsPaged("fake-jwt-123", null));

            server.enqueue(new MockResponse().setResponseCode(404));
            assertThrows(ApiCallException.class, () -> adapter.getEpubsPaged("fake-jwt-123", null));

            server.enqueue(new MockResponse().setResponseCode(500));
            assertThrows(ApiCallException.class,
                    () -> adapter.getEpubsPaged("fake-jwt-123", new HttpQuery.Builder().build()));

            server.enqueue(new MockResponse().setResponseCode(503));
            assertThrows(ApiCallException.class, () -> adapter.getEpubsPaged("fake-jwt-123", null));

            server.enqueue(new MockResponse().setResponseCode(401));
            assertThrows(SessionExpiredException.class, () -> adapter.getEpubsPaged("fake-jwt-123", null));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
