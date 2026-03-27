package org.koppe.epub.client;

import java.io.File;
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
import org.koppe.epub.client.exceptions.IllegalFileTypeException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.koppe.epub.client.http.EpubQueryBuilder;
import org.koppe.epub.client.http.HttpQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
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

        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException();
        }
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

    // #region get epubs paged
    /**
     * Queries all epubs in the system. If no query is given, page is set to 0 and
     * pagesize is set to 1000.
     * 
     * @param jwt   JWT for querying the api
     * @param query Http query
     * @return All epubs matching the specifications
     * @throws ApiCallException        If an error occurred while querying the api
     * @throws SessionExpiredException If the session has expired
     */
    protected @Nullable PagedRequestDto<EpubDto> getEpubsPaged(@NotNull String jwt, @Nullable HttpQuery query)
            throws ApiCallException, SessionExpiredException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("Jwt missing");
        }
        logger.info("Querying all epubs");

        // Add default query
        if (query == null) {
            logger.info("No query given, adding default query (page 0, pagesize 1000)");
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
            logger.info("Executing request");
            result = client.executeRequestPaged(builder.build(), EpubDto.class, query, true);
            logger.info("Successfully called the api");
        } catch (NotFoundException | BadRequestException | ForbiddenException | UnexpectedStatusException | IOException
                | ServerErrorException ex) {
            logger.info("Exception occurred during api call", ex);
            throw new ApiCallException("", ex);
        } catch (SessionExpiredException e) {
            logger.info("Session has expired");
            throw e;
        }

        if (result == null) {
            logger.info("No result retrieved from api");
            return null;
        }

        final PagedRequestDto<EpubDto> finalResult = result;
        Executors.newFixedThreadPool(1).submit(() -> {
            logger.info("Caching results of paged request in different thread");
            if (finalResult.getContent() != null) {
                logger.info("Trying to cache result content");
                for (var x : finalResult.getContent()) {
                    client.cacheValue(CacheType.EPUBS, x.getId(), x);
                }
            }
        });

        return result;
    }

    // #region update epub
    /**
     * Updates epub with given id. Id in the dto is irrelevant, as the given epubId
     * is used.
     * 
     * @param jwt            JWT for authenticating at the api
     * @param dto            Updated epub
     * @param epubId         Id of the epub to be updated
     * @param overwriteNulls If set to true, values in dto set as null will null
     *                       existing values on epub in the database.
     * @return Updated epub or null, if epub could not be updated
     * @throws IllegalArgumentException If no jwt or dto is given
     * @throws SessionExpiredException  If the session has expired
     * @throws BadRequestException      If the request was malformed (i.e.
     *                                  overwriteNulls is true but dto.title is
     *                                  null, as every epub needs a title)
     * @throws NotFoundException        If no epub with given id was found
     * @throws ApiCallException         General wrapper for all other api exceptions
     */
    protected @Nullable EpubDto updateEpub(@NotNull String jwt, @NotNull EpubDto dto, long epubId,
            boolean overwriteNulls)
            throws IllegalArgumentException, SessionExpiredException, BadRequestException, NotFoundException,
            ApiCallException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("Missing jwt");
        }

        if (dto == null) {
            logger.info("No dto given");
            throw new IllegalArgumentException("Missing dto");
        }

        HttpQuery query = null;
        if (overwriteNulls) {
            logger.debug("Overwrite nulls is true, adding query");
            query = new EpubQueryBuilder().overwriteNulls(overwriteNulls).build();
        }

        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/epubs/%s%s", client.url(), "" + epubId,
                        (query == null ? "" : query.toQueryString())))
                .patch(RequestBody.create(mapper.writeValueAsString(dto), EpubClient.APPLICATION_JSON));
        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);

        EpubDto result = null;
        try {
            logger.debug("Executing request");
            result = client.executeRequest(builder.build(), EpubDto.class, query, true);
        } catch (ForbiddenException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            logger.info("Exception occurred in api call", e);
            throw new ApiCallException(null, e);
        } catch (SessionExpiredException ex) {
            logger.info("Session has expired", ex);
            throw ex;
        } catch (BadRequestException ex) {
            logger.info("Bad request", ex);
            throw ex;
        } catch (NotFoundException ex) {
            logger.info("Epub with given id does not exist", ex);
            throw ex;
        }

        if (result == null) {
            logger.info("No result returned from api");
            return null;
        }
        logger.info("Successfully fetched updated epub");
        return result;
    }

    // #region upload
    /**
     * Uploads epub file to given epub edition
     * 
     * @param jwt        JWT for authenticating at the api
     * @param uploadGuid Upload guid for the epub edition. Can be found in
     *                   {@link EpubEditionDto#getUploadGuid()}
     * @param epubFile   File to be uploaded
     * @throws IllegalArgumentException If no jwt, upload guid or file is given
     * @throws IllegalFileTypeException If file is not an epub
     * @throws ApiCallException         General wrapper for api exception
     * @throws SessionExpiredException  if the session has expired
     * @throws BadRequestException      If either an invalid file or invalid upload
     *                                  guid has been given
     */
    public void upload(@NotNull String jwt, @NotNull String uploadGuid, @NotNull File epubFile)
            throws IllegalArgumentException, IllegalFileTypeException, ApiCallException, SessionExpiredException,
            BadRequestException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("Missing jwt");
        }

        if (uploadGuid == null || uploadGuid.isBlank()) {
            logger.info("No upload guid given");
            throw new IllegalArgumentException("Missing upload guid");
        }

        if (epubFile == null || !epubFile.exists()) {
            logger.info("No epub file given");
            throw new IllegalArgumentException("Missing epub file");
        }

        if (!epubFile.getName().substring(epubFile.getName().lastIndexOf(".")).equals("epub")) {
            logger.info("Given file is not an epub file");
            throw new IllegalFileTypeException(epubFile.getName().substring(epubFile.getName().lastIndexOf(".")), null);
        }

        HttpQuery query = new EpubQueryBuilder().uploadGuid(uploadGuid).build();
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/epubs/upload%s", client.url(), query.toQueryString()))
                .post(new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file",
                        epubFile.getName(), RequestBody.create(epubFile, MediaType.parse("application/epub+zip")))
                        .build());

        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, "application/epub+zip");

        try {
            client.executeRequest(builder.build(), Void.class, query, false);
        } catch (ForbiddenException | NotFoundException
                | ServerErrorException | UnexpectedStatusException | IOException e) {
            logger.info("Exception while querying the api", e);
            throw new ApiCallException(e.getMessage(), e);
        } catch (SessionExpiredException e) {
            logger.info("Session has expired");
            throw e;
        } catch (BadRequestException e) {
            logger.info("Invalid uplaod guid or file given");
            throw e;
        }
    }
}
