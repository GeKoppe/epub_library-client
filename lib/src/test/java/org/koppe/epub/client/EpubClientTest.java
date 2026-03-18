package org.koppe.epub.client;

import org.junit.jupiter.api.Test;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.CacheMissException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.http.EpubQueryBuilder;

public class EpubClientTest {
    @Test
    public void testLogin() throws IllegalArgumentException, IllegalStateException, ApiCallException,
            CacheMissException, SessionExpiredException {
        EpubClient client = EpubClientFactory.newCredentialCacheClient("http://localhost:9093");
        EpubDto dto = new EpubDto();
        dto.setTitle("Test book 16");

        var first = client.getEpub("Test2", "elo", 1, null);
        var second = client.getEpub(1, new EpubQueryBuilder().withAuthors(true).build());
        System.out.println(first + "" + second);
    }
}
