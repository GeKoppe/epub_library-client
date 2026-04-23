package org.koppe.epub.client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.koppe.epub.client.cache.Cache;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.cache.RequestCache;
import org.koppe.epub.client.cache.RequestCacheEntity;
import org.koppe.epub.client.cache.CredentialCache.CredentialCacheKeys;
import org.koppe.epub.client.configuration.ApiEndpoints;
import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.dto.CredentialDto;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.dto.EpubEditionDto;
import org.koppe.epub.client.dto.PagedRequestDto;
import org.koppe.epub.client.dto.TagDto;
import org.koppe.epub.client.dto.UserDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.CacheMissException;
import org.koppe.epub.client.exceptions.CachingException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.IllegalFileTypeException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.AuthorizationException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.koppe.epub.client.http.HttpQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tools.jackson.databind.ObjectMapper;

/**
 * Main component for calling the epub library api.
 * Provides all types of functionality for interacting with the server side
 * components of the epub library.
 * <br/>
 * This client supports caching various entities, most importantly credentials.
 * <br/>
 * 
 * Example usage of a default client:
 * 
 * <pre>
 * {@code
 * // Initialise a client via factory methods
 * EpubClient client = EpubClientFactory.newDefaultClient("http://localhost:9093");
 * EpubDto epub = new EpubDto();
 * epub.setTitle("Test");
 * EpubDto uploaded = client.addEpub("user", "password", epub);
 * }
 * </pre>
 * 
 * <br/>
 * 
 * If you want to instead use a client that caches your login, you could do
 * something like this:
 * 
 * <pre>
 * {@code
 * EpubClient client = EpubClientFactory.newCachingClient("http://localhost:9093",
 *         new CacheType[] { CacheType.CREDENTIALS });
 * 
 * // This method caches user and password and creates a session
 * client.login("user", "password");
 * 
 * EpubDto epub = new EpubDto();
 * epub.setTitle("Test");
 * 
 * // Now you can use the same methods but without username and password
 * EpubDto uploaded = client.addEpub(epub);
 * }
 * </pre>
 * 
 * This method is preferred, as logging into the api for every call creates a
 * lot of unnecessary jwts.
 * <br/>
 * You can also add caches for other entites. They will automatically be filled,
 * if they exist for the client. You can either use the factory method
 * {@link EpubClientFactory#newCachingClient(String, CacheType[])} and define
 * all cache types, those will have default configuration though. Another option
 * is the following:
 * 
 * <pre>
 * {@code
 * EpubClient client = new EpubClient("http://localhost:9093");
 * EpubCache cache = new EpubCache(client);
 * client.registerCache(CacheType.EPUBS, cache);
 * }
 * </pre>
 * 
 * <br/>
 * 
 * Or you could even define custom caches:
 * 
 * <pre>
 * {@code
 * EpubClient client = new EpubClient("http://localhost:9093");
 * Cache<Long, EpubDto> cache = new Cache<Long, EpubDto>() {
 *     // Override methods of the interface with your logic
 * };
 * client.registerCache(CacheType.EPUBS, cache);
 * }
 * </pre>
 */
@RequiredArgsConstructor
public class EpubClient {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(EpubClient.class);
    /**
     * Base url of the epub lib server api
     */
    private final String baseUrl;
    /**
     * Object mapper for mapping json to objects
     */
    private final ObjectMapper mapper = new ObjectMapper();
    /**
     * All caches the client can use
     */
    private final Map<CacheType, Cache<?, ?>> caches = new HashMap<>();
    /**
     * HTTP client for calling the epub lib api
     */
    private final OkHttpClient client;
    /**
     * Application json mediatype
     */
    protected static final MediaType APPLICATION_JSON = MediaType.get("application/json;charset=utf-8");
    /**
     * Mediatype string for application json
     */
    protected static final String APPLICATION_JSON_STRING = "application/json;charset=utf-8";
    /**
     * Adapter for epub operations
     */
    private EpubAdapter epubs;
    /**
     * Adapter for author operation
     */
    private AuthorAdapter authors;
    /**
     * Adapter for tags
     */
    private TagAdapter tags;

    // #region constructors
    /**
     * Default constructor. Base URL is required, everything else is initialized
     * with default values.
     * 
     * @param baseUrl Base URL of the epub lib api.
     */
    public EpubClient(String baseUrl) {
        if (baseUrl.charAt(baseUrl.length() - 1) == '/') {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .callTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .hostnameVerifier((hostname, session) -> {
                    return session.getPeerHost().equals(hostname);
                })
                .build();

        RequestCache rc = new RequestCache();
        rc.setMaxElements(100);
        rc.setRetention(100L, TimeUnit.MINUTES);
        caches.put(CacheType.REQUEST, rc);
    }

    /**
     * Initialises new Client with given base url. If trust all certs is true, no
     * ssl certificate check will be done. Otherwise a normal hostname verification
     * is performed.
     * 
     * @param baseUrl       Base url of the epub lib api
     * @param trustAllCerts True, if all certificates should be accepted and
     *                      hostname verification therefore skipped.
     */
    public EpubClient(String baseUrl, boolean trustAllCerts) {
        if (baseUrl.charAt(baseUrl.length() - 1) == '/') {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .hostnameVerifier(trustAllCerts ? (hostname, session) -> {
                    return true;
                } : (hostname, session) -> {
                    return session.getPeerHost().equals(hostname);
                })
                .build();
    }

    // #region check cache
    /**
     * Retrieves a value from the cache with given type
     * 
     * @param type Type of the cache to retrieve the value from
     * @param key  Key to retrieve from the cache
     * @return Retrieved value
     */
    public @Nullable Object checkCache(CacheType type, Object key) {
        if (key == null || !caches.containsKey(type)) {
            logger.info("No cache for type {} exists or key is null", type);
            return null;
        }

        return retrieveFromCache(caches.get(type), key);
    }

    // #region retrieve from cache
    /**
     * Retrieves value for the given key from cache. Cache will always reload the
     * value if possible and necessary.
     * 
     * @param <K>   Type of the keys of the cache
     * @param <V>   Type of the values of the cache
     * @param cache Cache to get the value from
     * @param key   Key to get the value for
     * @return Retrieved value
     */
    @SuppressWarnings("unchecked")
    private @Nullable <K, V> V retrieveFromCache(Cache<K, V> cache, Object key) {
        return cache.getValue((K) key);
    }

    // #region cache value
    /**
     * Caches the value in the correct cache if it exists
     * 
     * @param type  Type of the cache to put the value into.
     * @param key   Key to put the value into the cache for.
     * @param value Value to be inserted into the cache
     */
    public void cacheValue(CacheType type, Object key, Object value) {
        if (!caches.containsKey(type)) {
            logger.info("No cache for type {} exists", type);
            return;
        }
        if (key == null || value == null) {
            logger.info("No key or value given");
            return;
        }

        try {
            logger.info("Inserting new value into {} cache", type);
            insertIntoCache(caches.get(type), key, value);
        } catch (CachingException ex) {
            logger.warn("Could not cache value due to exception {}", ex);
        }
    }

    // #region insert into cache
    /**
     * Inserts value into given cache.
     * 
     * @param <K>   Type of the key for cache
     * @param <V>   Type of the value for cache
     * @param cache Cache to insert the value into
     * @param key   Key of the new cache value
     * @param value Value to be cached
     * @throws CachingException If caching yielded an exception
     */
    @SuppressWarnings("unchecked")
    private <K, V> void insertIntoCache(Cache<K, V> cache, Object key, Object value) throws CachingException {
        cache.setValue((K) key, (V) value);
    }

    /**
     * Removes value associated with given key from the cache of given type, if such
     * a cache and key exist.
     * 
     * @param type Type of the cache to remove the value from
     * @param key  Key of the value to be removed.
     * @return The removed value or null, if no such cache or key in that cache
     *         exists
     */
    public @Nullable Object removeFromCache(CacheType type, Object key) {
        if (!caches.containsKey(type)) {
            return null;
        }

        Object value = null;
        try {
            value = removeFromCache(caches.get(key), key);
        } catch (CachingException ex) {
            logger.info("Exception occurred when removing an item from a cache", ex);
            return null;
        }

        return value;
    }

    /**
     * Removes value associated with given key from the given cache.
     * 
     * @param <K>   Type of the keys in the cache
     * @param <V>   Type of the values in the cache
     * @param cache Cache to remove the value from
     * @param key   Cache key of the value to be removed from the cache
     * @return The removed value or null, if no value is associated with given key.
     * @throws CachingException If something happens during removal of the value.
     */
    @SuppressWarnings("unchecked")
    private <K, V> V removeFromCache(Cache<K, V> cache, Object key) throws CachingException {
        return cache.removeFromCache((K) key);
    }

    // #region login
    /**
     * Creates a new session in the system for given username and password
     * 
     * @param username Name of the user to create a session for
     * @param password Password of the user
     * @return Session credentials
     * @throws ApiCallException
     * @throws AuthorizationException
     * @throws IllegalArgumentException Thrown if username or password are not
     *                                  given.
     */
    public @Nullable CredentialDto login(String username, String password)
            throws ApiCallException, AuthorizationException, IllegalArgumentException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            logger.warn("Missing user name or password, cannot login");
            throw new IllegalArgumentException("Username or password missing");
        }
        logger.info("Creating new session for user {}", username);

        UserDto user = new UserDto();
        user.setName(username);
        user.setPassword(password);
        cacheValue(CacheType.CREDENTIALS, CredentialCacheKeys.USER.getValue(), username);
        cacheValue(CacheType.CREDENTIALS, CredentialCacheKeys.PASSWORD.getValue(), password);

        Request request = new Request.Builder()
                .url(this.baseUrl + ApiEndpoints.login.getUrl())
                .post(RequestBody.create(mapper.writeValueAsString(user),
                        MediaType.get("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .build();

        CredentialDto dto = null;
        try {
            dto = executeRequest(request, CredentialDto.class, null, false);
        } catch (ForbiddenException | ServerErrorException | UnexpectedStatusException | BadRequestException
                | IOException ex) {
            logger.info("Exception occurred in method call");
            throw new ApiCallException(null, ex);
        } catch (NotFoundException ex) {
            logger.info("Could not find requested resource");
            return null;
        } catch (AuthorizationException ex) {
            logger.info("Could not log into the api");
            throw ex;
        }
        if (dto == null || dto.getJwt() == null) {
            logger.info("No values in the credential dto");
            return null;
        }

        cacheValue(CacheType.CREDENTIALS, CredentialCacheKeys.JWT.getValue(), dto.getJwt());
        cacheValue(CacheType.CREDENTIALS, CredentialCacheKeys.REFRESH.getValue(), dto.getRefresh());

        return dto;
    }

    /**
     * Performs login with cached username and password. If either are missing, an
     * exception will be thrown.
     * 
     * @return The login credentials
     * @throws CacheMissException     If user or password are not cached
     * @throws AuthorizationException
     * @throws ApiCallException
     */
    public @Nullable CredentialDto login() throws CacheMissException, ApiCallException, AuthorizationException {
        Object user = checkCache(CacheType.CREDENTIALS, CredentialCacheKeys.USER.getValue());
        Object password = checkCache(CacheType.CREDENTIALS, CredentialCacheKeys.PASSWORD.getValue());

        if (user == null || !(user instanceof String)) {
            logger.info("No user cached, cannot perform cached login");
            throw new CacheMissException("No user cached", null);
        }

        if (password == null || !(password instanceof String)) {
            logger.info("No password cached, cannot perform cached login");
            throw new CacheMissException("No password cached", null);
        }

        return login((String) user, (String) password);
    }

    // #region refresh login
    /**
     * Requires session to be cached. Retrieves the refresh token from the cache and
     * calls {@link EpubClient#refreshLogin(String)} to refresh the session.
     * 
     * @return New credential infos
     * @throws CacheMissException If no refresh token is cached
     */
    public @Nullable CredentialDto refreshLogin() throws CacheMissException {
        String refreshToken = (String) checkCache(CacheType.CREDENTIALS, CredentialCacheKeys.REFRESH.getValue());
        if (refreshToken == null || refreshToken.isBlank()) {
            logger.info("Cannot refresh login, no login cached");
            throw new CacheMissException("No session cached", null);
        }
        logger.info("Refreshing login for cached session");

        return refreshLogin(refreshToken);
    }

    /**
     * Refreshes login with given refresh token
     * 
     * @param refreshToken Refresh token to refresh the session for
     * @return Refreshed session
     */
    public @Nullable CredentialDto refreshLogin(@NotNull String refreshToken) {
        return new CredentialDto();
    }

    // #region add epub
    /**
     * Pushes given epub to the api. Uses cached credentials to the applicaion. If
     * no jwt is cached, calls {@link EpubClient#login()} to retrieve a new jwt.
     * After jwt is retrieved, {@link EpubClient#addEpub(String, EpubDto)} is called
     * to actually upload the epub.
     * 
     * @param epub Epub to upload to the api
     * @return Uploaded epub or null if upload failed.
     * @throws CacheMissException       If no credentials are session tokens are
     *                                  cached.
     * @throws IllegalArgumentException If an invalid epub dto has been given.
     * @throws ApiCallException         If the call to the api itself failed.
     * @throws AuthorizationException   If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    public @Nullable EpubDto addEpub(@NotNull EpubDto epub)
            throws CacheMissException, IllegalArgumentException, ApiCallException, AuthorizationException {
        return addEpub(getCurrentJwt(), epub);
    }

    /**
     * Uploads given epub to the api. First calls
     * {@link EpubClient#login(String, String)} to retrieve a session token, with
     * which {@link EpubClient#addEpub(String, EpubDto)} is called to actually
     * upload the epub to the api.
     * 
     * @param username Username to log into the api with
     * @param password Password to log into the api with.
     * @param epub     Epub to upload to the api
     * @return Uploaded epub or null, if epub could not be uploaded
     * @throws IllegalArgumentException If an invalid epub dto has been given.
     * @throws IllegalStateException    If no jwt could be retrieved from the api
     *                                  and the client is not logged in.
     * @throws ApiCallException         If the call to the api itself failed.
     * @throws AuthorizationException   If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    public @Nullable EpubDto addEpub(@NotNull String username, @NotNull String password, @NotNull EpubDto epub)
            throws IllegalArgumentException, IllegalStateException, ApiCallException, AuthorizationException {
        return addEpub(getNewJwt(username, password), epub);
    }

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
     * @throws AuthorizationException   If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    private @Nullable EpubDto addEpub(@NotNull String jwt, @NotNull EpubDto epub)
            throws IllegalArgumentException, ApiCallException, AuthorizationException {
        if (epubs == null) {
            epubs = new EpubAdapter(this);
        }
        return epubs.addEpub(jwt, epub);
    }

    // #region delete epub
    /**
     * Deletes epub from the server
     * 
     * @param username Username to log into the application
     * @param password Password to log into the application
     * @param epubId   Id of the epub
     * @return The deleted epub
     * @throws IllegalStateException    If no session could be created
     * @throws IllegalArgumentException If username or password are missing
     * @throws ApiCallException         If an error occurred during call to the api
     * @throws AuthorizationException   If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    public @Nullable EpubDto deleteEpub(@NotNull String username, @NotNull String password, long epubId)
            throws IllegalStateException, IllegalArgumentException, ApiCallException, AuthorizationException {
        return deleteEpub(getNewJwt(username, password), epubId);
    }

    /**
     * Deletes epub from the server. Needs a credential cache to be registered in
     * the client.
     * 
     * @param epubId Id of the epub
     * @return The deleted epub
     * @throws CacheMissException       If necessary values are not yet cached
     * @throws IllegalArgumentException If username or password are missing
     * @throws ApiCallException         If an error occurred during call to the api
     * @throws AuthorizationException   If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    public @Nullable EpubDto deleteEpub(long epubId)
            throws CacheMissException, IllegalArgumentException, ApiCallException, AuthorizationException {
        return deleteEpub(getCurrentJwt(), epubId);
    }

    /**
     * Deletes epub from the server.
     * 
     * @param jwt    Token to log into the application with
     * @param epubId Id of the epub
     * @return The deleted epub
     * @throws IllegalArgumentException If username or password are missing
     * @throws ApiCallException         If an error occurred during call to the api
     * @throws AuthorizationException   If session has expired. If you are working
     *                                  without cache, call
     *                                  {@link EpubClient#refreshLogin(String)}
     *                                  again, otherwise a call to
     *                                  {@link EpubClient#refreshLogin()} suffices.
     */
    private @Nullable EpubDto deleteEpub(@NotNull String jwt, long epubId)
            throws IllegalArgumentException, ApiCallException, AuthorizationException {
        if (epubs == null) {
            logger.info("No epub adapter initialised, initialising now");
            epubs = new EpubAdapter(this);
        }
        return epubs.deleteEpub(jwt, epubId);
    }

    // #region get epub by id
    /**
     * Retrieves epub with given id from the api.
     * 
     * @param username Name of the user to log into the api
     * @param password Password to log into the api
     * @param epubId   Id of the epub to retrieve
     * @return Retrieved epub
     * @throws IllegalArgumentException
     * @throws AuthorizationException
     * @throws ApiCallException
     */
    public @Nullable EpubDto getEpub(@NotNull String username, @NotNull String password, long epubId, HttpQuery query)
            throws IllegalArgumentException, AuthorizationException, ApiCallException {
        return getEpub(getNewJwt(username, password), epubId, query);
    }

    /**
     * Retrieves epub with given id from the api. Needs cached credentials to work
     * 
     * @param epubId Id of the epub to retrieve
     * @return Retrieved epub
     * @throws IllegalArgumentException If the jwt is token
     * @throws CacheMissException       If credentials are missing in the cache
     * @throws AuthorizationException   If the session has expired or the api
     *                                  returned 401
     * @throws ApiCallException         If a general error occurred while calling
     *                                  the api
     */
    public @Nullable EpubDto getEpub(long epubId, HttpQuery query)
            throws IllegalArgumentException, CacheMissException, AuthorizationException, ApiCallException {
        return getEpub(getCurrentJwt(), epubId, query);
    }

    /**
     * Retrieves epub with given id from the api.
     * 
     * @param jwt    The session token
     * @param epubId Id of the epub to retrieve
     * @return Retrieved epub or null, if no epub with given id was found
     * @throws IllegalArgumentException If jwt is missing or blank
     * @throws AuthorizationException   If the session credentials are invalid
     * @throws ApiCallException         General api error exception
     */
    private @Nullable EpubDto getEpub(@NotNull String jwt, long epubId, HttpQuery query)
            throws IllegalArgumentException, AuthorizationException, ApiCallException {
        if (epubs == null) {
            logger.info("No epub adapter initialised, initialising now");
            epubs = new EpubAdapter(this);
        }
        return epubs.getEpub(jwt, epubId, query);
    }

    // #region add edition
    /**
     * Uploads given epub edition to the api for given epub id. Requires a session
     * to be cached
     * 
     * @param epubId  Id of the epub
     * @param edition Edition to add to the epub
     * @return Added edition or null, if edition could not be added
     * @throws CacheMissException       If no credentials are cached
     * @throws ApiCallException         General api error
     * @throws AuthorizationException   If cached credentials have expired
     * @throws IllegalArgumentException If given edition is missing the version name
     *                                  or is null
     * @throws NotFoundException        If no epub with given id exists
     * @throws BadRequestException      If the epub edition is invalid
     */
    public @Nullable EpubEditionDto addEpubEdition(long epubId, @NotNull EpubEditionDto edition)
            throws CacheMissException, ApiCallException, AuthorizationException, IllegalArgumentException,
            NotFoundException, BadRequestException {
        return addEpubEdition(getCurrentJwt(), epubId, edition);
    }

    /**
     * Uploads given epub edition to the api for given epub id.
     * 
     * @param username Username to log into the api
     * @param password Password to log into the api
     * @param epubId   Id of the epub to add the edition to
     * @param edition  Edition of the epub to add
     * @return Added edition or null, if edition could not be added
     * @throws ApiCallException         General api error
     * @throws AuthorizationException   If the session has expired
     * @throws IllegalArgumentException If no username, password, edition or
     *                                  edition.versionName are given
     * @throws NotFoundException        If no epub with given id exists
     * @throws BadRequestException      If the given edition is invalid
     */
    public @Nullable EpubEditionDto addEpubEdition(@NotNull String username, @NotNull String password, long epubId,
            @NotNull EpubEditionDto edition)
            throws ApiCallException, AuthorizationException, IllegalArgumentException, NotFoundException,
            BadRequestException {
        return addEpubEdition(getNewJwt(username, password), epubId, edition);
    }

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
     * @throws AuthorizationException   If the session has expired
     * @throws BadRequestException      If the given edition is invalid
     * @throws ApiCallException         General api error
     */
    private @Nullable EpubEditionDto addEpubEdition(@NotNull String jwt, long epubId, @NotNull EpubEditionDto edition)
            throws IllegalArgumentException, NotFoundException, AuthorizationException, BadRequestException,
            ApiCallException {
        if (epubs == null) {
            epubs = new EpubAdapter(this);
        }
        return epubs.addEpubEdition(jwt, epubId, edition);
    }

    // #region get all epubs
    /**
     * Gets all epubs meeting the specifications in query. If no query is given, all
     * are returned.
     * 
     * @param username Username for logging into the api
     * @param password Password for logging into the api
     * @param query    Query with epub requirements. Can be null
     * @return All epubs meeting the requirements
     * @throws ApiCallException       General api error
     * @throws AuthorizationException If the session has expired
     */
    public @Nullable PagedRequestDto<EpubDto> getAllEpubs(@NotNull String username, @NotNull String password,
            @Nullable HttpQuery query) throws ApiCallException, AuthorizationException {
        return getAllEpubs(getNewJwt(username, password), query);
    }

    /**
     * Gets all epubs meeting the specifications in query. If no query is given, all
     * are returned.
     * 
     * @param query Query with epub requirements. Can be null
     * @return All epubs meeting the requirements
     * @throws CacheMissException     If no username or password are in the clients
     *                                internal cache
     * @throws ApiCallException       General api error
     * @throws AuthorizationException If the session has expired
     */
    public @Nullable PagedRequestDto<EpubDto> getAllEpubs(@Nullable HttpQuery query)
            throws CacheMissException, ApiCallException, AuthorizationException {
        return getAllEpubs(getCurrentJwt(), query);
    }

    /**
     * Queries all epubs on the api and returns those meeting the requirements of
     * the query.
     * 
     * @param jwt   Jwt for logging into the api
     * @param query Query with requirements for the epubs to be returned.
     * @return All epubs meeting the requirements
     * @throws ApiCallException       General api error
     * @throws AuthorizationException If the session has expired
     */
    private @Nullable PagedRequestDto<EpubDto> getAllEpubs(@NotNull String jwt,
            @Nullable HttpQuery query) throws ApiCallException, AuthorizationException {
        if (epubs == null)
            epubs = new EpubAdapter(this);

        return epubs.getEpubsPaged(jwt, query);
    }

    // #region upload
    /**
     * Uploads the epub the the epub edition with the given upload guid.
     * 
     * @param username   Name of the user to log into the api
     * @param password   Password of the user
     * @param uploadGuid Upload guid of the EpubEdition.
     * @param epubFile   File to be uploaded
     * @throws IllegalArgumentException If one or more arguments are missing
     * @throws IllegalFileTypeException If the given file is not a .epub file.
     * @throws ApiCallException         General api error
     * @throws AuthorizationException   If the sessionhas expired
     * @throws BadRequestException      If the request was malformed in some way.
     */
    public void uploadEpub(String username, String password, @NotNull String uploadGuid, @NotNull File epubFile)
            throws IllegalArgumentException, IllegalFileTypeException, ApiCallException, AuthorizationException,
            BadRequestException {
        uploadEpub(getNewJwt(username, password), uploadGuid, epubFile);
    }

    /**
     * Uploads the epub the the epub edition with the given upload guid. Needs api
     * credentials to be cached in this client.
     * 
     * @param uploadGuid Upload guid of the EpubEdition.
     * @param epubFile   File to be uploaded
     * @throws IllegalArgumentException If one or more arguments are missing
     * @throws IllegalFileTypeException If the given file is not a .epub file.
     * @throws ApiCallException         General api error
     * @throws AuthorizationException   If the sessionhas expired
     * @throws BadRequestException      If the request was malformed in some way.
     * @throws CacheMissException       If no credentials are stored in the clients
     *                                  cache
     */
    public void uploadEpub(@NotNull String uploadGuid, @NotNull File epubFile)
            throws IllegalArgumentException, IllegalFileTypeException, ApiCallException, AuthorizationException,
            BadRequestException, CacheMissException {
        uploadEpub(getCurrentJwt(), uploadGuid, epubFile);
    }

    /**
     * Uploads the epub the the epub edition with the given upload guid. Needs api
     * credentials to be cached in this client.
     * 
     * @param jwt        The token to authorise with the api
     * @param uploadGuid Upload guid of the EpubEdition.
     * @param epubFile   File to be uploaded
     * @throws IllegalArgumentException If one or more arguments are missing
     * @throws IllegalFileTypeException If the given file is not a .epub file.
     * @throws ApiCallException         General api error
     * @throws AuthorizationException   If the sessionhas expired
     * @throws BadRequestException      If the request was malformed in some way.
     */
    private void uploadEpub(@NotNull String jwt, @NotNull String uploadGuid, @NotNull File epubFile)
            throws IllegalArgumentException, IllegalFileTypeException, ApiCallException, AuthorizationException,
            BadRequestException {
        if (epubs == null)
            epubs = new EpubAdapter(this);

        epubs.upload(jwt, uploadGuid, epubFile);
    }

    // #region download
    /**
     * Downloads the epub file or cover image of the epub edition with the given
     * download guid.
     * 
     * @param username          Username to log into the api with.
     * @param password          Password to log into the api with.
     * @param downloadGuid      Download guid of the epub edition.
     * @param downloadDirectory Directory into which the file should be downloaded.
     * @param downloadCover     If set to true, the cover instead of the epub is
     *                          downloaded.
     * @return The downloaded file
     * @throws IllegalArgumentException  If one of the arguments required is missing
     * @throws BadRequestException       If the download guid is invalid.
     * @throws ServerErrorException      If the server failed in providing the
     *                                   download.
     * @throws ApiCallException          General api call exception.
     * @throws AuthorizationException    If the client could not authenticate at the
     *                                   api.
     * @throws UnexpectedStatusException If the server responded with an unexpected
     *                                   status.
     */
    public @Nullable File download(@NotNull String username, @NotNull String password, @NotNull String downloadGuid,
            @NotNull File downloadDirectory, boolean downloadCover) throws IllegalArgumentException,
            BadRequestException, ServerErrorException, ApiCallException, AuthorizationException,
            UnexpectedStatusException {
        return download(getNewJwt(username, password), downloadGuid, downloadDirectory, downloadCover);
    }

    /**
     * Downloads the epub file or cover image of the epub edition with the given
     * download guid. Requires cached login data.
     * 
     * @param downloadGuid      Download guid of the epub edition.
     * @param downloadDirectory Directory into which the file should be downloaded.
     * @param downloadCover     If set to true, the cover instead of the epub is
     *                          downloaded.
     * @return The downloaded file
     * @throws IllegalArgumentException  If one of the arguments required is missing
     * @throws BadRequestException       If the download guid is invalid.
     * @throws ServerErrorException      If the server failed in providing the
     *                                   download.
     * @throws ApiCallException          General api call exception.
     * @throws AuthorizationException    If the client could not authenticate at the
     *                                   api.
     * @throws CacheMissException        If no credentials are stored in the clients
     *                                   cache.
     * @throws UnexpectedStatusException If the server responded with an unexpected
     *                                   status
     */
    public @Nullable File download(@NotNull String downloadGuid, @NotNull File downloadDirectory,
            boolean downloadCover) throws IllegalArgumentException, BadRequestException, ServerErrorException,
            CacheMissException, ApiCallException, AuthorizationException, UnexpectedStatusException {
        return download(getCurrentJwt(), downloadGuid, downloadDirectory, downloadCover);
    }

    /**
     * Downloads the epub file or cover image of the epub edition with the given
     * download guid.
     * 
     * @param username          Username to log into the api with.
     * @param password          Password to log into the api with.
     * @param downloadGuid      Download guid of the epub edition.
     * @param downloadDirectory Directory into which the file should be downloaded.
     * @param downloadCover     If set to true, the cover instead of the epub is
     *                          downloaded.
     * @return The downloaded file
     * @throws IllegalArgumentException  If one of the arguments required is missing
     * @throws BadRequestException       If the download guid is invalid.
     * @throws ServerErrorException      If the server failed in providing the
     *                                   download.
     * @throws AuthorizationException    If the jwt is invalid
     * @throws UnexpectedStatusException If the server responded with an unexpected
     *                                   status.
     */
    private final @Nullable File download(@NotNull String jwt, @NotNull String downloadGuid,
            @NotNull File downloadDirectory, boolean downloadCover)
            throws IllegalArgumentException, BadRequestException, ServerErrorException, AuthorizationException,
            UnexpectedStatusException {

        if (epubs == null) {
            epubs = new EpubAdapter(this);
        }
        return epubs.download(jwt, downloadGuid, downloadDirectory, downloadCover);
    }

    // #region add tag to epub
    /**
     * Adds tag with given id to epub with given id.
     * 
     * @param username Username to authenticate at the api with.
     * @param password Password to authenticate at the api with.
     * @param epubId   Id of the epub to add the tag to.
     * @param tagId    Id of the tag to add to the epub.
     * @return Updated epub or null, if tag is already associated with epub.
     * @throws IllegalArgumentException If username or password are missing
     * @throws BadRequestException      If no tag with given id does not exist.
     * @throws AuthorizationException   If authorization at the api failed.
     * @throws NotFoundException        If no epub with given id does not exist.
     * @throws ApiCallException         General wrapper for all unexpected responses
     *                                  from the api.
     */
    public @Nullable EpubDto addTagToEpub(@NotNull String username, @NotNull String password, long epubId, long tagId)
            throws IllegalArgumentException, BadRequestException, AuthorizationException, NotFoundException,
            ApiCallException {
        return addTagToEpub(getNewJwt(username, password), epubId, tagId);
    }

    /**
     * Adds tag with given id to epub with given id.
     * 
     * @param epubId Id of the epub to add the tag to.
     * @param tagId  Id of the tag to add to the epub.
     * @return Updated epub or null, if tag is already associated with epub.
     * @throws IllegalArgumentException If username or password are missing
     * @throws BadRequestException      If no tag with given id does not exist.
     * @throws AuthorizationException   If authorization at the api failed.
     * @throws NotFoundException        If no epub with given id does not exist.
     * @throws ApiCallException         General wrapper for all unexpected responses
     *                                  from the api.
     */
    public @Nullable EpubDto addTagToEpub(long epubId, long tagId) throws IllegalArgumentException, BadRequestException,
            AuthorizationException, NotFoundException, ApiCallException, CacheMissException {
        return addTagToEpub(getCurrentJwt(), epubId, tagId);
    }

    /**
     * Adds tag with given id to epub with given id.
     * 
     * @param jwt    JWT to authenticate at the api with.
     * @param epubId Id of the epub to add the tag to.
     * @param tagId  Id of the tag to add to the epub.
     * @return Updated epub or null, if tag is already associated with epub.
     * @throws IllegalArgumentException If username or password are missing
     * @throws BadRequestException      If no tag with given id does not exist.
     * @throws AuthorizationException   If authorization at the api failed.
     * @throws NotFoundException        If no epub with given id does not exist.
     * @throws ApiCallException         General wrapper for all unexpected responses
     *                                  from the api.
     */
    private @Nullable EpubDto addTagToEpub(@NotNull String jwt, long epubId, long tagId)
            throws IllegalArgumentException,
            BadRequestException, AuthorizationException, NotFoundException, ApiCallException {
        if (epubs == null)
            epubs = new EpubAdapter(this);
        return epubs.addTagToEpub(jwt, epubId, tagId);
    }
    // #endregion add tag to epub

    public @Nullable EpubDto updateEpub(@NotNull String username, @NotNull String password, long epubId,
            @NotNull EpubDto updated, boolean overwriteNulls) throws IllegalArgumentException, AuthorizationException,
            BadRequestException, NotFoundException, ApiCallException {
        return updateEpub(getNewJwt(username, password), updated, epubId, overwriteNulls);
    }

    public @Nullable EpubDto updateEpub(long epubId, @NotNull EpubDto updated, boolean overwriteNulls)
            throws IllegalArgumentException, AuthorizationException, BadRequestException, NotFoundException,
            ApiCallException, CacheMissException {
        return updateEpub(getCurrentJwt(), updated, epubId, overwriteNulls);
    }

    public @Nullable EpubDto updateEpub(@NotNull String jwt, @NotNull EpubDto updated, long epubId,
            boolean overwriteNulls) throws IllegalArgumentException, AuthorizationException, BadRequestException,
            NotFoundException, ApiCallException {
        if (epubs == null)
            epubs = new EpubAdapter(this);
        return epubs.updateEpub(jwt, updated, epubId, overwriteNulls);
    }

    // #region delete epub edition
    /**
     * Deletes the epub edition with the given edition id
     * 
     * @param username  User name to authenticate at the api
     * @param password  Password to authenticate at the api
     * @param epubId    Id of the epub the edition belongs to
     * @param editionId Id of the edition to be deleted
     * @return The deleted epub edition
     * @throws ApiCallException         If the api returned an unexpected status
     * @throws AuthorizationException   If the credentials are incorrect
     * @throws IllegalArgumentException If username or password are not given
     * @throws NotFoundException        If either the epub id or edition id do not
     *                                  exist
     */
    public @Nullable EpubEditionDto deleteEpubEdition(@NotNull String username, @NotNull String password, long epubId,
            long editionId)
            throws ApiCallException, AuthorizationException, IllegalArgumentException, NotFoundException {
        return deleteEpubEdition(getNewJwt(username, password), epubId, editionId);
    }

    /**
     * Deletes the epub edition with the given edition id. Credentials must be
     * cached in the client.
     * 
     * @param epubId    Id of the epub the edition belongs to
     * @param editionId Id of the edition to be deleted
     * @return The deleted epub edition
     * @throws ApiCallException         If the api returned an unexpected status
     * @throws AuthorizationException   If the credentials are incorrect
     * @throws IllegalArgumentException If username or password are not given
     * @throws NotFoundException        If either the epub id or edition id do not
     *                                  exist
     * @throws CacheMissException       If no credentials are stored in the clients
     *                                  cache
     */
    public @Nullable EpubEditionDto deleteEpubEdition(long epubId,
            long editionId) throws CacheMissException, ApiCallException, AuthorizationException,
            IllegalArgumentException, NotFoundException {
        return deleteEpubEdition(getCurrentJwt(), epubId, editionId);
    }

    /**
     * Deletes the epub edition with the given edition id
     * 
     * @param jwt       JWT to authenticate at the api with.
     * @param epubId    Id of the epub the edition belongs to
     * @param editionId Id of the edition to be deleted
     * @return The deleted epub edition
     * @throws ApiCallException         If the api returned an unexpected status
     * @throws AuthorizationException   If the credentials are incorrect
     * @throws IllegalArgumentException If username or password are not given
     * @throws NotFoundException        If either the epub id or edition id do not
     *                                  exist
     */
    private final @Nullable EpubEditionDto deleteEpubEdition(@NotNull String jwt, long epubId, long editionId)
            throws IllegalArgumentException, AuthorizationException, ApiCallException, NotFoundException {
        if (epubs == null)
            epubs = new EpubAdapter(this);

        return epubs.deleteEdition(jwt, epubId, editionId);
    }

    // #region add author
    /**
     * Adds a new author to the system. At least author.firstName and author.surname
     * are required, everything else is optional.
     * 
     * @param username Username to log into the api with
     * @param password Password to log into the api with.
     * @param author   Author to add to the system.
     * @return Added author
     * @throws IllegalArgumentException If username, password, author.firstName or
     *                                  author.surname are not given.
     * @throws ApiCallException         General wrapper for unexpected api behaviour
     * @throws AuthorizationException   If authentication at the api did not work
     * @throws BadRequestException      If the author dto was invalid.
     */
    public @Nullable AuthorDto addAuthor(@NotNull String username, @NotNull String password,
            @NotNull AuthorDto author)
            throws IllegalArgumentException, ApiCallException, AuthorizationException, BadRequestException {
        return addAuthor(getNewJwt(username, password), author);
    }

    /**
     * Adds a new author to the system. At least author.firstName and author.surname
     * are required, everything else is optional. Needs cached credentials to work
     * 
     * @param author Author to add to the system.
     * @return Added author
     * @throws IllegalArgumentException If author.firstName or
     *                                  author.surname are not given.
     * @throws ApiCallException         General wrapper for unexpected api behaviour
     * @throws AuthorizationException   If authentication at the api did not work
     * @throws BadRequestException      If the author dto was invalid.
     * @throws CacheMissException       If no credentials are cached within the
     *                                  client.
     */
    public @Nullable AuthorDto addAuthor(@NotNull AuthorDto author) throws IllegalArgumentException, ApiCallException,
            AuthorizationException, BadRequestException, CacheMissException {
        return addAuthor(getCurrentJwt(), author);
    }

    /**
     * Adds a new author to the system. At least author.firstName and author.surname
     * are required, everything else is optional.
     * 
     * @param jwt    JWT to authenticate at the api with.
     * @param author Author to add to the system.
     * @return Added author
     * @throws IllegalArgumentException If jwt author.firstName or
     *                                  author.surname are not given.
     * @throws ApiCallException         General wrapper for unexpected api behaviour
     * @throws AuthorizationException   If authentication at the api did not work
     * @throws BadRequestException      If the author dto was invalid.
     */
    private @Nullable AuthorDto addAuthor(@NotNull String jwt, @NotNull AuthorDto author)
            throws IllegalArgumentException, ApiCallException, AuthorizationException, BadRequestException {
        if (authors == null)
            authors = new AuthorAdapter(this);
        return authors.addAuthor(jwt, author);
    }
    // #endregion add author

    // #region get author
    /**
     * Returns the author with given id. Query defines what the backend should add
     * into the AuthorDto (for example if epubs should be returned as well.)
     * Example of getting author with id 1, as well as all epubs associated with it:
     * 
     * <pre>
     * {@code
     * HttpQuery query = new AuthorQueryBuilder().withEpubs(true).build();
     * AuthorDto dto = client.getAuthor("admin", "admin", 1L, query);
     * }
     * </pre>
     * 
     * @param username Username to authenticate at the api
     * @param password Password to authenticate at the api
     * @param authorId Id of the author to return
     * @param query    Query for getting author.
     * @return Requested author.
     * @throws IllegalArgumentException If username or password are missing
     * @throws ApiCallException         General api error wrapper
     * @throws AuthorizationException   If client could not authenticate
     */
    public @Nullable AuthorDto getAuthor(@NotNull String username, @NotNull String password, long authorId,
            @Nullable HttpQuery query) throws IllegalArgumentException, ApiCallException, AuthorizationException {
        return getAuthor(getNewJwt(username, password), authorId, query);
    }

    /**
     * Returns the author with given id. Query defines what the backend should add
     * into the AuthorDto (for example if epubs should be returned as well.)
     * Requires cached credentials.
     * Example of getting author with id 1, as well as all epubs associated with it:
     * 
     * <pre>
     * {@code
     * HttpQuery query = new AuthorQueryBuilder().withEpubs(true).build();
     * AuthorDto dto = client.getAuthor(1L, query);
     * }
     * </pre>
     * 
     * @param authorId Id of the author to return
     * @param query    Query for getting author.
     * @return Requested author.
     * @throws CacheMissException
     * @throws ApiCallException       General api error wrapper
     * @throws AuthorizationException If client could not authenticate
     */
    public @Nullable AuthorDto getAuthor(long authorId, @Nullable HttpQuery query)
            throws CacheMissException, ApiCallException, AuthorizationException {
        return getAuthor(getCurrentJwt(), authorId, query);
    }

    /**
     * Returns the author with given id. Query defines what the backend should add
     * into the AuthorDto (for example if epubs should be returned as well.)
     * Example of getting author with id 1, as well as all epubs associated with it:
     * 
     * <pre>
     * {@code
     * HttpQuery query = new AuthorQueryBuilder().withEpubs(true).build();
     * AuthorDto dto = client.getAuthor("jwt-123", 1L, query);
     * }
     * </pre>
     * 
     * @param jwt      JWT to authenticate at the api
     * @param authorId Id of the author to return
     * @param query    Query for getting author.
     * @return Requested author.
     * @throws IllegalArgumentException If username or password are missing
     * @throws ApiCallException         General api error wrapper
     * @throws AuthorizationException   If client could not authenticate
     */
    private @Nullable AuthorDto getAuthor(@NotNull String jwt, long authorId, @Nullable HttpQuery query)
            throws ApiCallException, AuthorizationException {
        if (authors == null)
            authors = new AuthorAdapter(this);
        return authors.getAuthorById(jwt, authorId, query);
    }
    // #endregion get author

    // #region delete author
    /**
     * Deletes author with given id. If deleteWithBooks is true, epubs associated
     * with the author are deleted as well.
     * 
     * @param username        Username to log into the api
     * @param password        password to log into the api
     * @param authorId        Id of the author to delete
     * @param deleteWithBooks If set to true, all epubs associated with the author
     *                        are deleted as well.
     * @return Deleted author
     * @throws ApiCallException         General wrapper for all
     * @throws AuthorizationException   If authorization failed
     * @throws IllegalArgumentException If username or password are missing
     * @throws BadRequestException      If the api returned 400
     */
    public @Nullable AuthorDto deleteAuthor(@NotNull String username, @NotNull String password, long authorId,
            boolean deleteWithBooks)
            throws ApiCallException, AuthorizationException, IllegalArgumentException,
            BadRequestException {
        return deleteAuthor(getNewJwt(username, password), authorId, deleteWithBooks);
    }

    /**
     * Deletes author with given id. If deleteWithBooks is true, epubs associated
     * with the author are deleted as well. Requires credentials to be stored in the
     * clients cache.
     * 
     * @param authorId        Id of the author to delete
     * @param deleteWithBooks If set to true, all epubs associated with the author
     *                        are deleted as well.
     * @return Deleted author
     * @throws ApiCallException         General wrapper for all
     * @throws AuthorizationException   If authorization failed
     * @throws IllegalArgumentException If credentials are missing
     * @throws BadRequestException      If the api returned 400
     * @throws CacheMissException       If no credentials are stored in the clients
     *                                  cache
     */
    public @Nullable AuthorDto deleteAuthor(long authorId, boolean deleteWithBooks)
            throws CacheMissException, ApiCallException, AuthorizationException,
            IllegalArgumentException,
            BadRequestException {
        return deleteAuthor(getCurrentJwt(), authorId, deleteWithBooks);
    }

    /**
     * Deletes author with given id. If deleteWithBooks is true, epubs associated
     * with the author are deleted as well.
     * 
     * @param jwt             JWT to authorize at the api
     * @param authorId        Id of the author to delete
     * @param deleteWithBooks If set to true, all epubs associated with the author
     *                        are deleted as well.
     * @return Deleted author
     * @throws ApiCallException         General wrapper for all
     * @throws AuthorizationException   If authorization failed
     * @throws IllegalArgumentException If credentials are missing
     * @throws BadRequestException      If the api returned 400
     */
    private @Nullable AuthorDto deleteAuthor(@NotNull String jwt, long authorId, boolean deleteWithBooks)
            throws IllegalArgumentException, AuthorizationException, ApiCallException, BadRequestException {
        if (authors == null)
            authors = new AuthorAdapter(this);
        return authors.deleteAuthor(jwt, authorId, deleteWithBooks);
    }
    // #endregion delete author

    // #region update author
    /**
     * Updates author with given author id. If overwrite nulls is set to true, first
     * name and surname must be given, as all values not given are overwritten with
     * null.
     * 
     * @param username       Name of the user to authenticate with at the api.
     * @param password       Password of the user.
     * @param author         Update author information
     * @param authorId       Id of the author
     * @param overwriteNulls If set to true, all values not given are set to null.
     *                       First name and surname cannot be overwriten with null.
     * @return
     * @throws IllegalArgumentException
     * @throws ApiCallException
     * @throws BadRequestException
     * @throws AuthorizationException
     */
    public @Nullable AuthorDto updateAuthor(@NotNull String username, @NotNull String password,
            @NotNull AuthorDto author, long authorId, boolean overwriteNulls)
            throws IllegalArgumentException, ApiCallException, BadRequestException, AuthorizationException {
        return updateAuthor(getNewJwt(username, password), author, authorId, overwriteNulls);
    }

    public @Nullable AuthorDto updateAuthor(@NotNull AuthorDto author, long authorId, boolean overwriteNulls)
            throws IllegalArgumentException, ApiCallException, BadRequestException, AuthorizationException,
            CacheMissException {
        return updateAuthor(getCurrentJwt(), author, authorId, overwriteNulls);
    }

    private @Nullable AuthorDto updateAuthor(@NotNull String jwt, @NotNull AuthorDto dto, long authorId,
            boolean overwriteNulls)
            throws IllegalArgumentException, ApiCallException, BadRequestException, AuthorizationException {
        if (authors == null)
            authors = new AuthorAdapter(this);
        return authors.updateAuthor(jwt, dto, authorId, overwriteNulls);
    }

    // #endregion update author

    // #region add epub to author
    /**
     * Adds epub with given id to author with given id.
     * 
     * @param username Username to authenticate at the api
     * @param password Password to authenticate at the api.
     * @param authorId Id of the author to add the epub to.
     * @param epubId   Id of the epub to add to the author.
     * @return The updated author or null, if epub was already associated with the
     *         author or epub does not exist.
     * @throws IllegalArgumentException If username or password are missing
     * @throws AuthorizationException   If authorization at the api failed
     * @throws ApiCallException         General wrapper for all unexpected responses
     *                                  from the api.
     * @throws NotFoundException        If no author with given id exists.
     */
    public @Nullable AuthorDto addEpubToAuthor(@NotNull String username, @NotNull String password, long authorId,
            long epubId) throws IllegalArgumentException, AuthorizationException, ApiCallException, NotFoundException {
        return addEpubToAuthor(getNewJwt(username, password), authorId, epubId);
    }

    /**
     * Adds epub with given id to author with given id. Requires credentials to be
     * cached in this client.
     *
     * @param authorId Id of the author to add the epub to.
     * @param epubId   Id of the epub to add to the author.
     * @return The updated author or null, if epub was already associated with the
     *         author or epub does not exist.
     * @throws IllegalArgumentException If username or password are missing
     * @throws AuthorizationException   If authorization at the api failed
     * @throws ApiCallException         General wrapper for all unexpected responses
     *                                  from the api.
     * @throws NotFoundException        If no author with given id exists.
     */
    public @Nullable AuthorDto addEpubToAuthor(long authorId, long epubId) throws IllegalArgumentException,
            AuthorizationException, ApiCallException, NotFoundException, CacheMissException {
        return addEpubToAuthor(getCurrentJwt(), authorId, epubId);
    }

    /**
     * Adds epub with given id to author with given id.
     * 
     * @param jwt      JWT to authenticate at the api with.
     * @param authorId Id of the author to add the epub to.
     * @param epubId   Id of the epub to add to the author.
     * @return The updated author or null, if epub was already associated with the
     *         author or epub does not exist.
     * @throws IllegalArgumentException If username or password are missing
     * @throws AuthorizationException   If authorization at the api failed
     * @throws ApiCallException         General wrapper for all unexpected responses
     *                                  from the api.
     * @throws NotFoundException        If no author with given id exists.
     */
    private @Nullable AuthorDto addEpubToAuthor(@NotNull String jwt, long authorId, long epubId)
            throws IllegalArgumentException, AuthorizationException, ApiCallException, NotFoundException {
        if (authors == null)
            authors = new AuthorAdapter(this);
        return authors.addEpubToAuthor(jwt, authorId, epubId);
    }
    // #endregion add epub to author

    // #region add tag
    /**
     * Creates a new tag with given name and colour. Name cannot be null. If name
     * equals name of an existing tag, null will be returned.
     * 
     * @param username  Name of the user to authorize at the api with
     * @param password  Password of the user to authorize at the api with
     * @param tagName   Name of the new tag
     * @param tagColour Colour of the new tag. Defaults to blank
     * @return The newly created tag or null, if a tag with the given name already
     *         exists.
     * @throws IllegalArgumentException If username or password are missing
     * @throws BadRequestException      Won't happen
     * @throws ApiCallException         Wrapper for all unexpected api exceptions.
     * @throws AuthorizationException   If authorization at the api failed.
     */
    public @Nullable TagDto addTag(@NotNull String username, @NotNull String password, @NotNull String tagName,
            @Nullable String tagColour)
            throws IllegalArgumentException, BadRequestException, ApiCallException, AuthorizationException {
        return addTag(getNewJwt(username, password), tagName, tagColour);
    }

    /**
     * Creates a new tag with given name and colour. Name cannot be null. If name
     * equals name of an existing tag, null will be returned. Needs cached
     * credentials.
     * 
     * @param tagName   Name of the new tag
     * @param tagColour Colour of the new tag. Defaults to blank
     * @return The newly created tag or null, if a tag with the given name already
     *         exists.
     * @throws IllegalArgumentException If username or password are missing
     * @throws BadRequestException      Won't happen
     * @throws ApiCallException         Wrapper for all unexpected api exceptions.
     * @throws AuthorizationException   If authorization at the api failed
     * @throws CacheMissException       If no credentials are stored in the cache..
     */
    public @Nullable TagDto addTag(@NotNull String tagName, @Nullable String tagColour) throws IllegalArgumentException,
            BadRequestException, ApiCallException, AuthorizationException, CacheMissException {
        return addTag(getCurrentJwt(), tagName, tagColour);
    }

    /**
     * Creates a new tag with given name and colour. Name cannot be null. If name
     * equals name of an existing tag, null will be returned.
     * 
     * @param jwt       Jwt to authorize at the api with.
     * @param tagName   Name of the new tag
     * @param tagColour Colour of the new tag. Defaults to blank
     * @return The newly created tag or null, if a tag with the given name already
     *         exists.
     * @throws IllegalArgumentException If username or password are missing
     * @throws BadRequestException      Won't happen
     * @throws ApiCallException         Wrapper for all unexpected api exceptions.
     * @throws AuthorizationException   If authorization at the api failed.
     */
    private @Nullable TagDto addTag(@NotNull String jwt, @NotNull String tagName, @Nullable String tagColour)
            throws IllegalArgumentException, BadRequestException, ApiCallException, AuthorizationException {
        if (tags == null)
            tags = new TagAdapter(this);
        return tags.createTag(jwt, tagName, tagColour);
    }
    // #endregion add tag

    // #region delete tag
    /**
     * Deletes tag with given id.
     * 
     * @param username Username to authorize at the api with
     * @param password Password to authorize at the api with.
     * @param tagId    Id of the tag to be deleted
     * @return The deleted tag or null, if no tag with that id exists.
     * @throws IllegalArgumentException If username or password are missing or
     *                                  incorrect
     * @throws AuthorizationException   If authorization at the api failed.
     * @throws ApiCallException         General wrapper for all unexpected status
     *                                  codes the api might return.
     */
    public @Nullable TagDto deleteTag(@NotNull String username, @NotNull String password, long tagId)
            throws IllegalArgumentException, AuthorizationException, ApiCallException {
        return deleteTag(getNewJwt(username, password), tagId);
    }

    /**
     * Deletes tag with given id. Requires cached credentials.
     * 
     * @param tagId Id of the tag to be deleted
     * @return The deleted tag or null, if no tag with that id exists.
     * @throws IllegalArgumentException If username or password are missing or
     *                                  incorrect
     * @throws AuthorizationException   If authorization at the api failed.
     * @throws ApiCallException         General wrapper for all unexpected status
     *                                  codes the api might return.
     * @throws CacheMissException       If no credentials are cached in this client.
     */
    public @Nullable TagDto deleteTag(long tagId)
            throws IllegalArgumentException, AuthorizationException, ApiCallException, CacheMissException {
        return deleteTag(getCurrentJwt(), tagId);
    }

    /**
     * Deletes tag with given id.
     * 
     * @param jwt   Jwt to authorize at the api with.
     * @param tagId Id of the tag to be deleted
     * @return The deleted tag or null, if no tag with that id exists.
     * @throws IllegalArgumentException If username or password are missing or
     *                                  incorrect
     * @throws AuthorizationException   If authorization at the api failed.
     * @throws ApiCallException         General wrapper for all unexpected status
     *                                  codes the api might return.
     */
    private @Nullable TagDto deleteTag(@NotNull String jwt, long tagId)
            throws IllegalArgumentException, AuthorizationException, ApiCallException {
        if (tags == null)
            tags = new TagAdapter(this);
        return tags.deleteTag(jwt, tagId);
    }
    // #endregion delete tag

    // #region register cache
    /**
     * Adds a new cache type to the clients internal caches, if such a cache type is
     * not already registered.
     * 
     * @param type      Type of the cache to register
     * @param cache     Cache to register
     * @param overwrite If true, existing cache with given type is overwritten.
     */
    public final void registerCache(@NotNull CacheType type, @NotNull Cache<?, ?> cache, boolean overwrite) {
        if (!overwrite)
            caches.putIfAbsent(type, cache);
        else
            caches.put(type, cache);
    }

    // #region add headers
    /**
     * Adds all relevant headers to the given {@link Request.Builder}
     * 
     * @param builder     Builder to modify
     * @param jwt         Bearer token to log into the application with
     * @param accept      Accepted content type
     * @param contentType Sent content type
     */
    protected final void addHeaders(@NotNull Request.Builder builder, @NotNull String jwt, @NotNull String accept,
            @NotNull String contentType) {
        builder.addHeader("Authorization", "Bearer " + jwt)
                .addHeader("Accept", accept)
                .addHeader("Content-Type", contentType);
    }

    // #region get current jwt
    /**
     * Fetches jwt from cache. If no jwt exists in cache, tries to create a new one
     * by calling {@link EpubClient#login()}.
     * 
     * @return The generated jwt
     * @throws CacheMissException     If username or password are not in cache
     * @throws AuthorizationException
     * @throws ApiCallException
     */
    private String getCurrentJwt() throws CacheMissException, ApiCallException, AuthorizationException {
        Object jwtObj = checkCache(CacheType.CREDENTIALS, CredentialCacheKeys.JWT.getValue());
        String jwt;
        if (jwtObj == null || !(jwtObj instanceof String)) {
            logger.info("No jwt cached, trying cached login");
            CredentialDto creds = login();
            jwt = creds.getJwt();
        } else {
            jwt = (String) jwtObj;
        }

        return jwt;
    }

    // #region get new jwt
    /**
     * Generates a new jwt by calling {@link EpubClient#login(String, String)}.
     * 
     * @param username Username to log into the api
     * @param password Password to log into the apui
     * @return Generated session token
     * @throws AuthorizationException
     * @throws ApiCallException
     */
    private String getNewJwt(@NotNull String username, @NotNull String password)
            throws ApiCallException, AuthorizationException {
        CredentialDto dto = login(username, password);
        if (dto == null || dto.getJwt() == null || dto.getJwt().isBlank()) {
            logger.info("Could not acquire jwt for login");
            throw new IllegalStateException("Not logged in");
        }
        return dto.getJwt();
    }

    // #region execute request
    /**
     * Executes the given request and returns the expected type.
     * 
     * @param <T>     expected type of the response body
     * @param request Request to be executed
     * @param type    Expected type of the request body
     * @return The response body or null, if the server answered with 204
     * @throws AuthorizationException    If the server answered with 401
     * @throws BadRequestException       If the server answered with 400
     * @throws ForbiddenException        If the server answered with 403
     * @throws NotFoundException         If the server answered with 404
     * @throws ServerErrorException      If the server answered with 500
     * @throws UnexpectedStatusException If the server answered with any status not
     *                                   previously defined
     * @throws IOException               If the request could not be executed due to
     *                                   an io exception
     */
    protected @Nullable <T> T executeRequest(@NotNull Request request, @NotNull Class<T> type,
            @Nullable HttpQuery query,
            boolean cacheRequest)
            throws AuthorizationException, BadRequestException, ForbiddenException, NotFoundException,
            ServerErrorException, UnexpectedStatusException, IOException {
        T dto = null;

        if (cacheRequest) {
            cacheReqeuest(request, type, query);
        }

        try (Response response = client.newCall(request).execute()) {
            int responseCode = response.code();
            switch (responseCode) {
                case 200:
                    logger.info("Request successful parsing body");
                    if (type.equals(Void.class)) {
                        logger.info("Expecting void, returning");
                        return null;
                    }
                    ResponseBody resp = response.body();
                    if (resp == null) {
                        logger.info("No response body");
                        return null;
                    }
                    String body = resp.string();
                    if (body == null || body.isBlank()) {
                        logger.info("No response body");
                        return null;
                    }

                    dto = mapper.readValue(body, type);
                    break;
                case 204:
                    logger.info("No content");
                    return null;
                case 400:
                    logger.info("Bad request");
                    throw new BadRequestException();
                case 401:
                    logger.info("Session expired");
                    throw new AuthorizationException();
                case 403:
                    logger.info("Action forbidden");
                    throw new ForbiddenException();
                case 404:
                    logger.info("Not found");
                    throw new NotFoundException();
                case 500:
                    logger.info("Server error");
                    throw new ServerErrorException();
                default:
                    logger.info("Unexpected status received");
                    throw new UnexpectedStatusException();
            }
        } catch (IOException e) {
            logger.info("Exception occurred during api call", e);
            throw e;
        }

        return dto;
    }

    // #region execute request paged
    /**
     * Executes the given request and returns the expected type.
     * 
     * @param <T>     expected type of the response body
     * @param request Request to be executed
     * @param type    Expected type of the request body
     * @return The response body or null, if the server answered with 204
     * @throws AuthorizationException    If the server answered with 401
     * @throws BadRequestException       If the server answered with 400
     * @throws ForbiddenException        If the server answered with 403
     * @throws NotFoundException         If the server answered with 404
     * @throws ServerErrorException      If the server answered with 500
     * @throws UnexpectedStatusException If the server answered with any status not
     *                                   previously defined
     * @throws IOException               If the request could not be executed due to
     *                                   an io exception
     */
    protected <T> PagedRequestDto<T> executeRequestPaged(@NotNull Request request, @NotNull Class<T> type,
            @Nullable HttpQuery query,
            boolean cacheRequest)
            throws AuthorizationException, BadRequestException, ForbiddenException, NotFoundException,
            ServerErrorException, UnexpectedStatusException, IOException {
        PagedRequestDto<T> dto = null;

        if (cacheRequest) {
            cacheReqeuest(request, type, query);
        }

        try (Response response = client.newCall(request).execute()) {
            int responseCode = response.code();
            switch (responseCode) {
                case 200:
                    logger.info("Request successful parsing body");
                    String body = response.body().string();
                    dto = mapper.readValue(body,
                            mapper.getTypeFactory().constructParametricType(PagedRequestDto.class, EpubDto.class));
                    break;
                case 204:
                    logger.info("No content");
                    return null;
                case 400:
                    logger.info("Bad request");
                    throw new BadRequestException();
                case 401:
                    logger.info("Session expired");
                    throw new AuthorizationException();
                case 403:
                    logger.info("Action forbidden");
                    throw new ForbiddenException();
                case 404:
                    logger.info("Not found");
                    throw new NotFoundException();
                case 500:
                    logger.info("Server error");
                    throw new ServerErrorException();
                default:
                    logger.info("Unexpected status received");
                    throw new UnexpectedStatusException();
            }
        } catch (IOException e) {
            logger.info("Exception occurred during api call", e);
            throw e;
        }

        return dto;
    }

    // #region redo request
    /**
     * Redoes exactly the request associated with given request guid
     * 
     * @param requestGuid
     * @return
     * @throws AuthorizationException
     * @throws BadRequestException
     * @throws ForbiddenException
     * @throws NotFoundException
     * @throws ServerErrorException
     * @throws UnexpectedStatusException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public Object redoRequest(@NotNull String requestGuid) throws AuthorizationException, BadRequestException,
            ForbiddenException, NotFoundException, ServerErrorException, UnexpectedStatusException, IOException,
            IllegalArgumentException {
        if (requestGuid == null || requestGuid.isBlank()) {
            logger.info("No request guid given");
            throw new IllegalArgumentException();
        }
        RequestCacheEntity e = ((RequestCache) caches.get(CacheType.REQUEST)).getValue(requestGuid);
        if (e == null) {
            return null;
        }
        return executeRequest(e.getRequest(), e.getExpectedResponse(), e.getQuery(), false);
    }

    // #region next page
    /**
     * Executes the cached request for the given request guid and gets the next
     * page. Only works for paged requests.
     * 
     * @param <T>                  Expected response type
     * @param requestGuid          Guid of the request for which to get the next
     *                             page. Last request guid can be retrieved with
     *                             {@link EpubClient#getLastRequestGuid()}.
     * @param expectedResponseType Type of the expected response
     * @return Response from the api or null, if the request is not paged.
     * @throws AuthorizationException    Thrown if session has expired
     * @throws BadRequestException       Thrown if server returned 400
     * @throws ForbiddenException        Thrown if server returned 403
     * @throws NotFoundException         Thrown if server returned 404
     * @throws ServerErrorException      Thrown if server returned 500
     * @throws UnexpectedStatusException Thrown if server returned a status, that
     *                                   was not expected by the client.
     * @throws IOException               If a network error occurred
     */
    public @Nullable <T> PagedRequestDto<T> nextPage(@NotNull String requestGuid,
            @NotNull Class<T> expectedResponseType)
            throws AuthorizationException, BadRequestException, ForbiddenException, NotFoundException,
            ServerErrorException, UnexpectedStatusException, IOException {
        RequestCacheEntity e = ((RequestCache) caches.get(CacheType.REQUEST)).getValue(requestGuid);
        if (!e.isPaged()) {
            logger.info("Trying to get next page for a non-paged request");
            return null;
        }
        HttpQuery q = e.getQuery();
        Integer page = q.get("page", Integer.class);
        page++;
        q.overwrite("page", page);
        return executeRequestPaged(
                e.getRequest().newBuilder().url(e.getBaseUrl() + q.toQueryString()).get().build(), expectedResponseType,
                q, true);
    }

    // #region cache request
    /**
     * Caches the given request.
     * 
     * @param <T>              Type of body expected from the request
     * @param request          Request to be cached
     * @param expectedResponse Expected response type
     * @param query            Http query
     * @return Request guid
     */
    private String cacheReqeuest(@NotNull Request request, @NotNull Class<?> expectedResponse,
            @Nullable HttpQuery query) {
        String baseUrl = request.url().toString();
        RequestCacheEntity entity = new RequestCacheEntity();

        if (baseUrl.indexOf("?") != -1) {
            logger.info("Removing query from url");
            baseUrl = baseUrl.substring(0, baseUrl.indexOf("?"));
        }

        entity.setBaseUrl(baseUrl);
        entity.setQuery(query);
        entity.setRequestGuid(UUID.randomUUID().toString());
        entity.setExpectedResponse(expectedResponse);
        entity.setRequest(request);

        if (query != null) {
            Object page = query.get("page");
            if (page != null) {
                entity.setPage((Integer) page);
                entity.setPaged(true);
                entity.setPageSize(query.exists("page_size") ? query.get("page_size", Integer.class) : 1000);
            }
        }

        cacheValue(CacheType.REQUEST, entity.getRequestGuid(), entity);
        return entity.getRequestGuid();
    }

    // #region get last request guid
    /**
     * Returns guid of the last executed reqeuest.
     * 
     * @return Guid of the last executed request
     */
    public String getLastRequestGuid() {
        return ((RequestCache) caches.get(CacheType.REQUEST)).getNewest().getRequestGuid();
    }

    // #region client
    /**
     * Returns the http client of the current epub client. Used by the adapters so
     * every adapter uses the exakt same client.
     * 
     * @return Http client
     */
    protected final OkHttpClient client() {
        return this.client;
    }

    // #region url
    /**
     * Returns base url of the epub api
     * 
     * @return base url of the epub api
     */
    protected final String url() {
        return this.baseUrl;
    }
}
