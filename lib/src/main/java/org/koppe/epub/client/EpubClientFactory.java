package org.koppe.epub.client;

import org.koppe.epub.client.cache.Cache;
import org.koppe.epub.client.cache.CacheFactory;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.cache.CredentialCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides factory methods for creating Epub Clients
 */
public final class EpubClientFactory {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(EpubClientFactory.class);

    /**
     * Creates a default {@link EpubClient} instance with the following
     * specifications:
     * - Credentials are not cached.
     * - Logout happens after every single query
     * - Does not trust all certs
     * 
     * @param baseUrl Base URL of the epub lib api
     * @return Default Epub Client instance
     */
    public static final EpubClient newDefaultClient(String baseUrl) {
        logger.info("Initializing new default epub client instance");
        EpubClient client = new EpubClient(baseUrl);
        return client;
    }

    /**
     * Creates a new instance of {@link EpubClient} that trusts all ssl
     * certificates.
     * 
     * @param baseUrl Base url of the epub library api
     * @return Client instance
     */
    public static final EpubClient newTrustingClient(String baseUrl) {
        return new EpubClient(baseUrl, true);
    }

    /**
     * Creates a new epub client that caches credentials. Calls
     * {@link EpubClientFactory#newDefaultClient(String)} and modifies the returned
     * entity.
     * 
     * @param baseUrl Base url of the epub lib api.
     * @return New Epub Client instance that caches credentials.
     */
    public static final EpubClient newCredentialCacheClient(String baseUrl) {
        EpubClient client = newDefaultClient(baseUrl);
        CredentialCache cache = CacheFactory.newDefaultCredentialCache(client);
        client.registerCache(CacheType.CREDENTIALS, cache, true);
        return client;
    }

    /**
     * Creates a new {@link EpubClient} instance that has caches registered for
     * every {@link CacheType} given in second parameter.
     * 
     * @param baseUrl    Base url of the epub lib api
     * @param cacheTypes All types of caches to be initialised
     * @return
     */
    public static final EpubClient newCachingClient(String baseUrl, CacheType[] cacheTypes) {
        logger.info("Initialising a new caching epub client for cache types {}", (Object[]) cacheTypes);
        EpubClient client = newDefaultClient(baseUrl);

        for (CacheType t : cacheTypes) {
            Cache<?, ?> cache = CacheFactory.newDefaultCacheForType(t, client);
            client.registerCache(t, cache, true);
        }

        return client;
    }

}
