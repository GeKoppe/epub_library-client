package org.koppe.epub.client.cache;

import java.util.concurrent.TimeUnit;

import org.koppe.epub.client.EpubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class that provides static methods to initialise caches for different
 * entities.
 */
public abstract class CacheFactory {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    /**
     * Creates a default {@link CredentialCache} instance that uses the given client
     * for refreshment of JWT und refresh tokens.
     * Returned cache auto refreshes entries after 15 minutes and holds max. 4
     * elements (username, password, jwt and refresh).
     * 
     * @param client Client for the credential cache to use.
     * @return The initialized credential cache
     */
    public static CredentialCache newDefaultCredentialCache(EpubClient client) {
        logger.info("Initialising default credential cache");
        CredentialCache cache = new CredentialCache(client);
        cache.setRetention(15, TimeUnit.MINUTES);
        cache.setMaxElements(4);

        return cache;
    }

    /**
     * Initialises new default cache for epubs. 15 minutes of retention,
     * maximum of 100 elements, no automatic refresh.
     * 
     * @return The initialised cache
     */
    public static EpubCache newDefaultEpubCache() {
        EpubCache cache = new EpubCache();
        cache.setMaxElements(100);
        cache.setRetention(15, TimeUnit.MINUTES);
        return cache;
    }

    /**
     * Initialises new default cache for epub editions. 15 minutes of retention,
     * maximum of 100 elements, no automatic refresh.
     * 
     * @return The initialised cache
     */
    public static EditionCache newDefaultEditionCache() {
        EditionCache cache = new EditionCache();
        cache.setMaxElements(100);
        cache.setRetention(15, TimeUnit.MINUTES);
        return cache;
    }

    /**
     * Creates a new default cache instance for the given type. Calls the respective
     * default factory method, e.g. for {@link CacheType#CREDENTIALS} the method
     * {@link CacheFactory#newDefaultCredentialCache(EpubClient)} is called to
     * initialise the cache.
     * 
     * @param type   Type of cache to be created
     * @param client Client the caches use for auto refreshment. At the moment this
     *               is only required for the credential cache.
     * @return The initialised cache instance.
     */
    public static Cache<?, ?> newDefaultCacheForType(CacheType type, EpubClient client) {
        logger.info("Creating new default cache instance for type {}", type);
        return switch (type) {
            case CREDENTIALS -> newDefaultCredentialCache(client);
            case EPUBS -> newDefaultEpubCache();
            case EDITIONS -> newDefaultEditionCache();
            case AUTHORS -> newDefaultAuthorCache();
            default -> null;
        };
    }

    // #region default author cache
    /**
     * Creates a default cache for AuthorDtos.
     * Retention: 10 minutes.
     * Maximum elements: 100.
     * No automatic refresh.
     * 
     * @return The created cache.
     */
    public static AuthorCache newDefaultAuthorCache() {
        AuthorCache cache = new AuthorCache();
        cache.setRetention(10, TimeUnit.MINUTES);
        cache.setMaxElements(100);

        return cache;
    }
}
