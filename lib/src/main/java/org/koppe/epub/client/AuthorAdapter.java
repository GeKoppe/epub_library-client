package org.koppe.epub.client;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.dto.PagedRequestDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.koppe.epub.client.http.AuthorQueryBuilder;
import org.koppe.epub.client.http.HttpQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Request;
import okhttp3.RequestBody;
import tools.jackson.databind.ObjectMapper;

/**
 * Encapsulates all author related actions.
 */
class AuthorAdapter {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(AuthorAdapter.class);
    /**
     * Client to execute requests
     */
    private final EpubClient client;
    /**
     * Object mapper
     */
    private final ObjectMapper mapper = new ObjectMapper();
    /**
     * Executor for running parallel threads
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Default constructor
     * 
     * @param client Client the adapter is supposed to use to execute requests.
     */
    protected AuthorAdapter(EpubClient client) {
        this.client = client;
    }

    // #region add author
    /**
     * Adds a new author to the api. At leas author.firstName and author.surname
     * need to be filled, everything else is optional.
     * 
     * @param jwt    JWT to authenticate at the api with
     * @param author Author to add to the system.
     * @return The added author
     * @throws IllegalArgumentException If no jwt, author, author.firstName or
     *                                  author.surname is given.
     * @throws ApiCallException         General wrapper for all unexpected api error
     * @throws SessionExpiredException  If the jwt has expired
     * @throws BadRequestException      If the author dto was invalid.
     */
    protected @Nullable AuthorDto addAuthor(@NotNull String jwt, @NotNull AuthorDto author)
            throws IllegalArgumentException, ApiCallException, SessionExpiredException, BadRequestException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("Invalid jwt given");
            throw new IllegalArgumentException("Missing jwt");
        }

        if (author == null || author.getFirstName() == null || author.getFirstName().isBlank()
                || author.getSurname() == null || author.getSurname().isBlank()) {
            logger.info("Invalid author given");
            throw new IllegalArgumentException("Invalid author given, at least first name and surname must be filled");
        }

        logger.info("Building request to add a new author");
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/authors", client.url()))
                .post(RequestBody.create(mapper.writeValueAsString(author), EpubClient.APPLICATION_JSON));

        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);

        logger.info("Successfully initialized request, executing");
        AuthorDto dto = null;
        try {
            dto = client.executeRequest(builder.build(), AuthorDto.class, null, true);
        } catch (SessionExpiredException e) {
            logger.warn("JWT has expired");
            throw e;
        } catch (BadRequestException e) {
            logger.info("Invalid author given");
            throw e;
        } catch (ForbiddenException | NotFoundException | UnexpectedStatusException | ServerErrorException
                | IOException e) {
            logger.warn("Unexpected error occurred during the api call", e);
            throw new ApiCallException(null, e);
        }

        if (dto == null) {
            logger.info("Author could not be created");
            return null;
        }

        client.cacheValue(CacheType.AUTHORS, dto.getId(), dto);
        return dto;
    }

    // #region get author
    /**
     * Queries api for author with given id.
     * 
     * @param jwt      JWT to authenticate at the api
     * @param authorId Id of the author to query
     * @param query    Defines attributes the api should return, for example whether
     *                 books should be returned as well
     * @return Queried author or null, if no such author exists
     * @throws ApiCallException        General wrapper for all unexpected api errors
     * @throws SessionExpiredException If the jwt has expired
     */
    protected @Nullable AuthorDto getAuthorById(@NotNull String jwt, long authorId, @Nullable HttpQuery query)
            throws ApiCallException, SessionExpiredException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("Invalid jwt given");
            throw new IllegalArgumentException("Missing jwt");
        }

        Object cached = client.checkCache(CacheType.AUTHORS, (Long) authorId);
        if (cached != null && (cached instanceof AuthorDto)) {
            logger.info("Author with given id already cached");
            return (AuthorDto) cached;
        }

        logger.info("Retrieving epub for id {}", authorId);
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/epubs/%s%s", client.url(), "" + authorId,
                        (query != null ? query.toQueryString() : "")))
                .get();

        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);
        logger.debug("Executing request");

        AuthorDto dto = null;
        try {
            dto = client.executeRequest(builder.build(), AuthorDto.class, query, true);
        } catch (BadRequestException | ForbiddenException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            logger.info("Request failed with an exception", e);
            throw new ApiCallException(null, e);
        } catch (SessionExpiredException ex) {
            logger.info("Jwt expired");
            throw ex;
        } catch (NotFoundException ex) {
            logger.info("Author with given id not found");
            return null;
        }

        if (dto == null) {
            logger.info("No author found");
            return null;
        }

        client.cacheValue(CacheType.AUTHORS, dto.getId(), dto);
        return dto;
    }

    // #region delete author
    /**
     * Deletes author with given id. If deleteWithBooks is set to true, all epubs
     * assoicated with the given author are deleted as well. Use with caution!
     * 
     * @param jwt             JWT to authenticate at the api with
     * @param authorId        Id of the author to be deleted
     * @param deleteWithBooks If set to true, all epubs associated with the author
     *                        are deleted as well. USE WITH CAUTION.
     * @return The deleted author or null, if no author with given id exists
     * @throws IllegalArgumentException If no jwt is given
     * @throws SessionExpiredException  If the session has expired
     * @throws ApiCallException         If an unexpected error occurred during the
     *                                  api call.
     * @throws BadRequestException      If the server returned 400
     */
    protected @Nullable AuthorDto deleteAuthor(@NotNull String jwt, long authorId, boolean deleteWithBooks)
            throws IllegalArgumentException, SessionExpiredException, ApiCallException, BadRequestException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("Invalid jwt given");
            throw new IllegalArgumentException("Missing jwt");
        }

        logger.info("Building query to delete author with id {}", authorId);
        if (deleteWithBooks)
            logger.warn("Deleting associated epubs as well");

        HttpQuery query = new AuthorQueryBuilder().deleteEpubsAsWell(deleteWithBooks).build();
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/authors/%s%s", client.url(), "" + authorId, query.toQueryString()))
                .delete();

        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);

        AuthorDto dto = null;
        try {
            dto = client.executeRequest(builder.build(), AuthorDto.class, query, false);
        } catch (SessionExpiredException e) {
            logger.info("Session has expired", e);
            throw e;
        } catch (BadRequestException e) {
            logger.info("Author has not been deleted", e);
            throw e;
        } catch (ForbiddenException | ServerErrorException | UnexpectedStatusException | IOException e) {
            logger.info("Exception occurred while querying the api", e);
            throw new ApiCallException(null, e);
        } catch (NotFoundException e) {
            logger.info("Author with given id does not exist");
            return null;
        }

        if (dto == null) {
            logger.info("No dto received");
            return null;
        }
        logger.info("Successfully deleted author {}", dto);
        client.removeFromCache(CacheType.AUTHORS, dto.getId());

        return dto;
    }

    // #region get all authors
    /**
     * 
     * @param jwt
     * @param query
     * @return
     * @throws IllegalArgumentException
     * @throws ApiCallException
     * @throws SessionExpiredException
     */
    public @Nullable PagedRequestDto<AuthorDto> getAllAuthors(@NotNull String jwt, @Nullable HttpQuery query)
            throws IllegalArgumentException, ApiCallException, SessionExpiredException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("Missing jwt");
        }

        if (query == null) {
            logger.info("No query given, initialising default query");
            query = new AuthorQueryBuilder().page(0).pageSize(1000).build();
        }

        if (query.get("page") == null)
            query.overwrite("page", (Long) 0L);
        if (query.get("page_size") == null)
            query.overwrite("page_size", (Long) 1000L);

        Request.Builder builer = new Request.Builder()
                .url(String.format("%s/authors%s", client.url(), query.toQueryString()))
                .get();
        client.addHeaders(builer, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);
        logger.info("Querying api for all authors");

        PagedRequestDto<AuthorDto> authors = null;
        try {
            authors = client.executeRequestPaged(builer.build(), AuthorDto.class, query, true);
        } catch (BadRequestException | ForbiddenException | NotFoundException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            logger.info("Unexpected status returned by api", e);
            throw new ApiCallException(null, e);
        } catch (SessionExpiredException ex) {
            logger.info("Session has expired");
            throw ex;
        }

        if (authors == null || authors.getContent() == null) {
            logger.info("No authors found");
            return null;
        }

        final var finalAuthors = authors;
        logger.info("Caching all authors threaded");
        executor.submit(
                () -> finalAuthors.getContent().forEach(a -> client.cacheValue(CacheType.AUTHORS, a.getId(), a)));
        return authors;
    }

}
