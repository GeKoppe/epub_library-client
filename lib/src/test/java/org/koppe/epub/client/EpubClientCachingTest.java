package org.koppe.epub.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.http.EpubQueryBuilder;

import okhttp3.mockwebserver.MockWebServer;

public class EpubClientCachingTest {
    private static MockWebServer server = new MockWebServer();

    @Test
    public void testRequestCaching() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        EpubDto dto1 = null;
        EpubDto dto2 = null;

        try {
            dto1 = client.getEpub("admin", "admin", 1,
                    EpubQueryBuilder.newInstance().withAuthors(true).withGenres(true).build());
            String lastRequest = client.getLastRequestGuid();
            dto2 = (EpubDto) client.redoRequest(lastRequest);
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(dto2);
        assertEquals(dto1, dto2);
    }

    @Test
    public void testEntityCaching() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCachingClient(server.url("/").toString(),
                new CacheType[] { CacheType.EPUBS, CacheType.CREDENTIALS });

        EpubDto dto1 = null;
        EpubDto dto2 = null;
        String requestGuid1 = null;
        String requestGuid2 = null;

        try {
            dto1 = client.getEpub("admin", "admin", 1, null);
            requestGuid1 = client.getLastRequestGuid();
            dto2 = client.getEpub(1, null);
            requestGuid2 = client.getLastRequestGuid();
        } catch (Exception ex) {
            fail();
        }

        assertEquals(dto1, dto2);
        assertEquals(requestGuid1, requestGuid2);
    }
}
