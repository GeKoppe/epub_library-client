package org.koppe.epub.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.dto.CredentialDto;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.dto.EpubEditionDto;
import org.koppe.epub.client.dto.PagedRequestDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.CacheMissException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.IllegalFileTypeException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.AuthorizationException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.koppe.epub.client.http.AuthorQueryBuilder;
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
        assertThrows(AuthorizationException.class, () -> client.login("admin", "test"));

        assertThrows(AuthorizationException.class, client::login);

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
        } catch (IllegalArgumentException | CacheMissException | ApiCallException | AuthorizationException e) {
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

    @Test
    public void testUpdateEpub() throws IllegalArgumentException, AuthorizationException, BadRequestException, NotFoundException, ApiCallException, CacheMissException {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        EpubDto dto = new EpubDto();
        assertThrows(NotFoundException.class, () -> client.updateEpub("admin", "admin", 5, dto, false));
        assertThrows(IllegalArgumentException.class, () -> client.updateEpub(1, null, false));
        assertThrows(BadRequestException.class, () -> client.updateEpub(1, dto, true));
        assertNotNull(client.updateEpub(1L, dto, false));
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
        } catch (AuthorizationException | BadRequestException | ForbiddenException | NotFoundException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            fail();
        }
        assertThrows(BadRequestException.class,
                () -> client.executeRequest(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(AuthorizationException.class,
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

    // #region test paged
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
        } catch (AuthorizationException | BadRequestException | ForbiddenException | NotFoundException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            fail();
        }
        assertThrows(BadRequestException.class,
                () -> client.executeRequestPaged(new Request.Builder().url(server.url("/").toString()).get().build(),
                        Void.class,
                        null, false));
        assertThrows(AuthorizationException.class,
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

    // #region test next page
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

    // #region get upload
    @Test
    public void testUpload() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        File epub = new File(getClass().getClassLoader().getResource("epubs/test.epub").getFile());
        File pdf = new File(getClass().getClassLoader().getResource("epubs/test.pdf").getFile());

        assertThrows(IllegalArgumentException.class, () -> client.uploadEpub("admin", "admin", null, null));
        assertThrows(IllegalArgumentException.class, () -> client.uploadEpub("admin", "admin", "", null));
        assertThrows(IllegalArgumentException.class, () -> client.uploadEpub("admin", "admin", "     ", null));
        assertThrows(IllegalArgumentException.class, () -> client.uploadEpub("admin", "admin", null, epub));
        assertThrows(IllegalFileTypeException.class, () -> client.uploadEpub("admin", "admin", "123", pdf));

        assertThrows(BadRequestException.class, () -> client.uploadEpub("admin", "admin", "234", epub));

        try {
            client.uploadEpub("admin", "admin", "123", epub);
        } catch (IllegalArgumentException | IllegalFileTypeException | ApiCallException | AuthorizationException
                | BadRequestException e) {
            fail();
        }
    }

    // #region download
    @Test
    public void testDownload() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        File downloadFolder = new File(System.getProperty("java.io.tmpdir"));
        File epub = null;
        File epub2 = null;
        File cover = null;
        File cover2 = null;

        try {
            epub = client.download("admin", "admin", "123", downloadFolder, false);
            epub2 = client.download("123", downloadFolder, false);
            cover = client.download("123", downloadFolder, true);
            cover2 = client.download("123", downloadFolder, true);
            assertNotNull(epub);
            assertNotNull(epub2);
            assertNotNull(cover);
            assertNotNull(cover2);
            assertTrue(epub2.getName().contains("(1)"));
            assertTrue(cover2.getName().contains("(1)"));
        } catch (Exception ex) {
            fail();
        } finally {
            if (epub.exists())
                epub.delete();
            if (epub2.exists())
                epub2.delete();
            if (cover.exists())
                cover.delete();
            if (cover2.exists())
                cover2.delete();
        }
    }

    // #region add author
    @Test
    public void testAddAuthor() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        assertThrows(IllegalArgumentException.class, () -> client.addAuthor(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> client.addAuthor("", null, null));
        assertThrows(IllegalArgumentException.class, () -> client.addAuthor(null, "", null));
        assertThrows(IllegalArgumentException.class, () -> client.addAuthor("admin", "admin", null));

        AuthorDto toAdd = new AuthorDto();
        assertThrows(IllegalArgumentException.class, () -> client.addAuthor("admin", "admin", toAdd));

        toAdd.setFirstName(" ");
        assertThrows(IllegalArgumentException.class, () -> client.addAuthor(toAdd));

        toAdd.setFirstName("Test");
        assertThrows(IllegalArgumentException.class, () -> client.addAuthor(toAdd));

        toAdd.setSurname(" ");
        assertThrows(IllegalArgumentException.class, () -> client.addAuthor(toAdd));

        toAdd.setSurname("Test");
        AuthorDto added = null;

        try {
            added = client.addAuthor(toAdd);
        } catch (IllegalArgumentException | ApiCallException | AuthorizationException | BadRequestException
                | CacheMissException e) {
            fail();
        }

        assertNotNull(added);
        assertEquals(toAdd.getFirstName(), added.getFirstName());
        assertEquals(toAdd.getSurname(), added.getSurname());
        assertNotNull(added.getId());
    }

    // #region get author
    @Test
    public void testGetAuthor() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        try {
            AuthorDto a3 = client.getAuthor("admin", "admin", 3L, null);
            assertNull(a3);

            AuthorDto a1 = client.getAuthor(1L, new AuthorQueryBuilder().withEpubs(true).withGenres(true).build());
            AuthorDto a2 = client.getAuthor(2L, null);

            assertEquals((Long) 1L, a1.getId());
            assertEquals("Test", a1.getFirstName());
            assertTrue(a1.getEpubs().size() > 0);
            assertNotNull(a2);
            assertEquals("Test2", a2.getFirstName());
            assertTrue(a2.getEpubs().size() == 0);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testDeleteAuthor() {
        server.setDispatcher(new MockDispatcher());
        EpubClient client = EpubClientFactory.newCredentialCacheClient(server.url("/").toString());

        try {
            AuthorDto dto = null;
            dto = client.deleteAuthor("admin", "admin", 3, false);
            assertNull(dto);

            dto = client.deleteAuthor(1, false);
            assertNotNull(dto);
            assertEquals("Test", dto.getFirstName());

            dto = client.deleteAuthor(2, true);
            assertNotNull(dto);
            assertEquals("Test2", dto.getFirstName());
            assertEquals(2, dto.getEpubs().size());
        } catch (IllegalArgumentException | ApiCallException | AuthorizationException | BadRequestException
                | CacheMissException e) {
            fail(e.getMessage());
        }
    }
}
