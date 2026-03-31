package org.koppe.epub.client;

import java.time.LocalDate;
import java.util.UUID;

import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.dto.CredentialDto;
import org.koppe.epub.client.dto.EpubDto;
import org.koppe.epub.client.dto.EpubEditionDto;
import org.koppe.epub.client.dto.GenreDto;

import lombok.Getter;

@Getter
class DtoRecord {
	/**
	 * JWT
	 */
	static final CredentialDto jwt = new CredentialDto("admin", "fake-jwt-123", "fake-refresh-123");

	/** EPUBS */
	static final EpubDto epub1 = new EpubDto(1L, "epub 1", LocalDate.of(2000, 1, 1), LocalDate.of(2020, 1, 1), null,
			null, null, null, null);
	static final EpubDto epub2 = new EpubDto(2L, "epub 2", LocalDate.of(2000, 1, 1), LocalDate.of(2020, 1, 1), null,
			null, null, null, null);

	static final EpubEditionDto edition1 = new EpubEditionDto(1L, "edition 1", 1, UUID.randomUUID().toString(),
			UUID.randomUUID().toString(), null);

	static final AuthorDto author1 = new AuthorDto(1L, "Test", "Test", LocalDate.of(2000, 1, 1),
			LocalDate.of(2000, 1, 1), "Test author", null, null, null);

	static final AuthorDto author2 = new AuthorDto(2L, "Test2", "Test2", LocalDate.of(2000, 1, 1),
			LocalDate.of(2000, 1, 1), "Test author 2", null, null, null);

	static final GenreDto genre1 = new GenreDto(1L);

	static final GenreDto genre2 = new GenreDto(2L);
}
