# Overview

**This is an ongoing project and not yet finished**

This readme is an ongoing project too. Still working on it.

Client library for working with the rest api of the Epub Library in [this repository](https://github.com/GeKoppe/epub_library-api).

# Features

Provides convenience functionality to query every endpoint in the Epub Library.

# Using it

See [usage description](/doc/use.md) for detailed examples.

## Gradle

First add the following in the `repositories` block in your `build.gradle`:

```Gradle
repositories {
    maven {
        name = "github"
        url = 'https://maven.pkg.github.com/GeKoppe/epub_library-client'
        credentials {
            username = findProperty('gpr.user') ?: ''
            password = findProperty('gpr.key') ?: ''
    }
}
```

Your GitHub credentials must be stored in the `~/.gradle/settings.gradle` file for this to work.

Then add the dependency:

```Gradle
dependencies {
    implementation 'org.koppe.epub.client:epub-lib-client:<version-number>'
}
```

## Maven

Add the following to your `pom.xml`:

```XML
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/GeKoppe/epub_library-client</url>
    </repository>
</repositories>
```

Your GitHub credentials must be stored in the `~/.m2/settings.xml` file for this to work.

Then add the following dependency:

```XML
<dependencies>
    <dependency>
        <groupId>org.koppe.epub.client</groupId>
        <artifactId>epub-lib-client</artifactId>
        <version>[version-number]</version>
    </dependency>
</dependencies>
```


# Changelog

See [changelog](doc/changelog.md).