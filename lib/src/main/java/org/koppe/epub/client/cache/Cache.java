package org.koppe.epub.client.cache;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.koppe.epub.client.exceptions.CachingException;

public interface Cache<K, V> {
    /**
     * Returns the cached value associated with the given key
     * 
     * @param key Key to retrieve value for
     * @return Cached value for given key or null, if no value is present for given
     *         key.
     */
    public V getValue(K key);

    /**
     * Returns the cached value associated with the given key. If refresh is set to
     * true and the value has expired or is null, the refresh function should be
     * called to automatically refresh the value.
     * 
     * @param key     Key to retrieve value for.
     * @param refresh If set to true, the value is automatically refreshed if
     *                necessary.
     * @return Cached value for given key or null, if no value is present for given
     *         key.
     */
    public V getValue(K key, boolean refresh);

    /**
     * Adds new value for given key to the cache.
     * 
     * @param key   Key the value should be cached for
     * @param value Value to be cached.
     * @throws CachingException Can have many different reasons, depending on the
     *                          implementation. Most common reason is that the
     *                          automatic refresh failed because the provided
     *                          refresh function threw an exception.
     */
    public void setValue(K key, V value) throws CachingException;

    /**
     * If true is given, cache will auto refresh, if a refresh function has been set
     * with {@link Cache#refreshFunction(RefreshFunction)}. If no function has been
     * set, no auto refresh will occur, even after time has elapsed.
     * 
     * @param time Time after whcih a value should be refreshed.
     * @param tu   Time unit for the time to have elapsed before refresh.
     */
    public void setRetention(long time, TimeUnit tu);

    /**
     * Function to refresh a cached value with a given key K
     * 
     * @param func Function to refresh the value with
     */
    public void refreshFunction(RefreshFunction<K, V> func);

    /**
     * Sets maximum number of elements in the cache. If the number has been exceded,
     * the oldest element will be deleted. If no number has been set or number is
     * -1, no deletion will occur.
     * 
     * @param max Maximum number of elements to be held in the cache.
     */
    public void setMaxElements(int max);

    /**
     * Removes element with given key from cache and returns the removed element.
     * 
     * @param key Key of the element to be removed.
     * @return Removed element.
     * @throws CachingException If something happens during the removal, e.g. two
     *                          threads accessing the same cache and modifiying it.
     */
    public V removeFromCache(K key) throws CachingException;

    public Map<K, V> getAll();

    /**
     * Functional interface. Provides the means for a cache to auto refresh.
     * <br/>
     * Example Usage:
     * <br/>
     * 
     * <pre>
     * {@code
     * Cache<String, String> cache = new CacheImplementation<>();
     * cache.refreshFunction((string) -> return "Hello " + string);
     * }
     * </pre>
     * 
     * <br/>
     */
    @FunctionalInterface
    public static interface RefreshFunction<K, V> {
        /**
         * The actual implementation of the functional interface
         * 
         * @param key Key to be refreshed
         * @return Retrieved value that should be cached
         * @throws Exception If the execution failed.
         */
        public V run(K key) throws Exception;
    }
}
