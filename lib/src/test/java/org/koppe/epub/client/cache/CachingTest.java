package org.koppe.epub.client.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.exceptions.CachingException;

public class CachingTest {

    private static EpubDto dto1;

    @BeforeAll
    public static void setup() {
        dto1 = new EpubDto();
        dto1.setTitle("Hello World");
    }

    @Test
    public void testAddValue() throws InterruptedException {
        EpubCache cache = new EpubCache();
        cache.setMaxElements(1);

        assertThrows(CachingException.class, () -> cache.setValue(null, dto1));
        assertThrows(CachingException.class, () -> cache.setValue(null, null));
        assertThrows(CachingException.class, () -> cache.setValue(1L, null));

        try {
            cache.setValue(1L, dto1);
        } catch (CachingException ex) {
            fail();
        }
        assertNull(cache.getValue(2L));

        EpubDto cached = cache.getValue(1L);
        assertNotNull(cached);
        assertEquals("Hello World", cached.getTitle());

        try {
            cache.setValue(2L, dto1);
        } catch (CachingException ex) {
            fail();
        }

        Thread.sleep(2000);
        cached = cache.getValue(2L);
        assertNotNull(cached);
        assertNull(cache.getValue(1L, false));
        assertEquals("Hello World", cached.getTitle());
        assertEquals(1, cache.getAll().entrySet().size());
    }

    @Test
    public void testRefresh() throws InterruptedException {
        TestCache test = new TestCache();
        test.setRetention(1, TimeUnit.SECONDS);

        try {
            test.setValue("World", "Hello");
        } catch (Exception ex) {
            fail();
        }

        Thread.sleep(2000);
        assertNull(test.getValue("World"));

        try {
            test.setValue("World", "Hello");
        } catch (Exception ex) {
            fail();
        }
        test.refreshFunction((key) -> "Hello " + key);
        assertEquals("Hello", test.getValue("World"));
        Thread.sleep(2000);
        assertEquals("Hello World", test.getValue("World"));
    }

    @Test
    public void testRemoveValue() {
        TestCache test = new TestCache();

        try {
            test.setValue("Hello", "World");
        } catch (Exception ex) {
            fail();
        }

        try {
            test.removeFromCache("Hello");
        } catch (CachingException e) {
            fail();
        }

        assertNull(test.getValue("Hello"));
    }

    @Test
    public void testElementEjection() throws InterruptedException {
        var test = new TestCache();

        test.setMaxElements(1);

        try {
            test.setValue("1", "1");
            test.setValue("2", "2");
        } catch (CachingException e) {
            fail();
        }

        // Sleep because ejection is done in a different thread
        Thread.sleep(1000);
        assertNull(test.getValue("1"));
        assertNotNull(test.getValue("2"));
        assertEquals("2", test.getValue("2"));
    }

    @Test
    public void testGetAll() {
        var test = new TestCache();

        try {
            test.setValue("1", "2");
            test.setValue("2", "2");
        } catch (CachingException e) {
            fail();
        }

        var all = test.getAll();

        assertEquals(2, all.entrySet().size());
    }

    @Test
    public void testGetOldestAndNewest() {
        var test = new TestCache();

        try {
            test.setValue("1", "1");
            test.setValue("2", "2");
        } catch (CachingException e) {
            fail();
        }

        assertEquals("1", test.getOldest());
        assertEquals("2", test.getNewest());
    }

    private static class TestCache extends AbstractCache<String, String> {

    }
}
