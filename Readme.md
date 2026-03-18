# Overview

**This is an ongoing project and not yet finished**

This readme is an ongoing project too. Still working on it.

Client library for working with the rest api of the Epub Library in [this repository](https://github.com/GeKoppe/epub_library-api).

# Features

Provides convenience functionality to query every endpoint in the Epub Library.

# Using it

## Creating a client

Use the provided factory methods to initialise a new client.

```Java
EpubClient client = EpubClientFactory.newDefaultClient("<url of your epub library api>");
```

There are also other factory methods for different kinds of clients

## Caching credentials

Every entity queriable in the api is cacheable. Credentials are not special in this regard, though they are special in the way that caching them is highly recommended.

The Epub Library API works with JSON Web Tokens. Those tokens expire after some time, not reusing them creates a lot of unnecessary traffic, as every operation needs a new login though.

To make caching easier, the `EpubClient` does it automatically, if you configure it that way.

The simples way to create a client that caches credentials is using the factory method `EpubClientFactory.newCredentialCacheClient(String)` and saving your credentials to the cache:

```Java
EpubClient client = EpubClientFactory.newCredentialCacheClient("<url>");
client.cacheValue(CacheType.CREDENTIALS, CredentialCacheKeys.USER, "username");
client.cacheValue(CacheType.CREDENTIALS, CredentialCacheKeys.PASSWORD, "pw");
```

Afterwards every operation will be automatically authenticated against the api.

## Querying

For every endpoint, there are two convenience methods in the EpubClient class to query said endpoint. One for clients that cache credentials, the other one for creating a new session every time. It is recommended to use a client that caches credentials in order to not create too many json web tokens.

### Example of getting an entity

Querying an entity is as simple as calling it's respective `.get` method in the `EpubClient`. This example will demonstrate that with an Epub, it works the same with every other entity though.

To get an epub for a specified id, you can just call `EpubClient.getEpub(long, HttpQuery)`. Depending on whether your client caches credentials or not, you can also call `EpubClient.getEpub(String, String, long, HttpQuery)` and provide username and password.

The `HttpQuery` parameter is used to define what parts of the given entity is returned (e.g. just the basics; authors; genres etc.). For every entity, a builder class for Http queries exist to simplify the filtering.

This is an example for getting the epub with id 1, including all authors and genres, with a client that does not cache the credentials:

```Java
EpubClient client = EpubClientFactory.newDefaultClient("<url>");

HttpQuery query = new EpubQueryBuilder()
    .withAuthors(true)
    .withGenres(true)
    .build();

try {
    // Contains epub info, authors and genres
    EpubDto epub = client.getEpub("user", "password", 1L, query);
} catch (Exception ex) {
    // If the api call fails, an exception representing the reason is thrown
}
```

## Caching

Caching has been discussed in the chapter [Caching Credentials](#caching-credentials). Other types of entities might be cached as well though. This will again be demonstrated with epubs but works the same with every other entity too.

If you just want a default cache (15 minute retention of entities, no refresh except for credentials), just use the provided factory method:

```Java
EpubClient cachingClient = EpubClientFactory.newCachingClient("url", new CacheType[]{ CacheType.EPUBS, CacheType.CREDENTIALS });
```

Before and after every call, the client will check the corresponding cache to see, whether an entity already exists, needs to be updated or deleted.

If you want custom caches in your client, you can also register a new cache manually:

```Java
EpubClient client = EpubClientFactory.newDefaultClient("url");

EpubCache cache = new EpubCache();
cache.setMaxElements(10); // Set maximum number of elements in cache
cache.setRetention(10L, TimeUnit.MINUTES); // Set time after which elements are ejected or refreshed
cache.refreshFunction((key) -> {
    // Some custom refresh logic
});

client.registerCache(CacheType.EPUBS, cache);
```

If you now do an epub operation, the client will use the cache you supplied.