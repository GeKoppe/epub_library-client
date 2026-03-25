package org.koppe.epub.client;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.dto.EpubEditionDto;
import org.koppe.epub.client.dto.PagedRequestDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.koppe.epub.client.http.EpubQueryBuilder;
import org.koppe.epub.client.http.HttpQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import okhttp3.RequestBody;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter for working with epubs in the api
 */
@RequiredArgsConstructor
class EpubAdapter {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(EpubAdapter.class);
    /**
     * Client for executing calls and providing important data
     */
    private final EpubClient client;
    /**
     * Object mapper for transforming objects to json strings
     */
    private final ObjectMapper mapper = new ObjectMapper();

    // #region add epub
    /**
     * Execution of adding an epub. Needs a valid jwt. EpubDto needs at least title
     * for this method to work properly.
     * 
     * @param jwt  JWT for logging into the application
     * @param epub Epub to add to the database
     * @return Inserted epub or null, if an error occurred
     * @throws IllegalArgumentException If an invalid epub dto has or no jwt has
     *                                  been given.
     * @throws ApiCallException         If api call itself failed due to an
     *                                  exception.
     * @throws SessionExpiredException  If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    protected @Nullable EpubDto addEpub(@NotNull String jwt, @NotNull EpubDto epub)
            throws IllegalArgumentException, ApiCallException, SessionExpiredException {
        if (epub == null || epub.getTitle() == null || epub.getTitle().isBlank()) {
            logger.info("No valid epub dto given");
            throw new IllegalArgumentException("Invalid epub dto, title is missing");
        }

        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("Invalid jwt");
        }

        logger.info("Preparing to transfer epub {} to the api", epub);
        Request.Builder builder = new Request.Builder()
                .url(client.url() + "/epubs")
                .post(RequestBody.create(mapper.writeValueAsString(epub), EpubClient.APPLICATION_JSON));
        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);

        Request request = builder.build();
        logger.info("Successfully build request");

        EpubDto dto = null;
        logger.info("Executing request");
        try {
            dto = client.executeRequest(request, EpubDto.class, null, true);
        } catch (NotFoundException | ForbiddenException | ServerErrorException | UnexpectedStatusException
                | BadRequestException
                | IOException ex) {
            logger.info("Exception occurred in method call");
            throw new ApiCallException(null, ex);
        } catch (SessionExpiredException ex) {
            logger.info("Session has expired");
            throw ex;
        }

        if (dto == null) {
            logger.info("No epub could be added");
            return null;
        }

        client.cacheValue(CacheType.EPUBS, dto.getId(), dto);
        return dto;
    }

    // #region delete epub
    /**
     * Deletes epub from the server.
     * 
     * @param jwt    Token to log into the application with
     * @param epubId Id of the epub
     * @return The deleted epub
     * @throws IllegalArgumentException If username or password are missing
     * @throws ApiCallException         If an error occurred during call to the api
     * @throws SessionExpiredException  If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    protected @Nullable EpubDto deleteEpub(@NotNull String jwt, long epubId)
            throws IllegalArgumentException, ApiCallException, SessionExpiredException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("Invalid jwt given");
            throw new IllegalArgumentException("Jwt is missing");
        }

        Request.Builder builer = new Request.Builder()
                .url(String.format("%s/epubs/%s", client.url(), epubId))
                .delete();
        client.addHeaders(builer, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);
        Request request = builer.build();
        EpubDto dto = null;

        try {
            dto = client.executeRequest(request, EpubDto.class, null, true);
        } catch (ForbiddenException | ServerErrorException | UnexpectedStatusException | BadRequestException
                | IOException ex) {
            logger.info("Exception occurred in method call");
            throw new ApiCallException(null, ex);
        } catch (NotFoundException ex) {
            logger.info("Could not find requested resource");
            return null;
        } catch (SessionExpiredException ex) {
            logger.info("Session expired or login failed");
            throw ex;
        }

        if (dto == null) {
            logger.info("Could not delete dto for some reason");
            return null;
        }

        logger.info("Successfully deleted epub", dto);
        client.removeFromCache(CacheType.EPUBS, (Long) epubId);
        return dto;
    }

    // #region get epub
    /**
     * Retrieves epub with given id from the api.
     * 
     * @param jwt    The session token
     * @param epubId Id of the epub to retrieve
     * @return Retrieved epub or null, if no epub with given id was found
     * @throws IllegalArgumentException If jwt is missing or blank
     * @throws SessionExpiredException  If the session credentials are invalid
     * @throws ApiCallException         General api error exception
     */
    protected @Nullable EpubDto getEpub(@NotNull String jwt, long epubId, HttpQuery query)
            throws IllegalArgumentException, SessionExpiredException, ApiCallException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("No jwt given");
        }
        logger.info("Retrieving epub with id {}", epubId);

        Object cached = client.checkCache(CacheType.EPUBS, (Long) epubId);
        if (cached != null && (cached instanceof EpubDto)) {
            logger.info("Found epub in cache");
            return (EpubDto) cached;
        }

        logger.info("Epub with id {} not in cache, calling api", epubId);

        Request.Builder builder = new Request.Builder()
                .url(client.url() + "/epubs/" + epubId + (query != null ? query.toQueryString() : ""))
                .get();
        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);
        Request request = builder.build();
        logger.debug("Built request, executing");

        EpubDto dto = null;
        try {
            dto = client.executeRequest(request, EpubDto.class, query, true);
        } catch (ForbiddenException | ServerErrorException | UnexpectedStatusException | BadRequestException
                | IOException ex) {
            logger.info("Exception occurred in method call");
            throw new ApiCallException(null, ex);
        } catch (NotFoundException ex) {
            logger.info("Could not find requested resource");
            return null;
        } catch (SessionExpiredException ex) {
            logger.info("Session expired or login missing");
            throw ex;
        }

        if (dto == null) {
            logger.info("Could not retrieve epub");
            return null;
        }
        client.cacheValue(CacheType.EPUBS, (Long) epubId, dto);
        return dto;
    }

    // #region add epub edition
    /**
     * Adds the given edition to epub with given id.
     * 
     * @param jwt     Session token
     * @param epubId  Id of the epub
     * @param edition Edition to add to the epub
     * @return Added edition or null, if edition could not be added
     * @throws IllegalArgumentException If no username, password, edition or
     *                                  edition.versionName are given
     * @throws NotFoundException        If no epub with given id exists
     * @throws SessionExpiredException  If the session has expired
     * @throws BadRequestException      If the given edition is invalid
     * @throws ApiCallException         General api error
     */
    protected @Nullable EpubEditionDto addEpubEdition(@NotNull String jwt, long epubId, @NotNull EpubEditionDto edition)
            throws IllegalArgumentException, NotFoundException, SessionExpiredException, BadRequestException,
            ApiCallException {
        if (edition == null || edition.getVersionName() == null || edition.getVersionName().isBlank()) {
            logger.info("Invalid edition dto given");
            throw new IllegalArgumentException();
        }
        logger.info("Uploading new epub edition");
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/epubs/%s/editions", client.url(), "" + epubId))
                .post(RequestBody.create(mapper.writeValueAsString(edition), EpubClient.APPLICATION_JSON));
        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);

        EpubEditionDto dto = null;
        try {
            dto = client.executeRequest(builder.build(), EpubEditionDto.class, null, true);
        } catch (ForbiddenException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            throw new ApiCallException("", e);
        } catch (NotFoundException ex) {
            logger.info("Invalid epub id given");
            throw ex;
        } catch (SessionExpiredException ex) {
            throw ex;
        } catch (BadRequestException ex) {
            throw ex;
        }

        if (dto == null) {
            logger.info("Could not upload edition");
            return null;
        }

        return dto;
    }

    /**
     * Queries all epubs in the system. If no query is given, page is set to 0 and
     * pagesize is set to 1000.
     * 
     * @param jwt   JWT for querying the api
     * @param query Http query
     * @return All epubs matching the specifications
     * @throws ApiCallException If an error occurred while querying the api
     */
    protected @Nullable PagedRequestDto<EpubDto> getEpubsPaged(@NotNull String jwt, @Nullable HttpQuery query)
            throws ApiCallException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("Jwt missing");
        }
        logger.info("Querying all epubs");

        if (query == null) {
            query = new EpubQueryBuilder().page(0).pageSize(1000).build();
        }
        if (query.get("page") == null) {
            query.overwrite("page", (Integer) 0);
        }
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/epubs%s", client.url(), query.toQueryString())).get();
        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);

        PagedRequestDto<EpubDto> result = null;
        try {
            result = client.executeRequestPaged(builder.build(), EpubDto.class, query, true);
            logger.info("Successfully called the api");
        } catch (Exception ex) {
            logger.info("Exception occurred during api call", ex);
            throw new ApiCallException("", ex);
        }

        if (result == null) {
            logger.info("No result retrieved from api");
            return null;
        }

        final PagedRequestDto<EpubDto> finalrResult = result;
        Executors.newFixedThreadPool(1).submit(() -> {
            logger.info("Caching results of paged request in different thread");
            if (finalrResult.getContent() != null) {
                logger.info("Trying to cache result content");
                for (var x : finalrResult.getContent()) {
                    client.cacheValue(CacheType.EPUBS, x.getId(), x);
                }
            }
        });

        return result;
    }
}
