# Publishing the Supaship Java SDK

This guide assumes you have **never** published a Java library before. It explains how artifacts get into a **package registry** that Maven and Gradle users can consume, and how to publish **updates** later.

## 1. What you are publishing

A Maven project produces a **JAR** and a **POM** (metadata). Together they are identified by three coordinates:

| Coordinate   | This project (example) | Meaning |
|-------------|-------------------------|---------|
| `groupId`   | `com.supaship`          | Your organization or product namespace (like an npm scope). |
| `artifactId`| `supaship-sdk`          | The library name. |
| `version`   | `1.0.0`                 | Semantic version; **release** versions must not end in `-SNAPSHOT`. |

**Snapshot** versions (for example `1.0.0-SNAPSHOT`) are mutable development builds. **Release** versions (for example `1.0.0`) are immutable once on Maven Central.

The **de facto** public registry for open source Java is **Maven Central** (synced from Sonatype). Alternatives include **GitHub Packages**, **JitPack**, or a private Nexus; this doc focuses on **Maven Central**, which is what most developers expect.

## 2. Prerequisites on your machine

1. **JDK 11+** (this project targets Java 11).
2. **Apache Maven** (3.9+ recommended).
3. **GnuPG** (`gpg`) for **signing** artifacts. Maven Central requires cryptographic signatures for releases.

Install GnuPG and create a key (one time):

```bash
gpg --full-generate-key
# Choose RSA, 4096 bits, expiry as you prefer, use your real name and the email you will register with Sonatype.
gpg --list-secret-keys --keyid-format=long
```

Publish the **public** key to a keyserver (Ubuntu keyserver is commonly used):

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID_LONG
```

## 3. Register a namespace with Sonatype (Maven Central)

Modern flow uses the **Central Portal**: https://central.sonatype.com/

1. **Sign up** and sign in.
2. **Choose a namespace** that matches your `groupId`:
   - **Reverse DNS** you control: for example if you own `supaship.com`, you can use `com.supaship` after proving domain ownership (DNS TXT record they specify).
   - **`io.github.your-org`** if you publish from GitHub and verify the namespace they require for that pattern.

3. **Open a namespace / verify** following the portal wizard (DNS TXT, or GitHub org verification, depending on the namespace type).

Until the namespace is approved, you **cannot** publish under that `groupId` to Maven Central.

> If your current `groupId` is `com.supaship`, you must prove you control a domain that authorizes that group id, **or** change `groupId` in `pom.xml` to a namespace Sonatype assigns you (for example `io.github.supashiphq`). Coordinate this before the first release.

## 4. Credentials in `settings.xml` (one time)

Maven needs a **token** (or username/password) to upload. In the Central Portal, create a **User token** and add a `<server>` entry.

Edit `~/.m2/settings.xml` (create the file if missing). **Do not commit tokens to git.**

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>central</id>
      <username><!-- token username or _json_token key from portal --></username>
      <password><!-- token password or secret --></password>
    </server>
  </servers>
</settings>
```

The `<id>` must match the `<distributionManagement>` repository `<id>` in your `pom.xml` (Sonatype’s docs often use `central` or `ossrh`; follow the exact id from their publishing guide for the plugin you use).

## 5. POM changes for publishing

For Maven Central you typically need:

1. **Project metadata**: `name`, `description`, `url`, `licenses`, `scm`, `developers`.
2. **Source and Javadoc JARs** (consumers and indexes expect them).
3. **GPG signing** of artifacts.
4. **distributionManagement** pointing at Sonatype’s deployment endpoint **or** use the official **Central Publishing** Maven plugin they document for new projects.

Exact plugin coordinates change over time; always cross check https://central.sonatype.org/publish/publish-maven/ for the **current** recommended `pom.xml` fragment.

A minimal **conceptual** checklist:

- `maven-source-plugin` → attaches `-sources.jar`
- `maven-javadoc-plugin` → attaches `-javadoc.jar`
- `maven-gpg-plugin` → signs all attached artifacts
- **Staging / publishing** plugin as per Sonatype (classic OSSRH `nexus-staging-maven-plugin` vs newer Central Publishing)

Add a **profile** `-P release` so day to day `mvn test` does not require GPG.

## 6. First release (high level)

1. Set **release version** in `pom.xml` (remove `-SNAPSHOT`), for example `1.0.0`.
2. Ensure `CHANGES` / git tag strategy is decided (`v1.0.0`).
3. Run locally:

   ```bash
   mvn clean verify
   ```

   With signing profile (example):

   ```bash
   mvn clean verify -P release
   ```

4. **Deploy** (from the repository root; the reactor publishes the parent POM `com.supaship:supaship-sdks` and the library `com.supaship:supaship-sdk`):

   ```bash
   mvn clean deploy -P release
   ```

5. In the **Sonatype portal** (or Nexus UI if legacy): **close** the staging repository, **release** it, wait until artifacts propagate to **Maven Central** (often tens of minutes the first time).

6. Verify in a browser: `https://repo1.maven.org/maven2/com/supaship/supaship-sdk/1.0.0/` (adjust `groupId` path: `com.supaship` → `com/supaship`). The parent POM lives under `https://repo1.maven.org/maven2/com/supaship/supaship-sdks/<version>/`.

## 7. Later releases (updates)

1. **Bump version** in `pom.xml`, for example `1.0.1` or `1.1.0`.
2. Commit and **tag** (`git tag v1.0.1`).
3. Run `mvn clean deploy -P release` again.
4. Close / release staging as before.

**Semantic versioning** (recommended):

- **PATCH** (`1.0.1`): bug fixes, documentation, internal refactors with no API change.
- **MINOR** (`1.1.0`): backward compatible API additions.
- **MAJOR** (`2.0.0`): breaking API changes.

## 8. Snapshots (optional)

If you want public `-SNAPSHOT` builds, configure a **snapshotRepository** in `distributionManagement` and publish snapshot versions. Many teams skip snapshots and only use Git commit hashes plus local `mvn install` for integration.

## 9. Simpler alternative: GitHub Packages

If Maven Central is too heavy for an early preview:

1. Create a **Personal Access Token** with `write:packages`.
2. Add a `<repository>` in `distributionManagement` pointing at  
   `https://maven.pkg.github.com/OWNER/REPO`.
3. In `~/.m2/settings.xml`, add `<server>` with your GitHub username and token.

Consumers must add the same repository block in their `pom.xml` (GitHub Packages are not on the default Central mirror).

## 10. Checklist before announcing a release

- [ ] `mvn test` passes.
- [ ] Version is **not** `-SNAPSHOT` for a public release tag.
- [ ] `LICENSE` file matches `pom.xml` `licenses`.
- [ ] `groupId` is **approved** in Sonatype for your namespace.
- [ ] Javadoc builds (`mvn javadoc:javadoc` or via release profile).
- [ ] You can resolve the artifact from a **fresh** machine or project using only Central (or document extra repositories if using GitHub Packages).

## 11. Where to get help

- Sonatype: https://central.sonatype.org/
- Maven Central status and search: https://central.sonatype.com/

When in doubt, Sonatype’s current **“Publish Maven”** article is the source of truth for XML snippets and plugin versions.
