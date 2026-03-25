package org.koppe.epub.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.koppe.epub.client.dto.CredentialDto;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.dto.EpubEditionDto;
import org.koppe.epub.client.dto.PagedRequestDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.CacheMissException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.koppe.epub.client.http.EpubQueryBuilder;

import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class EpubClientCommunicationTest {

    private static MockWebServer server = new MockWebServer();

    // #region login
    @Test
    public void testLogin() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        assertThrows(CacheMissException.class, client::login);

        assertThrows(IllegalArgumentException.class, () -> client.login(null, null));
        assertThrows(IllegalArgumentException.class, () -> client.login("      ", null));
        assertThrows(IllegalArgumentException.class, () -> client.login(null, "     "));
        assertThrows(IllegalArgumentException.class, () -> client.login("    ", "    "));
        assertThrows(SessionExpiredException.class, () -> client.login("admin", "test"));

        assertThrows(SessionExpiredException.class, client::login);

        CredentialDto dto = null;
        try {
            dto = client.login("admin", "admin");
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(dto);
        assertEquals("admin", dto.getUsername());
        assertEquals("fake-jwt-123", dto.getJwt());
        assertEquals("fake-refresh-123", dto.getRefresh());

        try {
            dto = client.login();
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(dto);
        assertEquals("admin", dto.getUsername());
        assertEquals("fake-jwt-123", dto.getJwt());
        assertEquals("fake-refresh-123", dto.getRefresh());
    }

    // #region add epub
    @Test
    public void testAddEpub() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        assertThrows(CacheMissException.class, () -> client.addEpub(null));
        assertThrows(IllegalArgumentException.class, () -> client.addEpub("admin", "admin", null));
        assertThrows(IllegalArgumentException.class, () -> client.addEpub("admin", "admin", new EpubDto()));

        EpubDto body = new EpubDto();
        body.setTitle("    ");
        assertThrows(IllegalArgumentException.class, () -> client.addEpub("admin", "admin", body));

        body.setTitle("Test");
        body.setPublishDate(LocalDate.of(1999, 2, 3));

        EpubDto response = null;
        try {
            response = client.addEpub(body);
        } catch (IllegalArgumentException | CacheMissException | ApiCallException | SessionExpiredException e) {
            fail();
        }

        assertNotNull(response);
        assertEquals("Test", response.getTitle());
        assertNotNull(response.getUploadDate());
        assertEquals(LocalDate.of(1999, 2, 3), response.getPublishDate());
        assertNotNull(response.getId());
    }

    // #region get by id
    @Test
    public void testGetEpubById() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        assertThrows(CacheMissException.class, () -> client.getEpub(1, null));

        EpubDto epub = null;
        try {
            epub = client.getEpub("admin", "admin", 3,
                    new EpubQueryBuilder().withAuthors(false).withTags(false).withGenres(false).build());
        } catch (Exception ex) {
            fail();
        }

        assertNull(epub);
        try {
            epub = client.getEpub(1,
                    new EpubQueryBuilder().withAuthors(false).withTags(false).withGenres(false).build());
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(epub);
        assertEquals((Long) 1L, epub.getId());
        assertEquals("epub 1", epub.getTitle());
        assertNotNull(epub.getAuthors());
        assertNotNull(epub.getTags());
        assertNotNull(epub.getGenres());
    }

    @Test
    public void testDeleteEpub() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        try {
            assertNull(client.deleteEpub("admin", "admin", 3));
        } catch (Exception ex) {
            fail();
        }

        EpubDto dto = null;
        try {
            dto = client.deleteEpub(1);
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(dto);
        assertEquals("epub 1", dto.getTitle());
    }

    // #region add epub edition
    @Test
    public void testAddEpubEdition() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());
        EpubEditionDto dto = new EpubEditionDto();

        assertThrows(IllegalArgumentException.class, () -> client.addEpubEdition("admin", "admin", 1, null));
        assertThrows(IllegalArgumentException.class, () -> client.addEpubEdition("admin", "admin", 1, dto));

        dto.setVersionName("   ");
        assertThrows(IllegalArgumentException.class, () -> client.addEpubEdition("admin", "admin", 1, dto));

        dto.setVersionName("Test");
        assertThrows(NotFoundException.class, () -> client.addEpubEdition("admin", "admin", 3, dto));

        EpubEditionDto created = null;
        try {
            created = client.addEpubEdition(1, dto);
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(created);
        assertEquals(dto.getVersionName(), created.getVersionName());
        assertNotNull(created.getUploadGuid());
        assertNotNull(created.getDownloadGuid());
        assertNotNull(created.getId());
    }

    // #region get all epubs
    @Test
    public void testGetAllEpubs() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        PagedRequestDto<EpubDto> response = null;

        try {
            response = client.getAllEpubs("admin", "admin", new EpubQueryBuilder().page(0).pageSize(1).build());
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(DtoRecord.epub1, response.getContent().get(0));

        try {
            response = client.getAllEpubs("admin", "admin", new EpubQueryBuilder().page(2).pageSize(1).build());
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(response);
        assertEquals(0, response.getContent().size());

        try {
            response = client.getAllEpubs("admin", "admin", new EpubQueryBuilder().page(1).pageSize(2).build());
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(response);
        assertEquals(1, response.getContent().size());

        try {
            response = client.getAllEpubs("admin", "admin", null);
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
    }

    // #region execute request
    @Test
    public void testExecuteRequest() {
        server = new MockWebServer();
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        server.enqueue(new MockResponse().setResponseCode(204));
        server.enqueue(new MockResponse().setResponseCode(400));
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setResponseCode(403));
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(503));

        try {
            assertNull(client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                    Void.class,
                    null,
                    false));
        } catch (SessionExpiredException | BadRequestException | ForbiddenException | NotFoundException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            fail();
        }
        assertThrows(BadRequestException.class,
                () -> client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(SessionExpiredException.class,
                () -> client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(ForbiddenException.class,
                () -> client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(NotFoundException.class,
                () -> client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(ServerErrorException.class,
                () -> client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(UnexpectedStatusException.class,
                () -> client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
    }

    @Test
    public void testExecuteRequestPaged() {
        server = new MockWebServer();
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        server.enqueue(new MockResponse().setResponseCode(204));
        server.enqueue(new MockResponse().setResponseCode(400));
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setResponseCode(403));
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(503));

        try {
            assertNull(client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                    Void.class,
                    null,
                    false));
        } catch (SessionExpiredException | BadRequestException | ForbiddenException | NotFoundException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            fail();
        }
        assertThrows(BadRequestException.class,
                () -> client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(SessionExpiredException.class,
                () -> client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(ForbiddenException.class,
                () -> client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(NotFoundException.class,
                () -> client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(ServerErrorException.class,
                () -> client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(UnexpectedStatusException.class,
                () -> client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
    }

    @Test
    public void testGetNextPage() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        try {
            client.getAllEpubs("admin", "admin", new EpubQueryBuilder().page(0).pageSize(1).build());
        } catch (Exception ex) {
            fail();
        }

        String lastRequestGuid = client.getLastRequestGuid();
        assertNotNull(lastRequestGuid);

        PagedRequestDto<EpubDto> response = null;
        try {
            response = client.nextPage(lastRequestGuid, EpubDto.class);
        } catch (Exception ex) {
            fail();
        }

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertEquals(1, response.getContent().size());
        assertEquals(DtoRecord.epub2, response.getContent().get(0));
        assertEquals(1L, (long) response.getNumber());
    }
}
