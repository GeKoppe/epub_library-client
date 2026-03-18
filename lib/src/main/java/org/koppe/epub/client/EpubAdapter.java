package org.koppe.epub.client;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
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
            dto = client.executeRequest(request, EpubDto.class);
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
            dto = client.executeRequest(request, EpubDto.class);
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
            dto = client.executeRequest(request, EpubDto.class);
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
        return dto;
    }
}
