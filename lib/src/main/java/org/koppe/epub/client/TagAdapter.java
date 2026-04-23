package org.koppe.epub.client;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.koppe.epub.client.cache.CacheType;
import org.koppe.epub.client.dto.TagDto;
import org.koppe.epub.client.exceptions.ApiCallException;
import org.koppe.epub.client.exceptions.AuthorizationException;
import org.koppe.epub.client.exceptions.BadRequestException;
import org.koppe.epub.client.exceptions.ForbiddenException;
import org.koppe.epub.client.exceptions.NotFoundException;
import org.koppe.epub.client.exceptions.ServerErrorException;
import org.koppe.epub.client.exceptions.UnexpectedStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import okhttp3.Request;
import okhttp3.RequestBody;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
class TagAdapter {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(TagAdapter.class);
    /**
     * Client for calling the epub api
     */
    private final EpubClient client;
    /**
     * Object mapper
     */
    private final ObjectMapper mapper = new ObjectMapper();

    // #region create tags
    /**
     * Creates a new tag with given name and colour in the system. Colour must not
     * be given.
     * 
     * @param jwt    Jwt to authorize at the api with.
     * @param name   Name of the new tag
     * @param colour Colour of the new tag
     * @return The newly created tag or null, if a tag with that name already
     *         exists.
     * @throws IllegalArgumentException If no jwt or name are given
     * @throws BadRequestException      If the transferred body is malformed
     * @throws ApiCallException         General wrapper for all unexpected api
     *                                  status
     * @throws AuthorizationException   If authorization at the api failed.
     */
    protected @Nullable TagDto createTag(@NotNull String jwt, @NotNull String name, @Nullable String colour)
            throws IllegalArgumentException, BadRequestException, ApiCallException, AuthorizationException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("No jwt given");
        }

        if (name == null || name.isBlank()) {
            logger.info("No name for new tag given");
            throw new IllegalArgumentException("No name for new tag given");
        }

        if (colour == null || colour.isBlank())
            colour = "blank";

        TagDto dto = new TagDto(null, name, colour);
        logger.info("createTag() called with tag {}", dto);

        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/tags", client.url()))
                .post(RequestBody.create(mapper.writeValueAsString(dto), EpubClient.APPLICATION_JSON));

        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);
        logger.debug("Built request to create tag, executing");

        TagDto response = null;
        try {
            response = client.executeRequest(builder.build(), TagDto.class, null, false);
        } catch (NotFoundException | ServerErrorException | UnexpectedStatusException | IOException e) {
            throw new ApiCallException("Unexpected response from api", e);
        } catch (ForbiddenException ex) {
            logger.info("Name for new tag already exists");
            return null;
        } catch (BadRequestException ex) {
            logger.info("Invalid tag definition given");
            throw ex;
        } catch (AuthorizationException ex) {
            logger.info("Authorization at the api failed.");
            throw ex;
        }

        if (response == null) {
            logger.info("Could not get a response from the api");
            return null;
        }
        logger.info("Created tag {}", response);

        client.cacheValue(CacheType.TAGS, response.getId(), response);
        return response;
    }

    // #region delete tag
    /**
     * Deletes tag with given id from the system and returns the deleted tag as
     * confirmation. If no tag with that id exists, null is returned.
     * 
     * @param jwt   Jwt to authenticate at the api with
     * @param tagId Id of the tag to be deleted
     * @return The deleted tag or null, if no tag with that id exists
     * @throws IllegalArgumentException If no jwt is given
     * @throws AuthorizationException   If authorization at the api fails
     * @throws ApiCallException         General wrapper for all unexpected
     *                                  exceptions
     */
    protected @Nullable TagDto deleteTag(@NotNull String jwt, long tagId)
            throws IllegalArgumentException, AuthorizationException, ApiCallException {
        if (jwt == null || jwt.isBlank()) {
            logger.info("No jwt given");
            throw new IllegalArgumentException("No jwt given");
        }

        logger.info("Trying to delete tag with id {}", tagId);
        Request.Builder builder = new Request.Builder()
                .url(String.format("%s/tags/%s", client.url(), "" + tagId))
                .delete();

        client.addHeaders(builder, jwt, EpubClient.APPLICATION_JSON_STRING, EpubClient.APPLICATION_JSON_STRING);
        logger.debug("Initialised request, executing");

        TagDto dto = null;

        try {
            dto = client.executeRequest(builder.build(), TagDto.class, null, false);
        } catch (BadRequestException | ForbiddenException | ServerErrorException | UnexpectedStatusException
                | IOException e) {
            logger.info("Unexpected return from api", e);
            throw new ApiCallException(null, e);
        } catch (AuthorizationException ex) {
            logger.info("Could not authenticate at api");
            throw ex;
        } catch (NotFoundException ex) {
            logger.info("Tag with given id does not exist");
            return null;
        }

        if (dto == null) {
            logger.info("No dto returned from api");
            return null;
        }
        logger.info("Successfully deleted tag {}", dto);
        client.removeFromCache(CacheType.TAGS, (Long) tagId);

        return dto;
    }

}
