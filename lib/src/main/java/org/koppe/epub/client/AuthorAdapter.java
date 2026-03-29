package org.koppe.epub.client;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.dto.AuthorDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.SessionExpiredException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Request;
import okhttp3.RequestBody;
import tools.jackson.databind.ObjectMapper;

/**
 * Encapsulates all author related actions.
 */
class AuthorAdapter {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(AuthorAdapter.class);
    /**
     * Client to execute requests
     */
    private final EpubClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Default constructor
     * 
     * @param client Client the adapter is supposed to use to execute requests.
     */
    protected AuthorAdapter(EpubClient client) {
        this.client = client;
    }

    // #region add author
    /**
     * Adds a new author to the api. At leas author.firstName and author.surname
     * need to be filled, everything else is optional.
     * 
     * @param jwt    JWT to authenticate at the api with
     * @param author Author to add to the system.
     * @return The added author
     * @throws IllegalArgumentException If no jwt, author, author.firstName or
     *                                  author.surname is given.
     * @throws ApiCallException         General wrapper for all unexpected api error
     * @throws SessionExpiredException  If the jwt has expired
     * @throws BadRequestException      If the author dto was invalid.
     */
    protected @Nullable AuthorDto addAuthor(@NotNull String jwt, @NotNull AuthorDto author)
            throws IllegalArgumentException, ApiCallException, SessionExpiredException, BadRequestException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("Invalid jwt given");
            throw new IllegalArgumentException("Missing jwt");
        }

        if (author == null || author.getFirstName() == null || author.getFirstName().isBlank()
                || author.getSurname() == null || author.getSurname().isBlank()) {
            logger.info("Invalid author given");
            throw new IllegalArgumentException("Invalid author given, at least first name and surname must be filled");
        }

        logger.info("Building request to add a new author");
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/authors", client.url()))
                .post(RequestBody.create(mapper.writeValueAsString(author), EpubClient.APPLICATION_JSON));

        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);

        logger.info("Successfully initialized request, executing");
        AuthorDto dto = null;
        try {
            dto = client.executeRequest(builder.build(), AuthorDto.class, null, true);
        } catch (SessionExpiredException e) {
            logger.warn("JWT has expired");
            throw e;
        } catch (BadRequestException e) {
            logger.info("Invalid author given");
            throw e;
        } catch (ForbiddenException | NotFoundException | UnexpectedStatusException | ServerErrorException
                | IOException e) {
            logger.warn("Unexpected error occurred during the api call", e);
            throw new ApiCallException(null, e);
        }

        if (dto == null) {
            logger.info("Author could not be created");
            return null;
        }

        client.cacheValue(CacheType.AUTHORS, dto.getId(), dto);
        return dto;
    }

}
