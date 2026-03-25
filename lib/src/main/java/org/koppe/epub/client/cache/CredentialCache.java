package org.koppe.epub.client.cache;

import org.koppe.epub.client.EpubClient;
import org.koppe.epub.client.dto.CredentialDto;
import org.koppe.epub.client.exceptions.CachingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * {@link Cache} implementation for caching credentials
 */
public class CredentialCache extends AbstractCache<String, String> {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(CredentialCache.class);
    /**
     * Client used for refreshing cached values
     */
    private final EpubClient client;

    /**
     * Function for refreshing cache values
     */
    private final RefreshFunction<String, String> refresh = (String key) -> {
        if (CredentialCacheKeys.USER.value.equals(key) || CredentialCacheKeys.PASSWORD.value.equals(key)) {
            logger.info("Cannot refresh user or password, needs to be set manually");
            return null;
        }

        logger.info("Refreshing cache value for key {}", key);

        String user = getValue(CredentialCacheKeys.USER, false);
        String password = getValue(CredentialCacheKeys.PASSWORD, false);
        String refresh = getValue(CredentialCacheKeys.REFRESH, false);

        if (user == null || password == null) {
            logger.info("User or password is null, cannot refresh jwt or refresh token");
            return null;
        }

        if (CredentialCacheKeys.JWT.value.equals(key)) {
            if (refresh != null) {
                // TODO implement refresh in epub client
            } else {
                CredentialDto dto = this.login(user, password);
                if (dto == null) {
                    logger.info("Could not retrieve new login information");
                    return null;
                }
                return dto.getJwt();
            }
        }

        if (CredentialCacheKeys.REFRESH.value.equals(key)) {
            CredentialDto dto = this.login(user, password);
            if (dto == null) {
                logger.info("Could not retrieve new login information");
                return null;
            }
            return dto.getRefresh();
        }

        return null;
    };

    /**
     * Default constructor
     * 
     * @param client Client for refreshing the cache.
     */
    public CredentialCache(EpubClient client) {
        this.client = client;
        refreshFunction(refresh);
    }

    public String getValue(CredentialCacheKeys key) {
        return getValue(key.getValue(), true);
    }

    public String getValue(CredentialCacheKeys key, boolean refresh) {
        return super.getValue(key.getValue(), refresh);
    }

    public void setValue(CredentialCacheKeys key, String value) throws CachingException {
        super.setValue(key.getValue(), value);
    }

    private CredentialDto login(String username, String password) {
        try {
            return this.client.login(username, password);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 
     */
    @Getter
    @RequiredArgsConstructor
    public static enum CredentialCacheKeys {
        JWT("jwt"),
        REFRESH("refresh"),
        USER("user"),
        PASSWORD("password");

        private final String value;
    }
}
