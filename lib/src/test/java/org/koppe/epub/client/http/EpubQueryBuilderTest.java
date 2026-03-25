package org.koppe.epub.client.http;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class EpubQueryBuilderTest {

    @Test
    public void test() {
        var b = new EpubQueryBuilder()
                .publishedAfter(LocalDate.of(2000, 1, 1))
                .publishedBefore(LocalDate.of(2000, 1, 1))
                .titleContains("test")
                .uploadedAfter(LocalDate.of(2000, 1, 1))
                .uploadedBefore(LocalDate.of(2000, 1, 1))
                .withTableOfContents(true)
                .withEditions(true)
                .withAuthors(true)
                .withGenres(true)
                .withMetadata(true)
                .withSeries(true)
                .withTags(true);

        String expected = "?published_after=2000-01-01&published_before=2000-01-01&title_contains=test&uploaded_after=2000-01-01&uploaded_before=2000-01-01&with_toc=true&with_editions=true"
                + "&with_authors=true&with_genres=true&with_metadata=true&with_series=true&with_tags=true";
        assertEquals(expected, b.build().toQueryString());
    }
}
