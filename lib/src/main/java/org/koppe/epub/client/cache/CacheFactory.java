package org.koppe.epub.client.cache;

import java.util.concurrent.TimeUnit;

import org.koppe.epub.client.EpubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static EpubCache newDefaultEpubCache() {
        return new EpubCache();
    }

    /**
     * Creates a new default cache instance for the given type. Calls the respective
     * default factory method, e.g. for {@link CacheType#CREDENTIALS} the method
     * {@link CacheFactory#newDefaultCredentialCache(EpubClient)} is called to
     * initialise the cache.
     * 
     * @param type   Type of cache to be created
     * @param client Client the caches use for auto refreshment.
     * @return The initialised cache instance.
     */
    public static Cache<?, ?> newDefaultCacheForType(CacheType type, EpubClient client) {
        logger.info("Creating new default cache instance for type {}", type);
        switch (type) {
            case CREDENTIALS:
                return newDefaultCredentialCache(client);
            case EPUBS:
                return newDefaultEpubCache();
            default:
                return null;
        }
    }
}
