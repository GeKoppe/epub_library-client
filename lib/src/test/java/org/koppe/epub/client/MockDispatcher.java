package org.koppe.epub.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.dto.EpubEditionDto;
import org.koppe.epub.client.dto.PagedRequestDto;
import org.koppe.epub.client.dto.UserDto;

import okhttp3.MediaType;
import okhttp3.MultipartReader;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import tools.jackson.databind.ObjectMapper;

public class MockDispatcher extends Dispatcher {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        String url = request.getPath();
        if (url.indexOf("?") != -1)
            url = url.substring(0, url.indexOf("?"));

        if (url.equals("/auth/login"))
            return login(request);

        if (request.getHeader("Authorization") == null
                || !request.getHeader("Authorization").equals("Bearer " + DtoRecord.jwt.getJwt())) {
            return new MockResponse().setResponseCode(401);
        }

        try {
            switch (url) {
                case "/epubs":
                    switch (request.getMethod()) {
                        case "POST":
                            return addEpub(request);
                        case "GET":
                            return getAllEpubs(request);
                        default:
                            return new MockResponse().setResponseCode(403);
                    }
                case "/epubs/1", "/epubs/2":
                    switch (request.getMethod()) {
                        case "GET":
                            return getEpub(request);
                        case "DELETE":
                            return deleteEpub(request);
                        case "PATCH":
                            return updateEpub(request);
                        default:
                            return new MockResponse().setResponseCode(403);
                    }
                case "/epubs/1/editions", "/epubs/2/editions":
                    switch (request.getMethod()) {
                        case "POST":
                            return addEpubEdition(request);
                        default:
                            return new MockResponse().setResponseCode(403);
                    }
                case "/epubs/upload":
                    switch (request.getMethod()) {
                        case "POST":
                            return upload(request);
                        default:
                            return new MockResponse().setResponseCode(403);
                    }
                case "/epubs/download":
                    switch (request.getMethod()) {
                        case "GET":
                            return download(request);
                        default:
                            return new MockResponse().setResponseCode(403);
                    }
                case "/authors":
                    switch (request.getMethod()) {
                        case "POST":
                            return addAuthor(request);
                        default:
                            return new MockResponse().setResponseCode(403);
                    }
                case "/authors/1", "/authors/2":
                    switch (request.getMethod()) {
                        case "GET":
                            return getAuthor(request);
                        case "DELETE":
                            return deleteAuthor(request);
                        default:
                            return new MockResponse().setResponseCode(403);
                    }
                default:
                    return new MockResponse().setResponseCode(404);
            }
        } catch (Exception ex) {
            return new MockResponse().setResponseCode(500);
        }
    }

    // #region login
    private MockResponse login(RecordedRequest r) {
        UserDto dto = getBody(r.getBody(), UserDto.class);
        if (dto.getName() == null || dto.getName().isBlank() || dto.getPassword() == null
                || dto.getPassword().isBlank()) {
            return new MockResponse().setResponseCode(401)
                    .setBody("{\"message\":\"unauthorized\"}");
        }

        if (!dto.getName().equals("admin") || !dto.getPassword().equals("admin")) {
            return new MockResponse().setResponseCode(401)
                    .setBody("{\"message\":\"unauthorized\"}");
        }

        return new MockResponse().setResponseCode(200)
                .setBody(mapper.writeValueAsString(DtoRecord.jwt));
    }

    // #region add epub
    private MockResponse addEpub(RecordedRequest r) {
        EpubDto dto = getBody(r.getBody(), EpubDto.class);

        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            return new MockResponse().setResponseCode(400);
        }

        dto.setId((long) Math.floor(Math.random() * 100.0) + 1);
        dto.setUploadDate(LocalDate.now());
        return new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(dto));
    }

    // #region get epub
    private MockResponse getEpub(RecordedRequest r) {
        EpubDto epub = r.getPath().contains("1") ? DtoRecord.epub1 : DtoRecord.epub2;

        if (r.getRequestUrl().queryParameter("with_authors") == null
                || r.getRequestUrl().queryParameter("with_authors").equals("false"))
            epub.setAuthors(new ArrayList<>());

        if (r.getRequestUrl().queryParameter("with_genres") == null
                || r.getRequestUrl().queryParameter("with_genres").equals("false"))
            epub.setGenres(new ArrayList<>());

        if (r.getRequestUrl().queryParameter("with_tags") == null
                || r.getRequestUrl().queryParameter("with_tags").equals("false"))
            epub.setTags(new ArrayList<>());

        return new MockResponse().setResponseCode(200)
                .setBody(mapper.writeValueAsString(epub));
    }

    // #region delete epub
    private MockResponse deleteEpub(RecordedRequest r) {
        return new MockResponse().setResponseCode(200)
                .setBody(mapper.writeValueAsString(r.getPath().contains("1") ? DtoRecord.epub1 : DtoRecord.epub2));
    }

    // #region add edition
    private MockResponse addEpubEdition(RecordedRequest r) {
        EpubEditionDto dto = getBody(r.getBody().getBuffer(), EpubEditionDto.class);
        if (dto == null || dto.getVersionName() == null || dto.getVersionName().isBlank()) {
            return new MockResponse().setResponseCode(400);
        }

        dto.setId((long) Math.floor(Math.random() * 100.0) + 1);
        dto.setDownloadGuid(UUID.randomUUID().toString());
        dto.setUploadGuid(UUID.randomUUID().toString());

        return new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(dto));
    }

    // #region get all epubs
    private MockResponse getAllEpubs(RecordedRequest r) {
        String pageStr = r.getRequestUrl().queryParameter("page");
        String pageSizeStr = r.getRequestUrl().queryParameter("page_size");
        int page = 0;
        int pageSize = 2;

        if (pageStr != null && !pageStr.isBlank()) {
            page = Integer.valueOf(pageStr);
        }

        if (pageSizeStr != null && !pageSizeStr.isBlank()) {
            pageSize = Integer.valueOf(pageSizeStr);
        }

        PagedRequestDto<EpubDto> response = new PagedRequestDto<>();
        response.setNumber(Long.valueOf(page));

        if (page == 0) {
            if (pageSize >= 2) {
                response.setContent(List.of(DtoRecord.epub1, DtoRecord.epub2));
            } else if (pageSize == 1) {
                response.setContent(List.of(DtoRecord.epub1));
            } else {
                response.setContent(List.of());
            }
        } else if (page == 1) {
            if (pageSize >= 1)
                response.setContent(List.of(DtoRecord.epub2));
            else
                response.setContent(List.of());
        } else {
            response.setContent(List.of());
        }

        response.setItemCount(Long.valueOf(response.getContent().size()));
        response.setPageSize(Long.valueOf(pageSize));

        return new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(response));
    }

    // #region uplaod
    private MockResponse upload(RecordedRequest r) {
        if (!r.getRequestUrl().queryParameter("upload-guid").equals("123")) {
            return new MockResponse().setResponseCode(400);
        }
        String contentType = r.getHeader("Content-Type");
        assert contentType != null;

        String boundary = MediaType.parse(contentType).parameter("boundary");
        assert boundary != null;

        byte[] bytes = new byte[] {};
        try (MultipartReader reader = new MultipartReader(r.getBody(), boundary)) {
            MultipartReader.Part part;
            while ((part = reader.nextPart()) != null) {
                String disposition = part.headers().get("Content-Disposition");
                if (disposition != null && disposition.contains("filename=")) {
                    bytes = part.body().readByteArray();
                }
            }
        } catch (IOException e) {
            return new MockResponse().setResponseCode(400);
        }

        File tmpFile = new File(System.getProperty("java.io.tmpdir") + "/test.epub");
        if (tmpFile.exists())
            tmpFile.delete();
        try {
            tmpFile.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(tmpFile.getAbsolutePath())) {
                fos.write(bytes);
            }
        } catch (IOException e) {
            return new MockResponse().setResponseCode(500);
        }

        if (tmpFile.length() > 0) {
            tmpFile.delete();
            return new MockResponse().setResponseCode(200);
        }

        return new MockResponse().setResponseCode(400);
    }

    // #region download
    private MockResponse download(RecordedRequest r) {
        if (!r.getRequestUrl().queryParameter("download-guid").equals("123")) {
            return new MockResponse().setResponseCode(400);
        }

        try {
            if (r.getRequestUrl().queryParameter("cover") != null
                    && r.getRequestUrl().queryParameter("cover").equals("true")) {
                byte[] fileContent = getClass().getClassLoader().getResourceAsStream("epubs/test-cover.png")
                        .readAllBytes();
                return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/octet-stream")
                        .setHeader("Content-Disposition", "attachment; filename=\"test-cover.png\"")
                        .setBody(new String(fileContent));
            } else {
                byte[] fileContent = getClass().getClassLoader().getResourceAsStream("epubs/test.epub").readAllBytes();
                return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/octet-stream")
                        .setHeader("Content-Disposition", "attachment; filename=\"test.epub\"")
                        .setBody(new String(fileContent));
            }
        } catch (Exception ex) {
            return new MockResponse().setResponseCode(500);
        }
    }

    // #region add author
    private MockResponse addAuthor(RecordedRequest r) {
        AuthorDto dto = getBody(r.getBody(), AuthorDto.class);
        if (dto == null) {
            return new MockResponse().setResponseCode(400);
        }

        if (dto.getFirstName() == null || dto.getFirstName().isBlank() || dto.getSurname() == null
                || dto.getSurname().isBlank()) {
            return new MockResponse().setResponseCode(400);
        }

        dto.setId((long) Math.floor(Math.random() * 100.0) + 1);
        return new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(dto));
    }

    // #region get author
    private MockResponse getAuthor(RecordedRequest r) {
        AuthorDto dto = r.getPath().contains("1") ? DtoRecord.author1 : DtoRecord.author2;

        if (r.getRequestUrl().queryParameter("with_epubs") == null
                || r.getRequestUrl().queryParameter("with_epubs").equals("false"))
            dto.setEpubs(new ArrayList<>());
        else if (r.getRequestUrl().queryParameter("with_epubs").equals("true")) {
            dto.setEpubs(List.of(DtoRecord.epub1, DtoRecord.epub2));
        }

        if (r.getRequestUrl().queryParameter("with_genres") == null
                || r.getRequestUrl().queryParameter("with_genres").equals("false"))
            dto.setGenres(new ArrayList<>());
        else if (r.getRequestUrl().queryParameter("with_genres").equals("true")) {
            dto.setGenres(List.of(DtoRecord.genre1, DtoRecord.genre2));
        }

        if (r.getRequestUrl().queryParameter("with_tags") == null
                || r.getRequestUrl().queryParameter("with_tags").equals("false"))
            dto.setGenres(new ArrayList<>());
        else if (r.getRequestUrl().queryParameter("with_tags").equals("true")) {
            dto.setGenres(new ArrayList<>());
        }

        return new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(dto));
    }

    // #region delete author
    private MockResponse deleteAuthor(RecordedRequest r) {
        AuthorDto dto = null;
        if (r.getRequestUrl().toString().contains("/1"))
            dto = DtoRecord.author1;
        else if (r.getRequestUrl().toString().contains("/2"))
            dto = DtoRecord.author2;
        else
            return new MockResponse().setResponseCode(404);

        if (r.getRequestUrl().queryParameter("with_epubs") != null
                && r.getRequestUrl().queryParameter("with_epubs").equals("true")) {
            dto.setEpubs(List.of(DtoRecord.epub1, DtoRecord.epub2));
        } else {
            dto.setEpubs(new ArrayList<>());
        }
        return new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(dto));
    }

    private MockResponse updateEpub(RecordedRequest r) {
        EpubDto dto = getBody(r.getBody(), EpubDto.class);
        boolean overwriteNulls = r.getRequestUrl().queryParameter("overwrite_nulls") != null
                && r.getRequestUrl().queryParameter("overwrite_nulls").equals("true");

        EpubDto original = r.getRequestUrl().toString().contains("/1") ? DtoRecord.epub1 : DtoRecord.epub2;
        if (dto == null) {
            return new MockResponse().setResponseCode(400);
        }

        if ((dto.getTitle() == null || dto.getTitle().isBlank()) && overwriteNulls) {
            return new MockResponse().setResponseCode(400);
        }

        dto.setId(original.getId());
        dto.setAuthors(original.getAuthors());
        dto.setEditions(original.getEditions());
        dto.setGenres(original.getGenres());
        dto.setSeries(original.getSeries());
        dto.setUploadDate(original.getPublishDate());
        dto.setTags(original.getTags());

        return new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(original));
    }

    // #region get body
    private <T> T getBody(Buffer buffer, Class<T> expected) {
        try {
            String bodyString = buffer.readString(StandardCharsets.UTF_8);
            return mapper.readValue(bodyString, expected);
        } catch (Exception ex) {
            return null;
        }
    }
}
