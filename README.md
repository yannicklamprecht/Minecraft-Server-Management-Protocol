# Minecraft Java Edition Management Protocol

Multi-module Java 21 / Gradle project for extracting Minecraft Server Management Protocol (MSMP) OpenRPC schemas and generating typed client bindings.

## Project Structure

- `gradle-plugin/`: Gradle plugin (`com.example.msmp.schema-extractor`) that automatically fetches Minecraft version manifests, downloads server JARs, executes data generation, and extracts OpenRPC schemas into `protocol-schemas/<version>/openrpc.json`.
- `client/`: Java 21 client library providing WebSocket transport and OpenRPC-driven code generation for typed DTOs and API facades.
- `protocol-schemas/`: OpenRPC protocol schemas categorized by management protocol version (e.g., `1.0.0`, `2.0.0`, `3.0.0`, `3.1.0`).

## Extracting Protocol Schemas

To download Minecraft release servers, run data generators, and extract all OpenRPC schemas:

```bash
./gradlew extractMinecraftManagementSchemas
```

Or using the alias:

```bash
./gradlew extractProtocolSchemas
```

### Plugin Configuration

In `build.gradle.kts`:

```kotlin
plugins {
    id("com.example.msmp.schema-extractor")
}

minecraftManagement {
    onlyReleases.set(true) // default: true
    minMinecraftVersion.set("1.21.9") // default: 1.21.9 (when MSMP was introduced)
    outputDir.set(layout.projectDirectory.dir("protocol-schemas"))
}
```

## Building the Client

Compile and generate typed Java DTOs and APIs from all extracted OpenRPC schemas:

```bash
./gradlew build
```

Generated sources are written to:

```
client/build/generated/sources/msmp/main/java/com/example/msmp/generated/v<version>/
```

## Runtime Configuration

```bash
export MINECRAFT_MANAGEMENT_URL=ws://localhost:25585
export MINECRAFT_MANAGEMENT_SECRET=your-secret
./gradlew :client:run
```

## Generated API Usage

```java
var client = new MinecraftManagementClient(new ObjectMapper(), URI.create(url), secret);
client.connect().join();

var api = new com.example.msmp.generated.v3_1_0.MinecraftManagementApi(client);
```
