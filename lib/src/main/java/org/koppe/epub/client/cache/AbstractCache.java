package org.koppe.epub.client.cache;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.koppe.epub.client.exceptions.CachingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * Basic implementation of {@link Cache}. Grants basic functionality and can be
 * extended, if no special implementation is needed.
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Map that holds all cached values
     */
    private final Map<K, CachedValue<V>> cache = new HashMap<>();
    /**
     * Lock for the cache map
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * Function to refresh certain cached values
     */
    private RefreshFunction<K, V> refreshFunction;
    /**
     * Time after which a value automatically refreshes
     */
    private long autoRefreshTime = -1;
    /**
     * Time unit in which the auto refresh time is measured
     */
    private TimeUnit autoRefreshTimeUnit = TimeUnit.MINUTES;
    /**
     * Maximum number of elements
     */
    private int maxElements = -1;

    /**
     * Calls {@link RefreshFunction#run(Object)} of the instances refresh function
     * to update the given key in the cache. If no refresh function exists, null is
     * returned automatically.
     * 
     * @param key Key to be refreshed
     * @return The updated cached value
     */
    private final CachedValue<V> refresh(K key) {
        V newVal = null;
        try {
            if (refreshFunction == null) {
                return null;
            } else {
                newVal = refreshFunction.run(key);
            }
        } catch (Exception ex) {
            logger.info("Exception occurred during refresh of key {}", key);
            return null;
        }

        if (newVal == null) {
            logger.info("No value for key {} could be refreshed", key);
            return null;
        }

        try {
            setValue(key, newVal);
        } catch (CachingException ex) {
            return null;
        }

        return cache.get(key);
    }

    /**
     * Checks if a cached value is expired. Returns true if it is.
     * 
     * @param val Value to be checked
     * @return True, if the value is expired, false otherwise
     */
    private final boolean isExpired(CachedValue<V> val) {
        if (autoRefreshTime <= 0 || autoRefreshTimeUnit == null) {
            return false;
        }
        switch (autoRefreshTimeUnit) {
            // Smallest supported unit is seconds
            case MICROSECONDS, NANOSECONDS, MILLISECONDS, SECONDS:
                return val.getAddedAt().plusSeconds(autoRefreshTime).isBefore(LocalDateTime.now());
            case MINUTES:
                return val.getAddedAt().plusMinutes(autoRefreshTime).isBefore(LocalDateTime.now());
            case HOURS:
                return val.getAddedAt().plusHours(autoRefreshTime).isBefore(LocalDateTime.now());
            case DAYS:
                return val.getAddedAt().plusDays(autoRefreshTime).isBefore(LocalDateTime.now());
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getValue(K key, boolean refresh) {
        if (!acquireLock()) {
            return null;
        }

        CachedValue<V> val = cache.get(key);
        lock.unlock();

        if (val == null) {
            if (refresh) {
                val = refresh(key);
                if (val == null) {
                    try {
                        removeFromCache(key);
                    } catch (Exception ex) {
                        logger.info("Could not remove value from cache", ex);
                    }
                }
            }
            if (val == null) {
                return null;
            }
        }

        if (isExpired(val)) {
            if (refresh) {
                val = refresh(key);
                if (val == null) {
                    try {
                        removeFromCache(key);
                    } catch (Exception ex) {
                        logger.info("Could not remove value from cache", ex);
                    }
                }
            }
            if (val == null) {
                return null;
            }
        }

        return val.getValue();
    }

    /**
     * {@inheritDoc}
     * Calls {@link AbstractCache#getValue(Object, boolean)} and sets refresh to
     * true.
     */
    @Override
    public V getValue(K key) {
        return getValue(key, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshFunction(RefreshFunction<K, V> func) {
        this.refreshFunction = func;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRetention(long time, TimeUnit tu) {
        this.autoRefreshTime = time;
        this.autoRefreshTimeUnit = tu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxElements(int max) {
        this.maxElements = max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(K key, V value) throws CachingException {
        if (key == null || value == null) {
            throw new CachingException("Key or value is null", null);
        }
        if (!acquireLock()) {
            throw new CachingException("Could not acquire lock on cache", null);
        }

        CachedValue<V> newVal = new CachedValue<>();
        newVal.value = value;
        newVal.addedAt = LocalDateTime.now();
        cache.put(key, newVal);
        lock.unlock();

        if (maxElements >= 0 && cache.size() > maxElements) {
            Executors.newFixedThreadPool(1).submit(this::removeOldest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V removeFromCache(K key) throws CachingException {
        CachedValue<V> cached = cache.get(key);
        if (cached == null) {
            return null;
        }

        if (!acquireLock()) {
            throw new CachingException("Could not acquire lock on cache", null);
        }
        cache.remove(key);
        lock.unlock();
        return cached.getValue();
    }

    /**
     * Removes the oldest item from the cache.
     */
    private final void removeOldest() {
        K oldest = null;
        if (!acquireLock()) {
            return;
        }

        for (var x : cache.keySet()) {
            if (oldest == null) {
                oldest = x;
                continue;
            }

            if (cache.get(x).getAddedAt().isBefore(cache.get(oldest).getAddedAt())) {
                oldest = x;
                continue;
            }
        }
        cache.remove(oldest);
        lock.unlock();
    }

    /**
     * Tries to acquire lock on the cache map.
     * 
     * @return True if lock could be acquired, false otherwise
     */
    private final boolean acquireLock() {
        try {
            if (!lock.tryLock(2000, TimeUnit.MILLISECONDS)) {
                return false;
            }
            return true;
        } catch (InterruptedException ex) {
            return false;
        }
    }

    /**
     * Represents a single cached value. Contains the value as well as the time it
     * was added to the cache.
     */
    @Getter
    @Setter
    private static class CachedValue<V> {
        /**
         * Actual cached value
         */
        private V value;
        /**
         * Time the value has been added
         */
        private LocalDateTime addedAt;
    }

    @Override
    public Map<K, V> getAll() {
        Map<K, V> all = new HashMap<>();
        for (K x : cache.keySet()) {
            all.put(x, cache.get(x).getValue());
        }
        return all;
    }

    public V getNewest() {
        K k = null;
        for (K x : cache.keySet()) {
            if (k == null) {
                k = x;
                continue;
            }
            if (cache.get(x).getAddedAt().isAfter(cache.get(k).getAddedAt())) {
                k = x;
            }
        }

        return cache.get(k).getValue();
    }

    public V getOldest() {
        K k = null;
        for (K x : cache.keySet()) {
            if (k == null) {
                k = x;
                continue;
            }
            if (cache.get(x).getAddedAt().isBefore(cache.get(k).getAddedAt())) {
                k = x;
            }
        }

        return cache.get(k).getValue();
    }
}
